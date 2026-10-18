# Installation und Betrieb

[English](../en/OPERATIONS.md) | Betriebsanleitung für einen einzelnen Pilotserver; keine Produktionszertifizierung.

## Lokale Demo

Die Startskripte im Hauptverzeichnis erzeugen ein zufälliges Datenbankpasswort in der ignorierten `.env`, bauen Client/JAR, starten PostgreSQL und Anwendung und warten auf den Gesundheitsstatus. Voraussetzung sind Docker mit Compose und Netzwerkzugriff auf npm, Maven und Container-Registries. Für native Entwicklung werden Java 25, Node 24.22.0 und die Voraussetzungen des Maven-Startskripts benoetigt. Eine belastbare Produktionskapazitaet wurde nicht gemessen.

Die Demo verwendet HTTP auf `127.0.0.1:8080`, `BF_DEMO=true` und für lokales HTTP deaktivierte Secure-Cookies. Die Datenbank hat ohne explizite `compose.dev.yml` keinen Host-Port. Das PostgreSQL-18-Volume liegt unter `/var/lib/postgresql`. `docker compose down` stoppt ohne Volumenlöschung. `docker compose down -v` löscht dagegen die Datenbank und darf nicht auf benoetigte Daten angewendet werden.

Logs: `docker compose logs --tail=150 app db`. Gesundheit: `curl --fail http://localhost:8080/actuator/health`. Interne Gesundheitsdetails, Zugangsdaten und SQL-Abfragen nicht öffentlich freigeben.

## Native Entwicklung

```bash
# Zuerst .env mit einem eigenen privaten Datenbankpasswort erzeugen.
docker compose -f compose.yml -f compose.dev.yml up -d db
export BF_DATABASE_URL=jdbc:postgresql://localhost:5432/belegflow
export BF_DATABASE_USER=belegflow
export BF_DATABASE_PASSWORD='das-gleiche-private-passwort'
export BF_DEMO=true
export BF_COOKIE_SECURE=false
./mvnw install -DskipTests
./mvnw -pl server spring-boot:run
```

In einem zweiten Terminal `cd frontend && npm install && npm run dev` ausführen. Die dort angezeigte Vite-Adresse oeffnen; `/api` wird nach 8080 weitergeleitet. Ein einzelnes Server-JAR mit Webdateien entsteht mit `sh scripts/build.sh`. `BF_INTEGRATION=true` niemals gegen eine Kundendatenbank setzen; diese Tests erwarten separate synthetische Daten.

## Konfiguration

| Variable | Bedeutung |
|---|---|
| BF_DATABASE_URL | PostgreSQL-JDBC-Adresse mit freigegebener Netzwerk-/TLS-Konfiguration. |
| BF_DATABASE_USER / BF_DATABASE_PASSWORD | Datenbankzugang; echte Werte niemals committen. |
| BF_DEMO | In der Anwendung standardmaessig false. True legt bei leerer Datenbank öffentliche Beispielkonten an. |
| BF_COOKIE_SECURE | Standard true. False nur für lokales HTTP. |
| BF_BOOTSTRAP_EMAIL | Erste administrative E-Mail bei leerer Nicht-Demo-Installation. |
| BF_BOOTSTRAP_PASSWORD | Initial mindestens 16 Zeichen; empfohlen ist ein eigener zufälliger Wert mit mindestens 24 Zeichen. |
| BF_BOOTSTRAP_ORGANIZATION | Name der ersten Organisation. |
| BF_FLYWAY_ENABLED | Standard true; nur mit eigenem kontrolliertem Migrationsprozess deaktivieren. |
| BF_REMINDER_DELAY_MS | Standard 900000, also 15 Minuten für interne Erinnerungen. |
| PORT | Standard 8080; der mitgelieferte Healthcheck erwartet 8080. |
| BF_INTEGRATION | Ausschliesslich Testschalter für echte Datenbankintegration. |

