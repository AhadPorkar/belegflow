# Tests, Coverage und Freigabeprüfung

Version 0.2.0 | [English](../en/TESTING.md) | [Modulreferenz](MODULE_REFERENCE.md) | [Tatsächliche Ergebnisse](../testing/VERIFICATION.md)

## Umfang der Modulprüfung

Jedes vorhandene Fachmodul besitzt benannte Tests: Identität und Sicherheit, Lieferanten/Projekte/Kostenstellen, Rechnungsprüfung und Zuordnung, Import und Dateien, Leistungsnachweise, Klärung/Dubletten, Freigaben/Vertretung, Buchhaltungspakete, Audit, Erinnerungen, Initialisierung und Fehlerbehandlung. Frontend-Tests behandeln jede Fachseite, Dialoge, Navigation, gemeinsame Komponenten, HTTP-Client, Zustandsverwaltung, Formatierung und Übersetzungen. Auch die Konfiguration hat Regressionstests.

Eine Fallliste ist keine gemessene Coverage. Ein gemockter Service führt keine SQL-Transaktion aus. React/jsdom beweist weder das echte Browserlayout noch native Formularvalidierung. Syntaxprüfung ist keine Java-Kompilierung. Die Offline-Vorschau ist kein Produktionsserver. Ergebnisse deshalb nach Schicht lesen und nicht alle Zahlen zu einem scheinbar vollständigen Erfolg addieren.

## Empfohlener Gesamtlauf mit Docker

Docker Engine/Desktop mit Compose und Netzwerkzugriff auf die verwendeten Registries installieren. Für diesen Weg sind Java, Maven und Node auf dem Host nicht erforderlich. Im Repository-Hauptverzeichnis ausführen:

```powershell
powershell -ExecutionPolicy Bypass -File .\scriptserify-all.ps1
```

```bash
# Linux / macOS / WSL: ausschliesslich den separaten Teststack stoppen.
docker compose -f compose.quality.yml down
docker compose -f compose.quality.yml up --build --abort-on-container-exit --exit-code-from verifier
# Nach Prüfung des Rueckgabecodes:
docker compose -f compose.quality.yml down
```

Das Projekt `belegflow-quality` verwendet `belegflow_test` auf einem temporären PostgreSQL-Dateisystem. Es greift nicht auf das Anwendungsvolume `pgdata` zu und veröffentlicht keinen Datenbankport. Das bekannte Testpasswort ist kein Produktionsgeheimnis. Der Helfer verlangt `BF_TEST_DATABASE_ACK=DISPOSABLE` und lehnt andere Datenbanknamen ab. Nebenläufigkeitstests dürfen ihre synthetischen Zeilen bis zum Entfernen dieses Teststacks behalten.

Der Lauf installiert Abhängigkeiten, prüft Typen, führt Core-/React-Tests mit V8-Coverage aus, baut den echten React-Client, prüft Maven/JUnit/PostgreSQL mit JaCoCo, kontrolliert die Ausführung aller Datenbanksuites, startet das gebaute Spring-Artefakt und führt 14 echte Browser-Spezifikationen aus. Fehler stoppen den Lauf; vorhandene Berichte werden trotzdem gesammelt. Die Offline-Demo ersetzt keinen fehlgeschlagenen Build.

## Befehle nach Schicht

| Schicht | Befehl ab Repository-Hauptverzeichnis | Inhalt |
|---|---|---|
| Domäne ohne Downloads | `sh scripts/test-domain-offline.sh` | 306 deterministische Fälle mit JDK 21+ kompatiblem Helfer. |
| Konfigurationsregression | `./mvnw -Dtest=JacksonConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test` | Wirkliches YAML, ISO-Daten und reproduzierter Altfehler. |
| Backend-Units | `./mvnw test` | Domänen- und Server-Units; Datenbanktests benötigen das explizite Flag. |
| Frontend-Core | `cd frontend && npm run test:core` | 92 Fälle für HTTP, Zustand, Formatierung und Sprache. |
| React-Komponenten | `cd frontend && npm run test:components` | 132 expandierte Fälle mit React und jsdom. |
| Frontend-Coverage | `cd frontend && npm run test:coverage` | Node- und DOM-Projekte, einschließlich nicht importierter Produktivdateien. |
| Echte Browser-Journeys | `cd frontend && npm run test:e2e` | 14 Spezifikationen gegen die laufende Anwendung, nicht DemoApi. |
| Gesamtlauf | `sh scripts/verify-all.sh` | Alle Schichten mit vorbereiteter Toolchain und wegwerfbarer Datenbank. |

Keine parallelen Gesamtläufe gegen dieselbe Datenbank starten. Fachliches Verhalten darf nicht von der Testreihenfolge abhängen. Transaktionale Datenbankfälle rollen zurück; Nebenläufigkeitstests verwenden eigene Organisationen. Browserformulare erzeugen eindeutig benannte Testdaten. Der automatische Erinnerungsjob ist während der Tests verzögert; spezielle Tests rufen ihn gezielt auf.

