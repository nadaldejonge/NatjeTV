# Natje TV 1.0.5

Eerste publieke GitHub-uitgave, versiecode 10005.

- Vaste HDMI / PS5-tegel en eigen bronkeuzescherm met HDMI 1 t/m 4. De oude optionele placeholder is vervangen.
- Philips Titan OS-bediening via lokaal MQTT/TLS met certificaatcontrole. Privékoppeling blijft buiten broncode, APK en configuratie-export. Er is nog geen ingebouwde koppelwizard.
- Ronde appknoppen, focusgloed, volledige Google Play-afbeelding en compacte zoek-/instellingen-/Night Mode-knoppen bovenaan.
- Aangepaste D-padnavigatie tussen kop, favorieten, recente apps en alle apps; afhandeling van lang OK moet voorkomen dat ook de app start.
- Lokale geanimeerde WebGL-rookachtergrond en uitgebreid Night Mode-filter met optionele HOME-hulp via toegankelijkheid.

## Status

De meegeleverde debugondertekende APK is gebouwd en op de Xiaomi MiTV-AFMU1 met Android 14 geïnstalleerd. Het HDMI-keuzescherm is geopend; de gekoppelde Philips accepteerde de MQTT-verbinding. Het daadwerkelijk wisselen van ingang is niet fysiek bevestigd. Verdere tests zijn op verzoek gestopt. Dit is geen volledig geteste productie-uitgave.

HOME-gedrag is firmwareafhankelijk. Op het gebruikte apparaat is HOME-hulp nodig; de oorspronkelijke Google-launcher is beschikbaar gebleven. DRM/HDR-filtering is niet algemeen bevestigd. De historische fase-1-tests in andere documenten gelden voor 1.0.0.

De release bevat alleen Natje TV en de bijbehorende broncode/licenties. Andere apps en appwinkels worden zelfstandig geïnstalleerd. De ingestelde Private DNS op het ontwikkelapparaat zit niet in deze APK.
