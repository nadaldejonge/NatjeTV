# Update 1.0.4

Versiecode 10004; artifacts/NatjeTV-1.0.4-debug.apk, daadwerkelijk geïnstalleerd op Xiaomi. AssembleDebug geslaagd, geen emulator gebruikt.

Bevat de rijgebaseerde navigatie en foregroundfocusring van 1.0.3. Instellingen bovenaan via D-pad geopend en scrollen naar Alle apps met witte ring op apparaat vastgelegd. Google Play-logo past binnen de cirkel. Rook is ongewijzigd.

OK wordt nu volledig als één press/hold/release afgehandeld: kort loslaten start één app; 500ms vasthouden opent het contextmenu en markeert de release als verbruikt. Geen extra performClick na een langdruk. Lopende hold wordt bij activity-pauze geannuleerd. De TV-feedback moet de fysieke langdrukflow nog bevestigen; ADB-navigatie-/langdrukreeksen gaven wisselende voorgrondapps, dus daarvan geen geslaagde contextmenucontrole claimen.

Videoland is op verzoek uitgesteld. HOME-pakketblokkering blijft afhankelijk van opnieuw opslaan van wifi door de gebruiker, zoals UPDATE_1.0.2.md beschrijft. Huidige route blijft HOME-hulp; originele launcher niet uitgeschakeld.
