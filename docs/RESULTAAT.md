# Resultaat fase 1

Bronproject: `C:\Users\nadal\Downloads\NatjeTV_1.0_Codex_Startpakket\NatjeTV`. Android TV-launcher Natje TV 1.0.0, package `nl.natje.tv`, code 10000. 17 lokale tests geslaagd, lint 0 fouten / 6 waarschuwingen. Geen ADB gestart, geen Xiaomi-/tv-toegang en geen HOME-/stock-launcherwijzigingen.

| APK | Package / versiecode | Bytes | SHA256 |
|---|---|---|---|
| `artifacts/NatjeTV-1.0.0-debug.apk` | nl.natje.tv / 1.0.0 / 10000 | 3129813 | `31536e062e618c41e8a924f7349caf2f19a033d4fadbfba900448597e92c0782` |
| `artifacts/NatjeTV-1.0.0-release-unsigned.apk` | nl.natje.tv / 1.0.0 / 10000 | 2355368 | `32b87785ce70cf9bc344ea7b9043f0b5bb6cb73a7874ada099fca1d5fb5ef0ba` |
| `artifacts/companions/Natje-IPTV-AndroidTV-0.1.11-recovery-debug.apk` | nl.natje.iptvplayer / 0.1.11-recovery / 11 | 13378183 | `24a9c2a7906cd43ba86f1828b1305dff4aff478f0bd355ecc3ad09dc5ee06398` |
| `artifacts/companions/NatjeTube_2.0.1_universal.apk` | nl.natje.natjetube / 2.0.1 / 2460 | 34964348 | `0cf1e3a5df75bbe89e9e475089a7300e1c1a4136e54e576264b6fd6bc35a8aa7` |

Debug is lokaal ondertekend en klaar voor latere testinstallatie. Release is unsigned en moet vóór installatie worden ondertekend. Er zitten geen private sleutels in project of broncode-ZIP. De NatjeTube-companion is byte-geverifieerd tegen GitHub en bevat menucrashfix plus 60%-warmtepatch; hij is niet in de launcher ingebouwd.

Lokale IPTV: `C:/Users/nadal/Downloads/Player/artifacts/android-tv/Natje-IPTV-AndroidTV-0.1.11-recovery-debug.apk`, `nl.natje.iptvplayer`, code 11, SHA256 `24a9c2a7906cd43ba86f1828b1305dff4aff478f0bd355ecc3ad09dc5ee06398`. Volledige APK ook gekopieerd naar artifacts/companions. Geen IPTV-gegevens of playlists gelezen. Zie de volledige inhoudsvergelijking in de inventaris.

Alle zes apptegels plus Appwinkel zijn geïntegreerd. Twee Natje-apps volledig als losse APK meegeleverd; Netflix/Videoland/NPO Start/Prime Video krijgen een officiële winkelzoekroute en zijn niet als offline binaries gebundeld. Appwinkel opent de aanwezige Google Play-/market-handler. Zie docs/APPPAKKET.md.

Scripts: `scripts/build.ps1`, `verify.ps1`, `preflight.ps1`, `install-phase2.ps1`, `restore-original-launcher.ps1`, `verify-scripts.ps1`. Installatie/herstel zijn standaard dry-run en zijn niet tegen een apparaat uitgevoerd. `-IncludeNatjeApps` bereidt installatie van beide losse Natje-apps voor. Broncodebundel: `artifacts/NatjeTV-1.0.0-source.zip`. Compleet apppakket inclusief broncode-ZIP, beide Natje-apps en launcher: `artifacts/NatjeTV-1.0.0-app-pakket.zip`.

Screenshots: `artifacts/screenshots/home-robolectric.png`, `settings-robolectric.png`, `nightmode-robolectric.png`. Echte Android-rendering onder Robolectric; geen claims over decoder, DRM, HOME of emulator. Alle details en bewijs: `docs/ACCEPTATIETESTS.md` en `artifacts/reports/`.

Geen lokale bouwblocker. Openstaande bewijzen: fysieke remote en volledige appflows, werkelijke overlay/DRM/HDR/Dolby Vision, firmwarepersistentie, HOME/recovery en 2GB-performance. HDMI is een stub. Fase 2 begint pas met jouw expliciete startsein; dan model/API/fingerprint/serial vaststellen, recoverybaseline vastleggen, eerst alleen APK testen en daarna eventueel HOME toewijzen. Stock launcher blijft intact.
