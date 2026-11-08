# Acceptatietests — fase 1

8 november 2026. Definitieve lokale ronde: 17 tests geslaagd, lint 0 fouten / 6 waarschuwingen. De drie PNG-schermen zijn werkelijke Android Views in Robolectric, geen ontwerp-mockups en geen emulator-/Xiaomi-bewijs. De PackageManager-inventaris in de tests is bewust gesimuleerd.

| Onderdeel | Status | Omgeving en concrete bewijsbron |
|---|---|---|
| Debug en unsigned release compilatie | **GETEST EN GESLAAGD** | Windows / Gradle; logs/final-verification-build.log |
| APK-versie/package/HOME/Leanback/foregroundtype/geen internetpermissie | **GETEST EN GESLAAGD** | aapt2 op definitieve debug-APK; logs/apk-manifest-tree.log, *-badging.log |
| Debug- en NatjeTube-signatuur + SHA256 | **GETEST EN GESLAAGD** | apksigner en hashlib; logs/*-signature.log; artifacts/APK-MANIFEST.json |
| Lint | **GETEST EN GESLAAGD** | 0 fouten, 6 waarschuwingen; artifacts/reports/lint-results-debug.html |
| Unit-/Android-UI-tests | **GETEST EN GESLAAGD** | 17 tests, 0 failures/errors; artifacts/reports/junit en reports/tests |
| Middernacht-/dagschema alle 1440 minuten, gelijke grenzen, handmatig aan/uit | **GETEST EN GESLAAGD** | CoreTest; JVM |
| Warmte 0.60 i.p.v. 0.06 / dimbereik / daadwerkelijke canvaspixels | **GETEST EN GESLAAGD** | CoreTest + actualFilterDrawHasAmberAndDimming; Robolectric API34 native graphics |
| Persistente instellingen/profielen/volgorde/status na nieuwe ConfigStore | **GETEST EN GESLAAGD** | persistentConfigurationSurvivesStoreRecreation; Robolectric, geen echte reboot |
| Importgrootte/schema/ongeldige JSON/veilig uit blijven | **GETEST EN GESLAAGD** | importIsAtomicAndCannotEnableServices + malformedProfilesAndRangesAreHandled; Robolectric |
| Dynamische TV- en sideload-appdetectie, deduplicatie, eigen app uitsluiten | **GETEST EN GESLAAGD** | installedTvAndSideloadedAppsAreDiscoveredAndDeduplicated; gesimuleerde PackageManager-inventaris |
| Pakket/naam/herkenning, handmatige koppeling, ontbrekende app veilig | **GETEST EN GESLAAGD** | favoriteMatchingUsesInstalledAppsAndManualBinding + missingLaunchIntentDoesNotCrash; Robolectric |
| Appwinkel: uitsluitend geïnstalleerde handler en juiste appnaam in zoekroute | **GETEST EN GESLAAGD** | marketOnlyOpensAnInstalledHandlerAndEncodesAppSearch; Robolectric met gesimuleerde Play-handler |
| Home/settings/night-schermen renderen, focusbaar en langklik | **GETEST EN GESLAAGD** | homeAndSettingsRenderWithoutInstalledApps; artifacts/screenshots/*-robolectric.png |
| Profiel maken via instellingsdialoog | **GETEST EN GESLAAGD** | profileCanBeCreatedFromRemoteAccessibleSettings; Robolectric |
| Noodstop: 3 seconden hold en status persistent uit | **GETEST EN GESLAAGD** | remoteEmergencyHoldWorksWithoutConsumingKeys + emergencyPersistsOffWithoutPermission; Robolectric |
| Geen overlay starten zonder expliciete toestemming | **GETEST EN GESLAAGD** | overlayRequiresExplicitPermission; Robolectric |
| PowerShell-syntax en standaard dry-run zonder SDK/ADB | **GETEST EN GESLAAGD** | logs/script-verification.log; werkelijke lokale uitvoering uitsluitend dry-run |
| NatjeTube nieuwste menu-/warmtepatch en releasebestand | **GETEST EN GESLAAGD** | GitHub HEAD/tag/release/diff; research/nadaldejonge__NatjeTube/ + APK-hash/signatuur |
| Lokale IPTV-kandidaten, versiecode, broncode, signatuur en logo | **GETEST EN GESLAAGD** | research/iptv_inventory.json; docs/NATJETUBE_EN_IPTV_VERSIES.md; logs/iptv-apksigner.log |
| Fysieke D-pad-navigatie, verplaatsen, favorieten, zoeken, echte apps starten | **NOG NIET GETEST** | NOG NIET OP ECHTE STICK GETEST; lokale focus-/inventaristests bewijzen dit niet volledig |
| Alle zes apps geïnstalleerd + echte Google Play Store open/installatie | **NOG NIET GETEST** | Twee Natje-APK's volledig meegeleverd; overige vier streamingapps via winkelroute, geen binaries. Niets geïnstalleerd op Xiaomi |
| Profielen hernoemen/wissen en documentpicker-import/export/artwork op firmware | **NOG NIET GETEST** | Geïmplementeerd; geen volledige geautomatiseerde end-to-endtest |
| Echte overlayvensters/foregroundservice boven andere processen | **NOG NIET GETEST** | Geïmplementeerd; unitcanvas/toestemmingstests zijn geen WindowManager/DRM-hardwarebewijs |
| 5 seconden echte compositorovergang en rendement/batterij | **NOG NIET GETEST** | ValueAnimator geïmplementeerd; meten in fase 2 |
| Night Mode na echte boot/procesdood/standby met Xiaomi-beleid | **NOG NIET GETEST** | Bootreceiver + START_STICKY + opgeslagen toestand voorbereid; echte firmware niet benaderd |
| Toegankelijkheidsbackend en echte firmware-combinaties | **NOG NIET GETEST** | Service gedeclareerd, geen toestemming geactiveerd in fase 1 |
| Homescreen + NatjeTube + IPTV filter tijdens daadwerkelijke playback | **NOG NIET GETEST** | NOG NIET OP ECHTE STICK GETEST |
| Netflix-menu en daadwerkelijke DRM-playback | **NOG NIET GETEST** | NOG NIET OP ECHTE STICK GETEST |
| Videoland, Prime Video, NPO Start menu/playback | **NOG NIET GETEST** | NOG NIET OP ECHTE STICK GETEST |
| SDR, HDR10, Dolby Vision en dubbele filtering | **NOG NIET GETEST** | NOG NIET OP ECHTE STICK GETEST |
| HOME standaardrol, Home-knop, reboot/wake en herstel | **NOG NIET GETEST** | Manifest en veilige scripts aanwezig; geen ADB/device-mutatie uitgevoerd |
| Install-/update-/herstelscript op Android-firmware | **NOG NIET GETEST** | Alleen syntax/dry-run getest. Uitvoering uitsluitend na fase-2-startsein |
| Screensaver, rusttimer en boot-app op werkelijke firmware | **NOG NIET GETEST** | Geïmplementeerd met beperkingen uit README; fysieke tv-uitstand niet aangeboden |
| RAM/PSS, cold/warm starttijd en framejank op 2GB-stick | **NOG NIET GETEST** | Geen cijfers verzonnen; meetprotocol in INSTALLATIE_FASE_2.md |
| Philips Titan OS / PS5 HDMI | **NOG NIET GETEST** | Uitsluitend niet-beschikbare provider/stub. Geen tv benaderd. Ambilight buiten scope |
| Emulator/instrumentatie op echt Android-systeem | **NOG NIET GETEST** | Niet gestart; uitsluitend JVM/Robolectric en native graphics. Geen ADB in fase 1 |

## Eerdere iteraties

Eerdere logs bevatten herstelde fouten: onjuiste volgorde van Robolectric-qualifiers, focuscontrole in touchmode, een nog niet afgehandeld OnShow-event in de profieltest, een API31-splashattribuut in de API26-basisstijl en een onjuist ge-escapet Windows-SDK-pad. De definitieve ronde hierboven is groen. Er is geen mislukte of ontbrekende hardwaretest als geslaagd aangemerkt.

## Bekende grenzen

Zes niet-blokkerende lintwaarschuwingen in deze toolchain (zie volledig rapport): gepinde oudere target/tool/testversies, synchroon opgeslagen bootmarker, vaste TV-landscape en toekomstige backupregels. Het doel is lokale sideload op API34; geen Play-publicatie. Android/Xiaomi kan overlays/achtergrondstart en inputrechten beperken. Het actieve filtervenster kan niet betrouwbaar vaststellen dat een andere app het boven secure playback verbergt. Touchscreenpassthrough kan door Android worden beperkt bij hoge gewone overlay-opacity; de TV-bediening is D-pad en vereist nog echte verificatie.
