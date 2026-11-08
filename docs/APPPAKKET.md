# De zes apps en Appwinkel

De launcher integreert alle zes vaste apptegels én een Appwinkel-tegel. Apps blijven echte zelfstandige apps met hun eigen updates/accounts. Er is in fase 1 niets op de Xiaomi geïnstalleerd. Alleen apps die PackageManager daadwerkelijk vindt, worden als geïnstalleerd behandeld.

| App | Wat zit in het pakket? | Hoe wordt hij later beschikbaar? |
|---|---|---|
| NatjeTube | Volledige GitHub-release `NatjeTube_2.0.1_universal.apk`, code 2460; menu-bugfix en 60% amberbereik geverifieerd | Losse APK via expliciete fase-2-installatie |
| Natje IPTV | Volledige lokaal geverifieerde `Natje-IPTV-AndroidTV-0.1.11-recovery-debug.apk`, code 11 | Losse APK via expliciete fase-2-installatie |
| Netflix | Vaste tegel, detectie en zoekroute naar aanwezige officiële winkel | Bestaande app starten of op apparaat via Google Play installeren |
| Videoland | Vaste tegel, detectie en zoekroute naar aanwezige officiële winkel | Bestaande app starten of op apparaat via Google Play installeren |
| NPO Start | Vaste tegel, detectie en zoekroute naar aanwezige officiële winkel | Bestaande app starten of op apparaat via Google Play installeren |
| Prime Video | Vaste tegel, detectie en zoekroute naar aanwezige officiële winkel | Bestaande app starten of op apparaat via Google Play installeren |
| Appwinkel | Google Play-/market-handlerdetectie, launchactie en eventuele normale store-appfallback | De op de stick aanwezige winkel openen; geen Google Play-systeem-APK gebundeld |

De losse APK's staan in `artifacts/companions/`. Hun versie, package, echte bytes/SHA256 en signatuurcontrole staan in `artifacts/APK-MANIFEST.json` en de inventaris. Het volledige apppakket `artifacts/NatjeTV-1.0.0-app-pakket.zip` bevat launcher, beide Natje-apps, hashes, scripts en installatie-instructies. De broncode staat daarnaast volledig in `NatjeTV-1.0.0-source.zip`.

Het fase-2-script ondersteunt expliciet `-IncludeNatjeApps`. In combinatie met `-Execute -Serial '<werkelijke serial>'` en de getypte installatiebevestiging worden de twee eigen apps afzonderlijk geïnstalleerd/geüpdatet vóór een eventuele HOME-toewijzing. Android controleert de signingidentiteit. Bij conflict stopt het script: geen automatische uninstall of datawissen. NatjeTube 2.0.1 gebruikt upstream een nieuwe signingkey; bespreek een eventuele herinstallatie eerst als een oudere anders ondertekende versie aanwezig blijkt.

Commerciële streamingapp-APK's zijn niet in de lokale toegestane Downloads-projectmappen gevonden en er is geen Play Store-downloadsessie gekoppeld. Daarom zijn ze niet als offline binaries meegeleverd of al geïnstalleerd gerapporteerd. De winkelactie gebruikt `market://search` met de appnaam en een werkelijk geïnstalleerde handler; de winkel bepaalt variant, regio en apparaatbeschikbaarheid. Accounts en eventuele abonnementen stel je rechtstreeks in de betreffende apps in.

Google Play krijgt voorrang. Een al geïnstalleerde andere winkel met market-handler kan ook worden geopend; normale lanceerbare Play Store/Aptoide TV/Amazon Appstore-entries worden bovendien dynamisch herkend. Geen alternatieve store gedownload of ingesteld, geen onbekende APK-mirror gebruikt.

Primaire installatiebronnen gecontroleerd op 8 november 2026: [Google Android TV — Play Store openen](https://support.google.com/androidtv/answer/9461288?hl=en), [Netflix op Google TV](https://help.netflix.com/en/node/40719), [NPO Start en Android TV](https://npo.nl/service/npo-start), [Prime Video op apparaten installeren](https://www.primevideo.com/help?nodeId=GQDJBTL3CFLLKPBV). Voor Videoland gebruikt de app een winkelzoekopdracht op merknaam; er wordt geen onbevestigd pakket-ID als gegarandeerde TV-installatie gebruikt.

Fase-2-acceptatie: controleer alle zes apps daadwerkelijk geïnstalleerd, iedere tegel opent de juiste TV-app, de Appwinkel opent, een ontbrekende streamingapp komt via de winkel terug op home, en menunavigatie plus echte playback werken. Die controles zijn **NOG NIET OP ECHTE STICK GETEST**.
