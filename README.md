# SunuGuard

**Application Android de sécurité et de vie privée — un produit Cissé Family Company (CFC).**

SunuGuard surveille les connexions réseau de votre téléphone : il montre quels
serveurs et quels traceurs (trackers) chaque application contacte, et vous permet
de les **bloquer**, sans root. Il fonctionne via un VPN local sur l'appareil —
aucune donnée n'est envoyée vers un serveur externe.

## Fonctionnalités de base

- Voir, application par application, quels serveurs et traceurs sont contactés
- Bloquer les traceurs (base de données Exodus intégrée)
- Pare-feu local (autoriser / bloquer l'accès réseau par application)
- 100 % sur l'appareil, sans root

## Angle CFC (en cours de développement)

- Interface en français / wolof
- Détection et alerte des serveurs liés aux arnaques Wave / Orange Money
- Listes de blocage adaptées au contexte sénégalais

## Construire l'APK

Le build se fait automatiquement via GitHub Actions (voir l'onglet **Actions**).
Chaque mise à jour produit un APK `githubDebug` téléchargeable, installable
directement sur un téléphone Android (autoriser « sources inconnues »).

## Licence et attribution

SunuGuard est un logiciel libre sous **GNU GPL v3.0** (voir `LICENSE`).

Il est dérivé de :
- **TrackerControl** — © Konrad Kollnig et contributeurs (GPLv3)
- lui-même dérivé de **NetGuard** — © Marcel Bokhorst / M66B (GPLv3)

Conformément à la GPLv3, le code source de SunuGuard reste ouvert. Les
modifications apportées par CFC sont documentées dans `NOTICE.md`.

---
*Une réalisation Cissé Family Company — +221 77 567 47 25 — cissefamilycompany@gmail.com*
