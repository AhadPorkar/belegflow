# Fehlerdiagnose und Aktualisierung auf 0.2.0

[English](../en/TROUBLESHOOTING.md) | [Tests](TESTING.md) | [Betrieb](OPERATIONS.md)

## Ursache des hochgeladenen GitHub-Actions-Fehlers

Der erste relevante Fehler in `full-stack/8_Spring, domain and real PostgreSQL integration tests.txt` ist eine fehlgeschlagene Konfigurationsbindung:

```text
Failed to bind properties under 'spring.jackson.serialization'
No enum constant tools.jackson.databind.SerializationFeature.write-dates-as-timestamps
```

Der Anwendungskontext startet nicht. Deshalb schlagen alle 15 Integrationstests fehl; es sind nicht 15 unabhängige Fachfehler. Die späteren Meldungen zum Context-Failure-Threshold sind Folgefehler. Im hochgeladenen Lauf waren 82 Domänentests, 12 Adaptertests, 24 Clienttests und der Frontend-Produktionsbuild erfolgreich. Das ist historische Evidenz, kein Nachweis für diese geänderte Version.

In `server/src/main/resources/application.yml` den alten Eintrag `serialization.write-dates-as-timestamps` entfernen und Folgendes verwenden:

```yaml
spring:
  jackson:
    datatype:
      datetime:
        write-dates-as-timestamps: false
```

Jackson 3 hat diese Option aus `SerializationFeature` verschoben. Ein anderer Boolean-Wert, ein höherer Failure-Threshold, deaktivierte Integrationstests oder zusätzliches Jackson 2 beheben diese Unvereinbarkeit nicht. Auch eine alte Umgebungsvariable `SPRING_JACKSON_SERIALIZATION_WRITE_DATES_AS_TIMESTAMPS` entfernen. Datumswerte und Zeitpunkte bleiben ISO-Zeichenfolgen. `JacksonConfigurationTest` prüft das wirkliche YAML sowie beide Ausgabeformen ohne Datenbank.

## Quellcode aktualisieren, Daten erhalten

Wichtige Daten zuerst sichern. Das neue Archiv in ein sauberes Verzeichnis entpacken. Eigene Anpassungen vergleichen und bewusst übernehmen; Geheimnisse aus einer sicheren Quelle kopieren, nicht aus Git. Kein zweites `belegflow/` innerhalb des vorhandenen Repositorys erzeugen. Quellcode, Tests, Workflow und Dokumentation gemeinsam committen. Version 0.2.0 enthält keine geänderte oder neue Datenbankmigration.

Zuerst den isolierten Qualitätslauf ausführen, dann die Anwendung mit `docker compose up --build -d` neu bauen. `docker compose logs app` prüfen. Wertvolle Datenvolumes nicht mit `docker compose down -v` löschen, um einen Jackson-Konfigurationsfehler zu beheben. Der Qualitätsstack besitzt einen eigenen Namen und eine separate, wegwerfbare Testdatenbank.

GitHub führt zuerst die datenbankfreie Konfigurationsregression und die Frontend-Prüfungen aus. Erst danach folgen PostgreSQL- und echte Browser-Tests. Bei einem Fehler die Artefakte herunterladen und mit der ersten Ursache beginnen, nicht mit der letzten Maven-Zusammenfassung.

## Häufige Probleme

| Symptom | Prüfung und Vorgehen |
|---|---|
| Jackson meldet weiterhin `serialization` | YAML, Umgebungsvariablen und Deployment-Overrides nach dem alten Schlüssel durchsuchen; Image neu bauen. |
| Integrationstests sind übersprungen | Den Qualitätsbefehl mit `BF_INTEGRATION=true` verwenden. Ein normales `mvn verify` ist kein vollständiger Prüflauf. |
| Abhängigkeiten oder Images nicht erreichbar | Registry, Proxy, DNS und Toolchain prüfen. Ein fehlgeschlagener Download wird nicht als Erfolg ersetzt. |
| PostgreSQL nicht erreichbar | Healthcheck und Logs der Testdatenbank prüfen; der sichere Prüflauf verlangt den Namen `belegflow_test`. |
| Vitest oder TypeScript fehlt | Unterstützte Node-Version verwenden und Abhängigkeiten installieren. Geprüfte Lockdatei committen, danach `npm ci`. |
| Coverage-Grenze nicht erreicht | HTML-Bericht lesen und fehlende fachliche Zweige testen; Grenzwerte nicht nur für ein grünes Badge senken. |
| Anmeldung bleibt im Formular | HTTP-Status, Cookies und TLS-Konfiguration prüfen; lokales HTTP benötigt die ausdrückliche Testkonfiguration. |
| PDF-Import: `INVALID_INPUT` | Lieferant, Rechnungsnummer, gültiges Rechnungs-/Fälligkeitsdatum und genaue Summen eingeben. API-Datumsformat: `YYYY-MM-DD`. |
| `VERSION_CONFLICT` | Aktualisieren, Verlauf prüfen und erst dann erneut entscheiden. |
| Idempotenzkonflikt | Gleichen Schlüssel nur mit unveränderten Rechnungs-IDs und Versionen wiederholen. |
| Keine Erinnerungs-E-Mail | Die Anwendung erzeugt interne Hinweise, keine E-Mails. |

Keine Passwörter, Cookies oder vertraulichen Rechnungen in öffentliche Issues kopieren. Version, Befehl, erste Ausnahme, Referenznummer und bereinigte Zeitangaben angeben.

## Technische Quellen

* Spring Boot JSON: https://docs.spring.io/spring-boot/reference/features/json.html
* Jackson-Datentypkonfiguration: https://docs.spring.io/spring-boot/api/java/org/springframework/boot/jackson/autoconfigure/JacksonProperties.Datatype.html
* Jackson 3 DateTimeFeature: https://javadoc.io/doc/tools.jackson.core/jackson-databind/latest/tools.jackson.databind/tools/jackson/databind/cfg/DateTimeFeature.html
