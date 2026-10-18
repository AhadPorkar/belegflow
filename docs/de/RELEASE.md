# Freigabekriterien und kommerzielle Grenze

Version 0.2.0 ist ein Entwicklungs-/Quellcodekandidat. Die folgenden Kriterien sind offen; es werden keine bestandenen Builds erfunden.

| Kriterium | Erforderlicher Nachweis vor Kundendaten |
|---|---|
| Abhängigkeiten | npm-Lockfile aufloesen, prüfen und committen; Maven-Artefakte, transitive Lizenzen und Container-Digests erfassen. |
| Netzwerk-Build | CI mit Java 25, Spring, echtem PostgreSQL, modernem React und allen Unit-/Integrations-/Browsertests erfolgreich. |
| Sicherheit | Bedrohungsmodell, Mutationen, Downloads, Mandantentrennung, CSRF, Sitzungen, Passwortwechsel, Uploads und Header prüfen. |
| Dokumente | Bei Bedarf gepflegten Konformitaetsvalidator und Malware-/Quarantaeneprozess ergänzen; Ausnahmen abstimmen. |
| Betrieb | HTTPS, Geheimnisverwaltung, Datenbank-Minimalrechte, verschlüsselte externe Sicherung, gemessener Restore, Monitoring und Vorfallverantwortung. |
| Datenschutz | Zwecke, Vertraege, Unterauftragnehmer, Aufbewahrung, Löschung, Legal Holds und Berechtigungen für den echten Betrieb prüfen. |
| Barrierefreiheit | Automatische und manuelle Tastatur-/Screenreader-/Kontrastprüfung. AA ist ein Ziel, keine bestätigte Zertifizierung. |
| Buchhaltung | CSV-Mapping und Steuerbehandlung fachlich freigeben; keine DATEV-Livebehauptung ohne echte Integrationstests. |
| Geschaeft | Bezahlten Pilotumfang, Preise, Support, Bedingungen, Kuendigung/Datenexport und Einrichtung vereinbaren. |
| Eigentum | Generierten Code, MIT-Vorschlag, Markenname und Portfolioaussagen selbst prüfen. |

Nicht implementiert sind Checkout/Abrechnung, E-Mail, Selbstregistrierung, SSO/MFA, vollständige E-Rechnungskonformitaet, OCR, Zahlungen, verteilter Betrieb, automatische Aufbewahrungslöschung und Legal Holds. Produktionsendpunkte taeuschen diese Funktionen nicht durch erfolgreiche Mockantworten vor.

Nach den passenden Prüfungen ist ein kontrollierter Pilot mit manueller Einrichtung und vertraglicher Abrechnung denkbar. Das Paket allein ist aber kein fertig betriebener Abonnementdienst. Preisannahmen sind keine Marktnachweise.

Zuerst synthetische oder redigierte Dokumente verwenden, echte Formate und Buchhaltungsanforderungen prüfen und Bearbeitungszeit, Freigabeverzoegerung, offene Ausnahmen und Einrichtungsaufwand messen. Verantwortung für Vertretung, zweite Freigabe, problematische Dateien und Rücknahme einer Version eindeutig festlegen.


## Pruef- und Upgradepflicht für 0.2.0

Vor Pilotbetrieb [Tests](TESTING.md) vollständig ausführen, strikte Coverage einschalten, Berichte prüfen und die Lockdatei committen. [Fehlerdiagnose](TROUBLESHOOTING.md) erklaert den YAML-Startfehler; Datenvolumes nicht loeschen. Der Erinnerungsjob startet standardmaessig nach 30 Sekunden; `BF_REMINDER_INITIAL_DELAY_MS` ist konfigurierbar. Keine Schemaaenderung. Automatischer Erfolg ersetzt keine manuelle Sicherheits-, Datenschutz-, Barrierefreiheits- und Wiederherstellungspruefung.
