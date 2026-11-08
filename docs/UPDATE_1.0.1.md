# Update 1.0.1 — op Xiaomi geïnstalleerd

Versiecode 10001. Debug-APK: artifacts/NatjeTV-1.0.1-debug.apk. SHA256 eae54f7a689af860a71acfd4f48c3ef46e2ff4c887463fdcfe28ae1dde11efba.

Kleinere ronde appknoppen, full-bleed vierkante appiconen gecropt binnen de cirkel in plaats van losse banners met padding. Zwarte standaardachtergrond met native geanimeerde rook; rendering stopt buiten HOME. Directe focus bij terugkeer, behoud van bestaande appadapter bij ongewijzigde inventaris, laadindicator bij echt wachten. Configuratie wordt in geheugen gecachet; toetscodes worden alleen bij eerste waarneming opgeslagen en schrijftaken blokkeren de UI niet langer.

Night Mode-probleem vastgesteld: accessibilitybackend gekozen maar niet verbonden, gewone overlaytoestemming ontbrak. Gewone overlay toegestaan, foregroundservice en TYPE_APPLICATION_OVERLAY-window op echte stick gecontroleerd; amber/dim zichtbaar in screenshot home-1.0.1-focused.png. Daarnaast lokale activityfilter bij ontbreken van globale toestemming en fallback op gewone backend bij niet-beschikbare accessibilitybackend. DRM/HDR-playback niet bewezen. Op laatste verzoek van gebruiker is Night Mode persistent UIT gezet voor feedbacktests, profielwaarden behouden.

HOME: rol/toewijzing alleen bleek niet genoeg; Google TV bleef geselecteerd. Tijdelijke disable-user van com.google.android.apps.tv.launcherx is op gebruikersverzoek geprobeerd. Firmware koos daarop com.google.android.tungsten.setupwraith/.RecoveryActivity. Stock launcher vervolgens weer ingeschakeld; geen systeemapp verwijderd of recoverycomponent uitgeschakeld. Herstelbaseline disabledByNatjeTv is weer leeg.

Gebruiker gaf expliciet toestemming om toegankelijkheid via ADB te activeren voor HOME-hulp. Andere toegankelijkheidsservices behouden. HOME-hulp opent Natje TV wanneer Google TV verschijnt, zonder scherminhoud te lezen. Op echte stick twee controles na HOME: topResumedActivity is nl.natje.tv/.launcher.MainActivity. Een kort Google TV-overgangsbeeld kan zichtbaar zijn; dit is HOME-hulp, geen bewezen native firmwarevervanging. Night Mode blijft uit terwijl de service HOME verzorgt.

Werkelijke cold start vóór HOME-hulp: 1361ms, hot resume 107ms (losse ADB-metingen, geen benchmark). D-padfocus op stick gecontroleerd. Nieuwste screenshot: artifacts/phase2/home-1.0.1-night-off.png. Geen emulator gebruikt voor deze update; assembleDebug geslaagd. Verdere fysieke-afstandsbediening-, reboot/wake- en playbackfeedback volgt van gebruiker.

Oorspronkelijke launcher/herstelgegevens staan in artifacts/phase2-recovery.json. restore-original-launcher.ps1 kan HOME, overlay en onze nieuw toegevoegde accessibility-entry herstellen. Historische 1.0.0-ZIP's bevatten de vorige versie, gebruik voor deze update de expliciete 1.0.1-APK.
