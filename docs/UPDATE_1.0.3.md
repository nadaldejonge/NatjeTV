# Update 1.0.3 — deterministische D-padnavigatie

Versiecode 10003. Geïnstalleerde debug-APK artifacts/NatjeTV-1.0.3-debug.apk, SHA256 085f42034dbd40a856b2f056a4211db79bf80bd5e3778a19f725fd70da0a1870. AssembleDebug geslaagd; rechtstreeks bijgewerkt op de Xiaomi zonder emulator.

De eerdere losse focus-sprongen zijn vervangen door een navigatiemodel gebaseerd op werkelijke RecyclerView-spanrijen. Alleen rijen met focusbare knoppen tellen mee: bovenbalk, favorieten, recent, alle-appsrijen. Omhoog/omlaag gaat uitsluitend naar de vorige/volgende rij; links/rechts blijft binnen die rij. Bij nog niet gebonden tegels wordt naar die adapterpositie gescrold en daarna focus gezet. De bestaande verplaatsmodus behoudt zijn eigen D-padgedrag.

Focusrand staat nu als foreground boven het artwork, met witte rand en lichte elevatieschaduw. Daardoor kan een ondoorzichtig icoon de ring niet meer bedekken. Google Play-artwork is binnen de cirkel ingesprongen zodat de hele driehoek zichtbaar blijft.

De foutieve extra performClick op een niet-afgehandelde OK-key-up is verwijderd. Lang indrukken zet bovendien een vlag die een eventuele late klik/loslaatgebeurtenis onderdrukt. Kort OK blijft native en lang OK opent het contextmenu zonder appstart.

Echte apparaatcontroles: via omhoog/links/rechts/OK SettingsActivity geopend; daarna via omlaag naar Recent en Alle apps, met zichtbare focusring (artifacts/phase2/scroll-1.0.3.png). De eerste langdrukcontroles waren niet eenduidig: een vervolgopname toonde een andere app in plaats van het contextmenu. Daarom is de OK-afhandeling verder vervangen in 1.0.4; geen geslaagde contextmenutest claimen op basis van die opname. Geen reboot-/playbackclaims. Rook onveranderd sinds 1.0.2; Night Mode blijft UIT. Videoland op verzoek uitgesteld.
