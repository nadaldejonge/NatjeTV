# Natje TV

Een Android TV-launcher met ronde appknoppen, een geanimeerde rookachtergrond en NatjeNightMode. Versie **1.0.5** · package `nl.natje.tv` · Android 8.0/API 26 of hoger.

## Downloaden en installeren

Download de APK bij [GitHub Releases](https://github.com/nadaldejonge/NatjeTV/releases/latest). Deze eerste publieke uitgave gebruikt de bestaande **debugondertekening**, zodat hij de eerder geïnstalleerde ontwikkelversie kan bijwerken. Dit is een ontwikkeluitgave; er is nog geen aparte productiesleutel. Bewaar je instellingen voordat je een versie met een ander ondertekeningscertificaat installeert.

```powershell
adb install -r NatjeTV-1.0.5-debug.apk
```

Kies Natje TV als startscherm als je apparaat dat ondersteunt. Sommige Xiaomi/Google TV-firmware blijft de eigen launcher openen. De optionele HOME-hulp via toegankelijkheid opent dan Natje TV wanneer het Google TV-startscherm verschijnt. Schakel die service zelf in via Android-instellingen; de app leest geen scherminhoud. Een korte flits van Google TV kan blijven optreden. Het uitschakelen van systeemlaunchers is geen vereiste en is niet universeel veilig.

## Wat zit erin?

- Kleine ronde appknoppen, witte focusgloed en rijen voor favorieten, recent geopend en alle apps.
- Zoeken, instellingen en Night Mode als drie kleine knoppen rechtsboven. D-padbediening en lang OK voor het appcontextmenu.
- Geïnstalleerde TV-apps en sideload-apps, eigen afbeeldingen, volgorde, categorieën en configuratie-import/-export.
- Een lokale WebGL-rookanimatie boven de zwarte achtergrond; er wordt hiervoor niets van internet geladen.
- **Appwinkel** opent een aanwezige winkel, bijvoorbeeld Google Play. Een winkel of streamingapp wordt niet meegebundeld. Ontbrekende streamingapps krijgen een winkelzoekroute.
- **HDMI / PS5** is een vaste keuze met een eigen bronkeuzescherm; zie hieronder voor de benodigde Philips-koppeling.
- **NatjeNightMode** met profielen, schema, vijf seconden overgang, warmte tot 60% en dimmen tot 80%.

NatjeTube is een zelfstandige app: [NatjeTube-releases](https://github.com/nadaldejonge/NatjeTube/releases). Deze repository bevat geen APK's van streamingdiensten, Google Play of andere losse apps.

## Night Mode

Verleen de overlaytoestemming via Android en zet het filter aan in de instellingen. Voor de alternatieve toegankelijkheidsmodus moet ook die service ingeschakeld zijn. De gewone filterservice toont een foregroundmelding met uitknop. De app heeft ook een filter voor het eigen scherm. Android, DRM-video en HDR kunnen de werking buiten het homescreen beperken; universele videofiltering is niet bevestigd.

Night Mode begint bij een nieuwe installatie uit. De instellingen van een bestaande installatie blijven bij een update bewaard. Android Private DNS/AdGuard is een afzonderlijke apparaatinstelling, geen onderdeel van de APK.

## HDMI op Philips Titan OS

De vaste HDMI-tegel is altijd zichtbaar. Werkelijke bediening vereist een compatibele, vooraf gekoppelde Philips Titan OS-tv en een **appprivé** `hdmi_private.json` met `host`, `username`, `password` en `certificateSha256`. Het project bevat geen tv-inloggegevens. Er is in deze versie nog geen ingebouwde koppelwizard.

De bediening gebruikt MQTT 5 over TLS op het lokale netwerk en controleert de SHA256-vingerafdruk van het tv-certificaat. De bron wordt via afstandsbedieningstoetsen gekozen. De indeling moet overeenkomen met de ondersteunde Philips-bronnenlijst. De koppeling accepteerde de verbinding op het gebruikte apparaat; **het daadwerkelijk omschakelen is nog niet fysiek bevestigd**. De melding 'aangevraagd' is geen bevestiging van de geselecteerde ingang. Zonder koppeling blijft de rest van de launcher bruikbaar.

## Zelf bouwen

Vereist: Java 17 of 21, Android SDK platform 36 en build-tools 36.0.0. Gradle 8.14.3 wordt via de wrapper opgehaald. Geen NDK nodig.

```powershell
.\scripts\build.ps1
```

Geef eventueel `-SdkPath C:\pad\naar\Android\Sdk` mee. Dit bouwt een debug-APK en een nog te ondertekenen release-APK in `artifacts/`, met versienamen uit `app/build.gradle.kts`. De gegenereerde debughandtekening is lokaal en kan verschillen van die van de downloadbare APK. Python is niet nodig om te bouwen.

`scripts/verify.ps1` bevat de ontwikkelcontroles. De fase-1-testverslagen in `docs/` zijn historische resultaten voor 1.0.0 en bewijzen niet dat alle wijzigingen in 1.0.5 getest zijn. De huidige APK is gebouwd en op de Xiaomi-stick geïnstalleerd. Verder testen is op verzoek gestopt; navigatie, lang indrukken en HDMI kunnen nog feedback vereisen. Zie [release-opmerkingen](docs/UPDATE_1.0.5.md).

## Broncode, privacy en licenties

Native Kotlin/Android Views met RecyclerView. De internetpermissie dient de lokale Philips-tv-bediening. Geen telemetrie of advertentiediensten; rookassets zijn lokaal. Recent geopende apps worden alleen bijgehouden voor starts vanuit Natje TV. Configuratie-export bevat geen accounts of tv-koppeling.

Eigen code: [MIT](LICENSE). Hergebruik en licenties: [bronnenoverzicht](docs/BRONNEN_EN_LICENTIES.md) en [aanvullende vermeldingen](app/src/main/assets/licenses/THIRD_PARTY_NOTICES.md). Rook: `zlollo/realistic-webgl-smoke`; Philips-protocolreferentie: `bedirhanyildirim/philips-titan-os-55pml9019-remote`.

`artifacts/`, apparaatback-ups, sleutels, lokale SDK-configuratie en onderzoeksdownloads worden niet gepubliceerd. Oude fase-2-installatie- en herstelscripts vereisen een eigen lokale apparaatbaseline; ze zijn geen universele installatiehandleiding.
