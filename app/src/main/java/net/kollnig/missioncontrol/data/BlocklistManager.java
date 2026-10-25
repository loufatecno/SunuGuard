package net.kollnig.missioncontrol.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BlocklistManager {
    private static final String TAG = "TrackerControl.Blocklist";
    private static final String PREF_BLOCKLISTS = "blocklists";
    public static final String DEFAULT_HOSTS_URL = "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts";
    // SunuGuard (CFC) : listes anti-phishing/arnaque ajoutees par defaut (format hosts).
    public static final String CFC_THREATS_URL = "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/hosts/tif.txt";
    public static final String CFC_SENEGAL_SCAM_URL = "https://raw.githubusercontent.com/loufatecno/SunuGuard/main/blocklists/senegal-scams.txt";

    /**
     * SunuGuard : seme les listes de blocage par defaut (pubs + malware + phishing
     * + arnaques Senegal) si elles ne sont pas deja presentes. Toutes activees.
     */
    public void seedDefaults() {
        List<Blocklist> list = getBlocklists();
        java.util.Set<String> urls = new java.util.HashSet<>();
        for (Blocklist b : list)
            urls.add(b.url);
        String[] defaults = {DEFAULT_HOSTS_URL, CFC_THREATS_URL, CFC_SENEGAL_SCAM_URL};
        boolean changed = false;
        for (String u : defaults) {
            if (!TextUtils.isEmpty(u) && !urls.contains(u)) {
                list.add(new Blocklist(u, true));
                changed = true;
            }
        }
        if (changed)
            saveBlocklists(list);
    }

    private static BlocklistManager instance;
    private final Context context;

    private BlocklistManager(Context context) {
        this.context = context.getApplicationContext();
        migrateIfNeeded();
        cleanup();
    }

    private void cleanup() {
        File dir = context.getFilesDir();
        File[] files = dir.listFiles(
                (dir1, name) -> name.startsWith("blocklist_") && (name.endsWith(".txt")));
        if (files == null)
            return;

        List<Blocklist> list = getBlocklists();
        for (File file : files) {
            boolean found = false;
            for (Blocklist item : list) {
                if (file.getName().equals("blocklist_" + item.uuid + ".txt")) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                Log.i(TAG, "Deleting orphaned file " + file.getName());
                file.delete();
            }
        }
    }

    public static synchronized BlocklistManager getInstance(Context context) {
        if (instance == null) {
            instance = new BlocklistManager(context);
        }
        return instance;
    }

    public List<Blocklist> getBlocklists() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String jsonString = prefs.getString(PREF_BLOCKLISTS, "[]");
        List<Blocklist> list = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                Blocklist item = new Blocklist();
                item.uuid = obj.getString("uuid");
                item.url = obj.getString("url");
                item.enabled = obj.optBoolean("enabled", true);
                item.lastModified = obj.optLong("lastModified", 0);
                item.lastDownloadSuccess = obj.optBoolean("lastDownloadSuccess", true);
                item.lastErrorMessage = obj.optString("lastErrorMessage", null);
                item.etag = obj.optString("etag", null);
                item.lastModifiedHeader = obj.optString("lastModifiedHeader", null);
                list.add(item);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing blocklists", e);
        }
        return list;
    }

    public void saveBlocklists(List<Blocklist> list) {
        JSONArray jsonArray = new JSONArray();
        for (Blocklist item : list) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("uuid", item.uuid);
                obj.put("url", item.url);

                obj.put("enabled", item.enabled);
                obj.put("lastModified", item.lastModified);
                obj.put("lastDownloadSuccess", item.lastDownloadSuccess);
                obj.put("lastErrorMessage", item.lastErrorMessage);
                obj.put("etag", item.etag);
                obj.put("lastModifiedHeader", item.lastModifiedHeader);
                jsonArray.put(obj);
            } catch (JSONException e) {
                Log.e(TAG, "Error saving blocklist item", e);
            }
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(PREF_BLOCKLISTS, jsonArray.toString()).apply();
    }

    public void addBlocklist(Blocklist blocklist) {
        List<Blocklist> list = getBlocklists();
        list.add(blocklist);
        saveBlocklists(list);
    }

    public void updateBlocklist(Blocklist blocklist) {
        List<Blocklist> list = getBlocklists();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).uuid.equals(blocklist.uuid)) {
                list.set(i, blocklist);
                break;
            }
        }
        saveBlocklists(list);
    }

    public void removeBlocklist(String uuid) {
        List<Blocklist> list = getBlocklists();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).uuid.equals(uuid)) {
                list.remove(i);
                break;
            }
        }
        saveBlocklists(list);

        // Also remove the cached file
        File file = new File(context.getFilesDir(), "blocklist_" + uuid + ".txt");
        if (file.exists()) {
            file.delete();
        }
    }

    public void migrateIfNeeded() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.contains("hosts_url_new")) {
            String oldUrl = prefs.getString("hosts_url_new", DEFAULT_HOSTS_URL);

            // Only migrate if we don't have existing blocklists to avoid
            // duplicates/overwrite
            // (Though user said "if existing host list exists, just delete and migrate",
            // the implementation plan says migrate if pref exists.
            // Let's check provided list. If empty, definitely migrate.
            List<Blocklist> currentList = getBlocklists();
            if (currentList.isEmpty()) {
                if (!TextUtils.isEmpty(oldUrl)) {
                    Blocklist item = new Blocklist(oldUrl, true);
                    item.lastModified = prefs.getLong("hosts_last_download_time", 0); // Try to preserve if possible,
                                                                                      // otherwise 0
                    addBlocklist(item);
                } else {
                    seedDefaults();
                }
            }

            // Remove old preference as requested
            prefs.edit().remove("hosts_url_new").apply();
        } else {
            // If completely fresh install or no pref, ensure we have at least the default
            // list if list is empty
            List<Blocklist> currentList = getBlocklists();
            if (currentList.isEmpty()) {
                seedDefaults();
            }
        }
    }

    public File getBlocklistFile(String uuid) {
        return new File(context.getFilesDir(), "blocklist_" + uuid + ".txt");
    }

    public boolean mergeBlocklists() {
        File hostsFile = new File(context.getFilesDir(), "hosts.txt");
        File hostsTmp = new File(context.getFilesDir(), "hosts.tmp");

        List<Blocklist> list = getBlocklists();
        int count = 0;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(hostsTmp))) {
            writer.write("# Merged Blocklists by TrackerControl\n");

            for (Blocklist item : list) {
                if (!item.enabled)
                    continue;

                File itemFile = getBlocklistFile(item.uuid);
                if (itemFile.exists() && itemFile.canRead()) {
                    writer.write("# Blocklist: " + item.url + "\n");
                    try (BufferedReader reader = new BufferedReader(new FileReader(itemFile))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            writer.write(line);
                            writer.newLine();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading blocklist " + item.url, e);
                    }
                    writer.write("\n");
                    count++;
                }
            }

        } catch (IOException e) {
            Log.e(TAG, "Error writing merged hosts file", e);
            return false;
        }

        return hostsTmp.renameTo(hostsFile);
    }
}