Für einen manuellen Gesamtlauf `BF_DATABASE_URL=jdbc:postgresql://localhost:5432/belegflow_test`, passende Zugangsdaten, `BF_INTEGRATION=true` und `BF_TEST_DATABASE_ACK=DISPOSABLE` setzen. Konfigurierte Java-/Node-Version und Chromium installieren: `npx playwright install --with-deps chromium`. Docker erledigt diese Vorbereitung automatisch.

## Coverage und Artefakte

JaCoCo: `domain/target/site/jacoco/` und `server/target/site/jacoco/`. V8: `frontend/coverage/`. Ausgeschlossen sind nur die nicht ausführbaren Typdeklarationen `models.ts` und der durch echte Browser-Journeys verwendete Einstieg `main.jsx`. Keine Fachklasse wird wegen schlechter Abdeckung ausgeblendet. JSX-Ansichten werden ausgeführt, sind aber nicht Bestandteil der strikten TypeScript-Kompilierung.

Mit `-StrictCoverage` beim PowerShell-Helfer oder `BF_STRICT_COVERAGE=true` die zusätzliche Freigabegrenze aktivieren. Backend: mindestens 80 Prozent Zeilen und 70 Prozent Verzweigungen je Maven-Bundle. Frontend: 80 Prozent Anweisungen/Funktionen/Zeilen und 70 Prozent Verzweigungen. Das sind konfigurierte Ziele, keine nachgewiesenen Messwerte. Ein hoher Gesamtwert ersetzt keine Modulprüfung.

`quality-results/` enthält JUnit-XML, HTML-/maschinenlesbare Coverage, Playwright-Traces und Screenshots bei Fehlern, Serverlogs, gegebenenfalls die erzeugte Lockdatei sowie `exit-code.txt`. Traces mit `npx playwright show-trace PFAD` ansehen. Keine vertraulichen Rechnungen aus Screenshots oder Logs committen.

`scripts/assert-integration-reports.py` verlangt `IntegrationTest` mit 15, `ModuleIntegrationTest` mit 29 und `ConcurrencyIntegrationTest` mit 3 Fällen. Fehlende, übersprungene oder fehlgeschlagene Berichte stoppen den Lauf. Diese Kontrolle nicht entfernen, um ein grünes Ergebnis zu erhalten.

## GitHub Actions und reproduzierbare Builds

Die drei Jobs prüfen Jackson ohne Datenbank, Frontend samt Build und anschliessend den vollständigen Stack. Alle liefern Artefakte. Der Frontend-Job führt zusätzlich `npm audit --audit-level=high` aus; eine Schwachstelle kann den Lauf trotz erfolgreicher Funktionstests stoppen. Per Workflow-Dispatch lässt sich strikte Coverage anfordern. Der normale Lauf misst Coverage, bestätigt aber nicht automatisch die optionale Freigabegrenze oder manuelle Prüfungen.

Das Archiv enthält keine erfundene Lockdatei. Der erste Netzwerklauf erzeugt sie. Abhängigkeiten prüfen, Lockdatei committen, mit `npm ci` erneut testen und Commit-SHA sowie Artefakte zur Freigabe aufbewahren. Ein Badge muss den tatsächlichen Workflow anzeigen.

## Neue Tests schreiben

Mit einer verletzten Regel und einem kleinen fehlschlagenden Fall beginnen. Deterministische Regeln in `domain`, Adapter und Rechte in Modul-Units, SQL und Transaktionen in `ModuleIntegrationTest`, Rennen in `ConcurrencyIntegrationTest`. Im Client Zustand über `FixtureApi`, Ansichten über React Testing Library und wichtige Gesamtwege über Playwright prüfen. Sichtbare Sprachänderungen in EN und DE absichern.

Unabhängige Fixtures, exakte Dezimalzeichenfolgen, kontrollierte Uhren und synthetische IDs verwenden. Erlaubtes Ergebnis und verbotene Nebenwirkung prüfen. Keine Sleeps zum Verdecken von Rennen und nicht den zu prüfenden Service selbst mocken. Eine Regression muss vor der Korrektur scheitern und danach in der passenden Umgebung bestehen.

## Manuelle Freigabeschritte

Tastatur/Screenreader, Barrierefreiheit, Bedrohungsmodell, Abhängigkeiten/Lizenzen, Sicherheitsprüfung, Wiederherstellung einer Sicherung, TLS/Geheimnisse, echte Abstimmung des Buchhaltungsexports sowie Datenschutz/Aufbewahrung bleiben erforderlich. Testabdeckung ist keine rechtliche Zertifizierung.


## Selbsttest der Berichtskontrolle

`python3 -m unittest discover -s scripts/tests -p 'test_*.py' -v` prueft die Berichtskontrolle mit neun isolierten Faellen. Ihr XML ist bewusst synthetisch und darf nie als Anwendungs-Surefire-Bericht verwendet werden. Der Gesamtlauf fuehrt diese Pruefung ebenfalls aus.
