# Lokale ontwikkelomgeving

Gecontroleerd op 8 november 2026, Windows, gebruikersmap `C:/Users/nadal`. Bestaande tools hergebruikt; geen nieuwe SDK/NDK/emulator geïnstalleerd. Geen ADB-proces of verbinding gestart voor de tests.

- Java: Eclipse Adoptium `jdk-21.0.11.10-hotspot` in Program Files.
- SDK: `C:/Users/nadal/AppData/Local/Android/Sdk`, platform 36 en build-tools 36.0.0 gebruikt. Ook 36.1/37-previewplatforms en NDK aanwezig; niet nodig voor deze app.
- Emulatorprogramma en bestaande Google TV/API27-AVD's aanwezig; niet gestart wegens de strikte fase-1-ADB-grens.
- Python: `C:/Users/nadal/anaconda3/python.exe`, uitsluitend voor audit, inventarisatie en artifactbundeling. Bouw vereist geen Python.
- Git en Node aanwezig; Node niet nodig. Gradle 8.14.3 reeds gecachet; AGP 8.13.0 en Kotlin 2.2.10 gekozen als compatibele vaste versies bij bestaande toolchain.
- Productie: native Kotlin/framework Views + RecyclerView 1.4.0; compileSdk36, targetSdk34, minSdk26.
- JVM/UI-testomgeving: JUnit 4.13.2, Robolectric 4.16.1, Android API34 met native graphics en TV-layout 960×540dp, gerenderd naar 1920×1080px. Geen echte videodecoder/DRM of firmware.

De wrapper-JAR is vergeleken met de officiële Gradle 8.14.3-wrapperhash: `7d3a4ac4de1c32b59bc6a4eb8ecb8e612ccd0cf1ae1e99f66902da64df296172`. Overeenkomst geslaagd. De officiële distributiehash van `gradle-8.14.3-all.zip`, `ed1a8d686605fd7c23bdf62c7fc7add1c5b23b2bbc3721e661934ef4a4911d7c`, staat als `distributionSha256Sum` in wrapperproperties. Primaire bron: [Gradle-distributies](https://services.gradle.org/distributions/).

Target34 is bewust voor Android TV14/sideload uit de briefing; deze app claimt geen huidige Play Store-targetvereisten. De waarschuwingen over nieuwere tool-/testversies zijn geen aanleiding tot een ongecontroleerde platformmigratie. Native fullscreen gebruikt compatibele legacyflags voor oudere TV-API's. Synchroon committen van de eenmalige bootmarker voorkomt een startloop bij procesdood vóór de marker is weggeschreven.