Bootstrap erfolgt nur bei leerer Organisationstabelle. Spaetere Umgebungsvariablen ändern keine bestehenden Benutzerpasswoerter. Konten über die Administration ändern. Auch ein geändertes `.env`-Datenbankpasswort rotiert nicht automatisch das Passwort eines vorhandenen PostgreSQL-Volumes. Zuerst die Datenbank kontrolliert umstellen, dann das Anwendungsgeheimnis aktualisieren.

## Kontrollierter Pilotbetrieb

Vor echten Kundendaten die Freigabekriterien abschliessen. Andere Installation und Volumes als für die Demo verwenden. `.env.production.example` in eine private Datei kopieren, Platzhalter ersetzen und Zugriff einschraenken. Die Produktionsdatei ist eigenstaendig, kein Demo-Override:

```bash
docker compose --env-file /secure/path/belegflow.env \
  -f compose.production.yml up --build -d --wait
```

Die Anwendung bleibt an Host-Loopback gebunden. Davor ist ein eigener HTTPS-Reverse-Proxy mit echtem Zertifikat, Domain, Zeit-/Größenlimits und Ratenbegrenzung erforderlich. Statische Dateien und `/api` müssen denselben Ursprung verwenden. Secure-Cookies benoetigen HTTPS. Keine pauschale CORS-Freigabe, keine öffentliche Datenbank und keine ungeschuetzte Managementoberflaeche.

Der eingebaute Anmeldelimiter verwendet die direkte Gegenadresse. Ein Proxy kann dadurch mehrere Benutzer zu einer Adresse zusammenfassen. Die Begrenzung am Proxy bewusst konfigurieren und testen, statt beliebigen Forwarded-Headern zu vertrauen.

Umgebungsvariablen sind für privilegierte Containerbetreiber einsehbar. Für kommerziellen Betrieb eine freigegebene Geheimnisverwaltung vorsehen. Nach der Initialisierung Bootstrap-Geheimnisse aus der Dauer-Konfiguration entfernen und den Neustart testen. Die bestehende Organisation braucht diese Werte nicht. Den einzigen verifizierten Administratorzugang niemals verlieren.

Das Beispielkonto besitzt das Schema, damit Flyway migrieren kann. Das ist eine dokumentierte Demo-/Pilotvereinfachung. Eine gehaertete Installation sollte Migration/Eigentuemer und Laufzeitrolle trennen, Laufzeitrechte auf benoetigte Tabellen/Sequenzen begrenzen und DDL sowie Triggeränderungen verhindern. Eine fertige Least-Privilege-/RLS-Installation wird nicht behauptet. Migrationen und Laufzeit mit den echten Rollen testen.

Diese Anleitung unterstuetzt eine Instanz. Sitzungen und Anmeldelimiter liegen im Speicher; ein Neustart meldet Benutzer ab. Mehrere Replikate brauchen bewusst entwickelte gemeinsame Sitzungen, Jobkoordination und Limiter. Ein Load-Balancer allein liefert keine Hochverfügbarkeit.

## Organisation einrichten

Finance-, Freigabe- und Administrationskonten anlegen. E-Mail-Adressen sind eindeutig und stehen in dieser Version jeweils für ein Organisationsmitglied. Lieferanten, verantwortete Projekte und Kostenstellen anlegen. Bruttoschwellenwert und befristete Vertretungen festlegen. Bei hohen Rechnungen muss eine unabhängige administrative Person vorhanden sein, die weder Ersteller noch erste Freigabe ist. Eine falsche Personalzuordnung wird absichtlich blockiert statt still umgangen.

Es gibt keine Selbstregistrierung, Abonnementabrechnung oder Einladungs-E-Mail. Bootstrap legt eine Organisation an. Schema und Dienste sind mandantenbezogen, die Demo hat einen zweiten isolierten Mandanten für Tests; die Anlage weiterer Organisationen ist aber keine Benutzerfunktion.

## Sicherung und Wiederherstellung

