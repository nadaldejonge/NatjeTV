# NatjeTube en lokale IPTV-versies

Gecontroleerd op 8 november 2026. Geen apps geïnstalleerd of bijgewerkt op een apparaat.

## NatjeTube

- HEAD, enige gerapporteerde branch `main` en tag `v2.0.1`: `607a15a4256a7a0a911fc230195199e53d13ff22` (6 november 2026).
- Menucrash + warmtepatch: `a061876e2c8167a4873422754ab448a2a1fa65ea`; daarop volgen twee renderer-testcommits en documentatie. Geen nieuwere niet-gereleasete patch in de onderzochte hoofdbranch gevonden.
- Release gepubliceerd 6 november 2026 23:50:18 UTC. APK `NatjeTube_2.0.1_universal.apk`, 34.964.348 bytes, package `nl.natje.natjetube`, versionCode 2460.
- Zelf gedownload en SHA256 vergeleken met GitHub-digest: `0cf1e3a5df75bbe89e9e475089a7300e1c1a4136e54e576264b6fd6bc35a8aa7`. Bestand: `artifacts/companions/NatjeTube_2.0.1_universal.apk`.
- `apksigner verify` geslaagd. Certificaat-SHA256: `f590ea01b366e4c2a4560514bba579bcc4cb1efd69077e43f663bbc9ba41834d`. Upstream meldt een nieuwe thuisbuild-key t.o.v. 2.0: niet automatisch verwijderen/updaten; eerst in fase 2 geïnstalleerd certificaat en gewenste databehoud bespreken.
- Renderbron: `common/.../misc/NatjeNightOverlayView.java`: maximaal 0.60 amber (`255,171,80`), dim 0..80%, warmte 0..100%. Presets uit settingsactivity: Normaal 25/15, Avond 45/30, Extra Warm 75/40, Film 15/45. SharedPreferences JSON, eigen profielen, overgang standaard 5 sec. NatjeTube playeroverlay is activitygebonden; daarom geen claim dat die eigen code vanzelf systeemwijd werkt.
- Natje TV neemt geen streamingmodules, accounts, player of APK in de launcher op. NatjeTube blijft zelfstandig.

## Lokale Natje IPTV Player

Beperkte zoekscope: Downloads/Player plus de drie Player-backupmappen, alleen artifact-/Android-debug-outputmappen. Geen playlists, accountbestanden, tokens of overige persoonlijke documenten gelezen. Het record met alle paden/SHA256 staat in `research/iptv_inventory.json`. Meerdere oude versies gevonden; de generieke debugnaam is versie 0.1.9 en is dus niet gekozen.

| Bestand | Versie / code | Bytes | SHA256 |
|---|---|---|---|
| `C:\Users\nadal\Downloads\Player\artifacts\android-tv\Natje-IPTV-AndroidTV-0.1.11-recovery-debug.apk` | 0.1.11-recovery / 11 | 13378183 | `24a9c2a7906cd43ba86f1828b1305dff4aff478f0bd355ecc3ad09dc5ee06398` |
| `C:\Users\nadal\Downloads\Player\android\app\build\outputs\apk\debug\app-debug.apk` | 0.1.11-recovery / 11 | 13378183 | `24a9c2a7906cd43ba86f1828b1305dff4aff478f0bd355ecc3ad09dc5ee06398` |
| `C:\Users\nadal\Downloads\Player\artifacts\android-tv\Natje-IPTV-AndroidTV-0.1.10-tv-debug.apk` | 0.1.10-tv / 10 | 13084831 | `93681db62fa35508acb4a241cb88ba7b08e756aca958effbe418c06fd2e39765` |
| `C:\Users\nadal\Downloads\Player\artifacts\android-tv\Natje-IPTV-AndroidTV-0.1.9-awake-debug.apk` | 0.1.9-awake / 9 | 9577532 | `43e54623fb824804c8ab0971a73fa9d6c90bef0f5b2f241d1fdad3b311faebcd` |
| `C:\Users\nadal\Downloads\Player\artifacts\android-tv\Natje-IPTV-AndroidTV-debug.apk` | 0.1.9-awake / 9 | 9577532 | `43e54623fb824804c8ab0971a73fa9d6c90bef0f5b2f241d1fdad3b311faebcd` |
| `C:\Users\nadal\Downloads\Player\artifacts\android-tv\Natje-IPTV-AndroidTV-0.1.8-focus-debug.apk` | 0.1.8-focus / 8 | 9577004 | `d9f628340a5b3547d5ffab5563e8c65ca6ce098e9c6639a4cbe73cb11eb00e1e` |
| `C:\Users\nadal\Downloads\Player\artifacts\android-tv\Natje-IPTV-AndroidTV-0.1.7-speed-debug.apk` | 0.1.7-speed / 7 | 9576592 | `7d033b9964b7d235b5f9677ef95a8c912841c3752fdc3ad7762d84741aaed3e5` |
| `C:\Users\nadal\Downloads\Player\artifacts\android-tv\Natje-IPTV-AndroidTV-0.1.6-tvfix-debug.apk` | 0.1.6-tvfix / 6 | 9575820 | `8abcb62ce440d0f8364725c3cc163ee26bf8e54f6f6a03de00ff6ab6540c0811` |
| `C:\Users\nadal\Downloads\Player\artifacts\android-tv\Natje-IPTV-AndroidTV-0.1.5-tvfix-debug.apk` | 0.1.5-tvfix / 5 | 9575368 | `204b7fc481d50cfeb12385a7bf85c1cea5c52707687406047d752163309c4fb8` |

Nieuwste inhoudelijk bevestigde kandidaat: `C:/Users/nadal/Downloads/Player/artifacts/android-tv/Natje-IPTV-AndroidTV-0.1.11-recovery-debug.apk`, code 11, versie `0.1.11-recovery`, laatst gewijzigd 3 november 2026 21:52 lokale tijd. De build-outputkopie heeft dezelfde SHA256. `Player/android/app/build.gradle` bevestigt namespace/applicationId `nl.natje.iptvplayer`, versie/code 11; `aapt dump badging` bevestigt dit in de APK. De launcherlabel is “Natje's IPTV-player”; de automatische naamdetectie ondersteunt ook die spelling. Versie 0.1.10-tv heeft code 10 en andere inhoud; oude 0.1.9-/lagere builds worden niet als nieuwste behandeld.

Signatuur gecontroleerd met apksigner; volledig resultaat in `logs/iptv-apksigner.log`. App hoeft voor bouwen niet te worden geïnstalleerd. Het echte IPTV-logo is ongewijzigd gekopieerd uit `Player/src/renderer/public/branding/natje-2026/natje-iptv-logo-400w.png`. Na installatie worden echte iconen/banners via PackageManager geladen; de meegeleverde logofallback koppelt geen fictieve app.
