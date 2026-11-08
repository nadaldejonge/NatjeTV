# Update 1.0.2 — bediening, bovenbalk en GPU-rook

Versiecode 10002, artifacts/NatjeTV-1.0.2-debug.apk. De nieuwste hash is rechtstreeks te berekenen met Get-FileHash; oudere 1.0.0-ZIP's blijven historische builds.

Zoeken, instellingen en Night Mode staan als drie kleine ronde native vectoriconen rechtsboven naast de klok. De grote actierij onder de favorieten is verwijderd. Expliciete D-padverbindingen tussen favorieten en bovenbalk voorkomen dat RecyclerView de focusovergang opvangt. Focus op een scrollcontainer telt niet meer als bruikbare knopfocus. Herstel wordt ook na windowovergangen uitgevoerd. OK behoudt native langdrukbediening en heeft een fallback als een key-up niet wordt afgehandeld.

De oude cirkelrook is vervangen door Realistic Smoke Wisps (MIT, Lorenzo Fattò Offidani), met Ashima Arts-noisecredits en oorspronkelijke licenties in app/src/main/assets/licenses. Werkelijke bron: https://github.com/zlollo/realistic-webgl-smoke . Het gedownloade bestand is lokaal gebundeld; alleen ES-module-exportstatements zijn verwijderd voor laden als lokaal script. GPU-simulatie via een niet-focusbare WebView, alle netwerkrequests geblokkeerd, 192×96 particles, render-pixelratio 0.75, maximaal 24 fps. Pauze buiten home. Screenshot van werkelijke stick: artifacts/phase2/home-1.0.2-xiaomi.png. Koude WebView-initialisatie gaf eenmalig overgeslagen frames in logcat; warme terugkeer hergebruikt de bestaande view. Geen emulator gebruikt.

Night Mode blijft op gebruikersverzoek UIT. Gewone overlaytoestemming en de eerder expliciet toegestane HOME-hulp blijven beschikbaar.

## HOME-onderzoek en concrete blokkade

Alleen com.google.android.tungsten.setupwraith/.RecoveryActivity uitschakelen is geprobeerd op het echte apparaat. Android weigert dit met SecurityException: Shell cannot change component state. Er is hierdoor geen component uitgeschakeld.

De primaire onderhoudersbron tools/disable.sh van Gathin23/cleanyourtv beschrijft dat uitschakelen van het hele setupwraith-pakket het opgeslagen wifi-netwerk kan verwijderen als dat pakket eigenaar is. Alleen creator-identiteiten zijn uit dumpsys wifi gehaald, geen SSID/wachtwoorden opgeslagen: de gevonden eigenaar op deze Xiaomi is daadwerkelijk com.google.android.tungsten.setupwraith. Daarom is het hele pakket niet uitgeschakeld. Ook launcherx blijft ingeschakeld zolang de herstelactiviteit HOME zou kunnen overnemen.

Bronnen: https://github.com/Gathin23/cleanyourtv/blob/main/tools/disable.sh (Wi-Fi ownership guard), https://github.com/Gathin23/cleanyourtv/issues/1106 (andere firmware kan setupwraith bij reboot herstellen), https://github.com/Gathin23/cleanyourtv/issues/1573 (HOME-prioriteit). Externe firmwarevoorbeelden zijn geen Xiaomi-bewijs. Geen uninstall, root of gewijzigde wifi-credentials toegepast.

Voor verdere pakketblokkering moet de gebruiker eerst op de tv het wifi-netwerk vergeten en opnieuw via Instellingen verbinden, waarna de eigenaar opnieuw gecontroleerd wordt. Dan kan een tijdelijke blokkering van beide pakketten met herstelbaseline worden geprobeerd. Die stap is nog niet uitgevoerd. Tot dan is HOME-hulp de actieve route. Rebootbestendigheid blijft ongetest.