Originale und Exportpakete liegen als PostgreSQL-`bytea` vor. Ein konsistenter Datenbankdump umfasst daher Metadaten und diese Bytes. Lokales Hilfsskript: `sh scripts/backup.sh`. Es erzeugt einen Dump mit restriktiven Dateirechten, keine verschlüsselte externe Sicherung. Freigegebene Kopien verschlüsseln, ausserhalb des Hosts speichern, Aufbewahrung festlegen und Aktualitaet überwachen. Geheimnisse/Konfiguration getrennt sichern.

Jede Strategie durch Wiederherstellung in eine **separate wegwerfbare Testdatenbank** prüfen. Niemals die einzige Produktionskopie überschreiben:

```bash
# Für die gewuenschte Umgebung passende Compose-/Env-Optionen verwenden.
docker compose exec -T db createdb -U belegflow belegflow_restore_check
cat backups/your-verified.dump | docker compose exec -T db \
  pg_restore -U belegflow -d belegflow_restore_check --no-owner --exit-on-error
```

Eine getrennte Testanwendung auf anderem Port darauf starten. Organisations-/Benutzeranzahlen, Rechnungszustaende, Originalhashes, Exportdownload und Verlauf prüfen. Dauer und Datenstand dokumentieren. RPO/RTO sind Geschaeftsentscheidungen und wurden hier nicht gemessen. Nur die wegwerfbare Testdatenbank nach erfolgreicher Prüfung bereinigen.

## Aktualisierung und Rücknahme

Änderungen, Schwachstellenmeldungen, Lockfile und Image-Digests prüfen; komplette CI ausführen. Sicherung und Restore-Test, Staging, Migrationen und repraesentative Arbeitsablaeufe vor dem Rollout durchführen. Eine bereits eingesetzte Flyway-Datei nicht ändern, sondern eine neue Version anfuegen. Ein altes JAR kann nach irreversibler Migration ungeeignet sein. Verifizierte separate Wiederherstellung oder geprüften Vorwaertsfix verwenden, keine improvisierten Löschbefehle.

## Überwachung und Stoerungen

Gesundheit, Fehlerrate, Laufzeit, Datenbankplatz/-verbindungen, Fehlanmeldungen und Sicherungsalter überwachen. Die Anwendung enthält nur einen kleinen Health-Endpunkt, keinen gehosteten Monitoringdienst oder vollständige OpenTelemetry-Installation. Unerwartete Fehler protokollieren Referenz und Fehlerklasse, nicht vertrauliche Rechnungsinhalte oder SQL-Fehlermeldungen. Unter geregeltem Zugriff mit Zeitpunkt und Anfragekontext korrelieren.

Erinnerungen sind tageweise dedupliziert, kein verteilter Scheduler. Exportwiederholungen verwenden denselben Idempotenzschlüssel und dieselben Daten. Nach Versionskonflikten neu laden statt veraltete Entscheidungen automatisch zu wiederholen.

## Datenschutz, Aufbewahrung und Vorfaelle

Verantwortlichkeiten, Hostingzugriffe, Auftragsverarbeitung, Unterauftragnehmer, Transfers, Vertragsunterlagen und Aufbewahrung fachlich/rechtlich beurteilen lassen. EU-Hosting allein ist keine Datenschutz-Zertifizierung. Standardmaessig gibt es keine externe Analyse oder KI-Datenübertragung.

Trigger verhindern gewoehnliche Änderungen/Löschungen an Dokument-, Audit- und Exportzeilen. Das ist kein WORM-Speicher, kein Schutz gegen jeden DBA und kein vollständiges rechtliches Archiv. Automatische Aufbewahrungslöschung, Legal Holds, Betroffenenlöschung und Organisationsbereinigung sind nicht implementiert. Vor echten personenbezogenen Daten die benoetigten administrativen Verfahren definieren, implementieren und testen. Die ganze Datenbank zu löschen ist kein regulaeres Löschkonzept.

Bei Verdacht auf Kompromittierung Zugriff begrenzen, angemessene Beweise erhalten, Geheimnisse kontrolliert rotieren, betroffene Daten und Organisationen ermitteln und den vorgesehenen Melde-/Rechtsprozess einschalten. Keine vertraulichen Belege in GitHub-Issues veröffentlichen. Siehe SECURITY.md und die Freigabekriterien.
