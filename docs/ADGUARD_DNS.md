# AdGuard Private DNS — ingesteld op Xiaomi

8 november 2026. Gebruiker gaf na uitleg over gratis openbare DNS en tv-brede filtering akkoord: "zet adguard er maar op hoor". Geen AdGuard-app, VPN of certificaat geïnstalleerd; Androids ingebouwde Privé-DNS ingesteld op hostname dns.adguard-dns.com.

Oorspronkelijke waarden private_dns_mode en private_dns_specifier waren beide unset/null. Herstelbestand: artifacts/phase2/npo-analysis/private-dns-before.json, inclusief apparaatfingerprint. Herstellen: scripts/restore-dns.ps1 -Execute (standaard zonder Execute alleen dry-run).

Werkelijke verificatie op de stick: ConnectivityService meldt PrivateDnsServerName dns.adguard-dns.com en gevalideerde IPv4/IPv6-adressen van AdGuard. googleads.g.doubleclick.net en pagead2.googlesyndication.com worden via de systeemresolver naar 127.0.0.1 afgevangen. npo.nl resolveert naar een publiek CDN-adres en reageerde op de bereikbaarheidstest. Details: artifacts/phase2/npo-analysis/adguard-device-check.json. Dit bewijst domeinfiltering, niet dat alle NPO-advertenties verdwenen zijn.

Filtering geldt voor alle apps die Androids systeem-DNS gebruiken. Android Privé-DNS heeft geen afzonderlijke NatjeTube-uitzondering; NatjeTubes eigen reclameblokkering blijft bestaan. De openbare DNS-dienst is gratis, geen account gebruikt. Primaire providerinformatie: https://adguard-dns.io/en/public-dns.html .

PCAPdroid is uitsluitend op de pc gedownload; installatie/start van de capture werd door automatische goedkeuringscontrole geweigerd. Deze DNS-wijziging is een later, expliciet door de gebruiker geautoriseerde domeinblokkering. Er draait geen pakketopname en HTTPS wordt niet ontsleuteld. Geen garantie voor advertenties die via andere of gedeelde videodomeinen worden aangeleverd. Test met een nieuwe NPO-videosessie; bestaande verbindingen kunnen nog doorlopen.
