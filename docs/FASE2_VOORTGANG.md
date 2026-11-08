# Fase 2 — voortgang 8 november 2026

Gebruiker heeft fase 2 expliciet gestart. Wireless ADB gekoppeld; tijdelijke koppelcode niet opgeslagen in projectbestanden.

- Apparaat: Xiaomi MiTV-AFMU1, Android 14, API 34, product adastra/device twilight.
- Fingerprint: Xiaomi/adastra/twilight:14/UKG3.250107.001/V816.0.25.6.13.1.UZFAATK:user/release-keys.
- Oorspronkelijke HOME: com.google.android.apps.tv.launcherx/.home.HomeActivity. Herstelbaseline vóór installatie opgeslagen in artifacts/phase2-recovery.json. HOME is nog steeds de oorspronkelijke Google TV-launcher; geen stock app uitgeschakeld.
- Natje TV 1.0.0 debug/code10000 met geverifieerde artifacthash geïnstalleerd: adb install -r meldt Success. Activity gestart: Status ok, COLD, TotalTime 2507ms inclusief openen onboarding. Dit is één ADB-startmeting, geen prestatiebenchmark.
- Echte screenshot van onboarding: artifacts/phase2/onboarding-xiaomi.png. App meldt 17 lanceerbare apps. Gebruiker moet onboarding/profielkeuze en eventuele overlaytoestemming nog voltooien.
- NatjeTube 2.0.1/code2460 stond al geïnstalleerd. Geïnstalleerde base-APK is byte-identiek aan geverifieerde nieuwste release: SHA256 0cf1e3a5df75bbe89e9e475089a7300e1c1a4136e54e576264b6fd6bc35a8aa7. Geen herinstallatie nodig; menu-/warmtepatch is aanwezig in dezelfde releasebinary.
- IPTV 0.1.11-recovery/code11 stond al geïnstalleerd. Geïnstalleerde base-APK is byte-identiek aan de geselecteerde lokale versie: SHA256 24a9c2a7906cd43ba86f1828b1305dff4aff478f0bd355ecc3ad09dc5ee06398. Geen herinstallatie of dataverwijdering uitgevoerd.
- Netflix (com.netflix.ninja), Prime Video (com.amazon.amazonvideo.livingroom), NPO Start (nl.uitzendinggemist) en Google Play (com.android.vending) aanwezig. Geen Videoland-pakket gevonden in gerichte naamcontrole; volledige UI-/winkelcontrole volgt.

Nog niet getest: fysieke D-padflows, alle app-launches/playback, gewone/accessibility-overlay, planning/herstart/SDR/HDR/Dolby Vision, HOME-toewijzing en herstel. Geen overlay-/Accessibility-rechten automatisch verleend. Geen reboot uitgevoerd. Fase-1-rapporten en ZIP's blijven historische fase-1-artefacten.
