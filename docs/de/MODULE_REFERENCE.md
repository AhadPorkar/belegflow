# Modulreferenz und Erweiterungsvertrag

Version 0.2.2 | [English](../en/MODULE_REFERENCE.md) | [Deutsch](../de/MODULE_REFERENCE.md)

Die folgenden Grenzen beschreiben die implementierte Anwendung, kein zukünftiges ERP. Pfade sind relativ zum Repository. Bei Java-Paketpfaden gilt `server/src/main/java/com/belegflow/server/`; Domänenklassen liegen unter `domain/src/main/java/com/belegflow/domain/`. Die API-Referenz enthält Payloads, der Quellcode die exakten Signaturen.

[API](../api/API.de.md) | [Code map](../code/CODE_MAP.md) | [Tests](TESTING.md)

## 1. Identität und Sitzungssicherheit

**Quellcode**: `identity/IdentityService.java`; `identity/SecurityConfiguration.java`; `identity/SecurityPrincipal.java`; `identity/LoginRateLimitFilter.java`; `identity/SessionController.java`.

**API**: `/api/csrf, /api/login, /api/logout, /api/session, /api/session/locale, /api/public/config`.

**Daten**: `app_user, organization; server session; no client-selected tenant`.

Aktives Konto und Authentifizierungsversion bei geschützten Aktionen prüfen. Rollen und Organisation aus der serverseitigen Identität ableiten, nicht aus Clientfeldern. Unsichere Requests benötigen CSRF. Anmeldung begrenzen; Sprache nur für das eigene Konto ändern.

**Anwendung und Erweiterung**: Neue Anmeldungsmethoden mit Session-Widerruf und Fremdorganisationszugriff testen. SSO braucht eine bewusste Principal-Zuordnung und Sicherheitsprüfung, keine Umgehung von IdentityService.

**Backend-Tests**: `IdentityModuleTest, LoginRateLimitFilterTest, IntegrationTest, ModuleIntegrationTest`.

**Frontend-Tests**: `identity.test.jsx; shell-app.test.jsx; modules.test.mjs; http.test.mjs`.

## 2. Lieferanten, Projekte, Kostenstellen und Team

**Quellcode**: `catalog/CatalogService.java`; `catalog/CatalogController.java`.

**API**: `/api/catalog, /api/suppliers, /api/projects, /api/cost-centers, /api/users, /api/settings/rules`.

**Daten**: `supplier, project, cost_center, app_user, organization`.

Finance/Administration pflegt Stammdaten; Team und Schwellenwert sind der Administration vorbehalten. Projektverantwortliche müssen aktive, geeignete Mitglieder derselben Organisation sein. Kontowechsel widerrufen bestehende Sitzungen. Selbstdeaktivierung/-herabstufung ist gesperrt. Speichern und Audit sind atomar.

**Anwendung und Erweiterung**: Vor Zuordnung Lieferant, Projekt und Kostenstelle anlegen. Neue Felder gemeinsam in DTO, Validierung, Migration, API-Referenz, beiden Sprachen und Tests ergänzen.

**Backend-Tests**: `CatalogModuleTest, IdentityModuleTest, ModuleIntegrationTest`.

**Frontend-Tests**: `catalog-settings.test.jsx; dialogs.test.jsx; modules.spec.js`.

## 3. Rechnungseingang, Prüfung und Zuordnung

**Quellcode**: `invoices/InvoiceRepository.java`; `invoices/InvoiceService.java`; `invoices/InvoiceController.java`.

**API**: `/api/invoices, /api/invoices/summary, /api/invoices/{id}, /api/invoices/{id}/allocation, /api/invoices/{id}/commands`.

**Daten**: `invoice plus tenant-scoped supplier/project/user joins`.

Listen und Details sind organisationsbezogen und paginiert. Eine UUID verleiht kein Zugriffsrecht. Änderungen sperren den Datensatz und prüfen die erwartete Version. Zuordnung nur im bearbeitbaren Zustand; Projektwechsel setzt die Leistungsabnahme zurück. Originalbeträge werden nicht still umgeschrieben.

**Anwendung und Erweiterung**: allowed_actions und Bereitschaft nur zur Darstellung nutzen; serverseitig erneut prüfen. Explizite Befehle statt frei schreibbarer Statuswerte ergänzen und Rollen-/Statusmatrix sowie Versionskonflikte erweitern.

**Backend-Tests**: `InvoiceModuleTest, InvoiceRepositoryTest, IntegrationTest, ModuleIntegrationTest, ConcurrencyIntegrationTest`.

**Frontend-Tests**: `workspace.test.jsx; dialogs.test.jsx; modules.test.mjs; workflows.spec.js`.

## 4. Originalimport und Dokumenterhaltung

**Quellcode**: `documents/InvoiceImportService.java`; `documents/DocumentService.java`; `documents/DocumentController.java`.

**API**: `/api/invoices/import, /api/documents/{id}`.

**Daten**: `document (bytea, SHA-256, metadata), invoice, supplier`.

Dateigröße vor Verarbeitung begrenzen. Sichere UBL-/CII-Extraktion ist keine vollständige Normprüfung. PDF-Felder manuell eingeben; ungültige Daten sind Validierungsfehler. Bytegleicher Import nutzt den Bestand; gleicher Geschäftsschlüssel bei anderer Datei erzeugt einen Hinweis. Bytes und geschützten Download erhalten.

**Anwendung und Erweiterung**: Neue Formate brauchen sichere begrenzte Parser, Dateiregeltests, Datenvertrag und Negativbeispiele. Keine DTDs aktivieren und MIME-Header nicht als Beweis werten. Externe Archive benötigen eigene Aufbewahrungs- und Integritätsgarantien.

**Backend-Tests**: `ImportModuleTest, DocumentModuleTest, DomainTest, ModuleIntegrationTest, ConcurrencyIntegrationTest`.

**Frontend-Tests**: `dialogs.test.jsx; http.test.mjs; modules.test.mjs; modules.spec.js`.

## 5. Leistungsnachweise und Abnahme

**Quellcode**: `documents/DocumentService.java`; `invoices/InvoiceService.java`.

**API**: `/api/invoices/{id}/evidence, /api/invoices/{id}/evidence-acceptance`.

**Daten**: `document(kind=EVIDENCE), invoice acceptance fields, audit_event`.

Datei und verantwortliche Projektperson beziehungsweise aktive Vertretung verlangen. Ersteller dürfen eigene erforderliche Leistungen nicht abnehmen. Neue Nachweise oder Projektwechsel setzen die Abnahme zurück. Abnahme und Rechnungsfreigabe sind verschiedene Fachentscheidungen, keine qualifizierte Signatur.

**Anwendung und Erweiterung**: Finance hängt einen Bericht an; unabhängige Projektverantwortliche laden ihn herunter, prüfen und nehmen ab. Mengen-, Größen-, Versions- und Rechteprüfungen bei Erweiterungen erhalten.

**Backend-Tests**: `DocumentModuleTest, InvoiceModuleTest, IntegrationTest, ModuleIntegrationTest`.

**Frontend-Tests**: `workspace.test.jsx; dialogs.test.jsx; workflows.spec.js`.

## 6. Rückfragen, Dubletten und Ablehnung

**Quellcode**: `invoices/InvoiceService.java`; `domain/WorkflowPolicy.java`; `domain/DuplicateFingerprint.java`.

**API**: `/api/invoices/{id}/comments, /questions/{question}/resolution, /duplicate-resolution, /commands`.

**Daten**: `discussion, invoice duplicate/readiness/approval fields, audit_event`.

Kommentare lösen Fragen nicht automatisch. Klärung erzeugt eine offene Frage und setzt Unterschriften zurück. Finance dokumentiert die Antwort vor Rückkehr zur Prüfung. Dublettenfreigabe, Ablehnung und Wiederöffnung brauchen Gründe. Kommentare sind Klartext; verbotene Befehle dürfen keine Teiländerungen hinterlassen.

**Anwendung und Erweiterung**: Antwort prüfen und explizit auflösen. Für neue Ausnahmearten Bereitschaft, Unterschriften, Version, Audit und EN-/DE-Fehlertexte gemeinsam festlegen.

**Backend-Tests**: `InvoiceModuleTest, DomainTest, ModuleIntegrationTest`.

**Frontend-Tests**: `workspace.test.jsx; dialogs.test.jsx; modules.test.mjs; modules.spec.js`.

## 7. Freigaberegeln und Vertretung

**Quellcode**: `domain/WorkflowPolicy.java`; `domain/WorkflowFacts.java`; `domain/DelegationPolicy.java`; `identity/IdentityService.java`; `catalog/CatalogService.java`.

**API**: `/api/invoices/{id}/commands, /api/settings/rules, /api/delegations`.

**Daten**: `invoice approval slots and threshold snapshot, delegation, organization`.

Weiterleitung verlangt valide Basisdaten, Zuordnung, geklärte Dublette, erforderliche Abnahme und keine offenen Fragen. Hohe-Betrag-Regel bei Weiterleitung festhalten. Keine Selbstfreigabe. Zugeordnete Person/Vertretung zuerst, bei Bedarf andere Administration danach. Vertretungsbeginn eingeschlossen, Ende ausgeschlossen; Unabhängigkeit bleibt.

**Anwendung und Erweiterung**: Schwellenwerte nur administrativ ändern. Neue Regeln mit unabhängigen Erwartungen, Betrags-/Datumsgrenzen und mehreren Benutzern testen. Die Rollen-/Statusmatrix ersetzt keine detaillierten Faktenkombinationen.

**Backend-Tests**: `DomainTest, PolicyMatrixChecks, InvoiceModuleTest, CatalogModuleTest, IdentityModuleTest, IntegrationTest`.

**Frontend-Tests**: `workspace.test.jsx; catalog-settings.test.jsx; dialogs.test.jsx; workflows.spec.js; modules.spec.js`.

## 8. Buchhaltungsübergabe und Idempotenz

**Quellcode**: `exports/ExportService.java`; `exports/ExportController.java`; `domain/SafeCsv.java`.

**API**: `/api/exports, /api/exports/{id}/download`.

**Daten**: `accounting_export, export_invoice, original/evidence bytes, audit_event`.

Nur Finance/Administration; Freigabefakten unter Sperre erneut prüfen. Maximal 20 verschiedene Rechnungen und begrenzte Quelldatenmenge. IDs/Versionen mit Idempotenzschlüssel verknüpfen. Gleicher Inhalt liefert dasselbe Paket, anderer Inhalt erzeugt Konflikt. CSV-Formelpräfixe neutralisieren; Dokumentbytes erhalten. Keine Buchung, Zahlung oder DATEV-Zustellung.

**Anwendung und Erweiterung**: Paket unter Buchhaltungsübergabe prüfen/herunterladen. Live-Konnektoren brauchen eigene Zustellung, Zugangsdaten, Wiederholungen, Idempotenz und echte Anbietertests. EXPORTED ist keine externe Annahmebestätigung.

**Backend-Tests**: `ExportModuleTest, DomainTest, IntegrationTest, ModuleIntegrationTest, ConcurrencyIntegrationTest`.

**Frontend-Tests**: `accounting-audit.test.jsx; dialogs.test.jsx; modules.test.mjs; workflows.spec.js`.

## 9. Audit-Verlauf

**Quellcode**: `audit/AuditService.java`; `audit/AuditController.java`.

**API**: `/api/audit?before=EVENT_ID; invoice detail audit`.

**Daten**: `audit_event, database immutability trigger`.

Tatsächliche Person und Organisation in derselben Transaktion protokollieren. Cursor-Paginierung statt unbegrenztem Dump. Keine öffentliche Änderungs-/Lösch-API. Trigger schützen vor normalen Änderungen, nicht vor privilegierten Administratoren. Keine Passwörter oder Originalinhalte protokollieren.

**Anwendung und Erweiterung**: Vom Ereignis zur Rechnung navigieren und Verlauf vergleichen. Jede neue Fachänderung mit stabilem Ereigniscode und beiden Übersetzungen ergänzen.

**Backend-Tests**: `AuditModuleTest, ModuleIntegrationTest, InvoiceModuleTest`.

**Frontend-Tests**: `accounting-audit.test.jsx; modules.test.mjs; modules.spec.js`.

## 10. Erinnerungen und Benachrichtigungen

**Quellcode**: `notifications/ReminderService.java`; `notifications/NotificationController.java`; `domain/ReminderPolicy.java`.

**API**: `/api/notifications, /api/notifications/{id}/read, /api/notifications/run`.

**Daten**: `notification, invoice, delegation, active app_user`.

Europe/Berlin-Kalenderdaten, Fälligkeit und aktive berechtigte Empfänger verwenden. Datenbankeindeutigkeit verhindert doppelte Hinweise pro Person/Rechnung/Tag. Nur Empfänger markieren gelesen; nur Administration startet manuell. Hinweise bleiben intern. Anfangsverzögerung ist konfigurierbar und in Tests länger.

**Anwendung und Erweiterung**: Glocke öffnen, Rechnung aufrufen und gezielten Hinweis gelesen markieren. E-Mail ist ein separater Adapter mit echtem Transport, Fehler- und Datenschutzkonzept.

**Backend-Tests**: `NotificationModuleTest, DomainTest, ModuleIntegrationTest`.

**Frontend-Tests**: `catalog-settings.test.jsx; dialogs.test.jsx; modules.test.mjs; modules.spec.js`.

## 11. Initialisierung, SQL-Normalisierung und Konfiguration

**Quellcode**: `bootstrap/BootstrapData.java`; `shared/Sql.java`; `shared/ApiErrors.java`; `BelegFlowApplication.java`; `resources/application.yml`; `resources/db/migration/V1__initial_schema.sql`.

**API**: `Startup, health endpoint, all typed inputs/error responses`.

**Daten**: `Flyway schema; initial organization/admin or explicit synthetic demo`.

Produktionsstart verlangt bewusste sichere Zugangsdaten und erzeugt nicht still Demo-Konten. SQL-Zahlen/-Daten ohne Gleitkomma-Verlust normalisieren. Unbekannte Fehler liefern Referenz statt interner SQL-Traces. Jackson-3-YAML muss binden. Ausgerollte Migrationen unverändert lassen.

**Anwendung und Erweiterung**: Konfiguration mit echtem Context-Test statt nur Textvergleich ändern. Neue Migration für Schemaänderungen; Erststart, Wiederholung und Nicht-Demo separat testen.

**Backend-Tests**: `BootstrapModuleTest, SharedModuleTest, AdapterTest, JacksonConfigurationTest, ModuleIntegrationTest`.

**Frontend-Tests**: `http.test.mjs; shell-app.test.jsx; identity.test.jsx`.

## 12. Frontend-Schichten und Seitencodes

| Datei | Vertrag | Tests |
|---|---|---|
| `frontend/src/api.ts` | Gleicher Origin, CSRF, Fehler und Downloads; kein DemoApi-Ersatz. | `http.test.mjs` |
| `frontend/src/store.ts` | Zustand, Request-Tickets, fixierte Rechnung/Version, Datenleerung bei Kontowechsel und explizite Aktionen. | `core.test.mjs, modules.test.mjs` |
| `frontend/src/format.ts + i18n.ts` | Exakte Centwerte, Sprachschlüssel, Platzhalter und getrenntes Zahlenformat. | `core.test.mjs, modules.test.mjs` |
| `frontend/features/identity/Login.jsx` | Zugangsdaten, Demo-Sichtbarkeit, Sprachwahl, Lade-/Fehlerzustand. | `identity.test.jsx` |
| `frontend/features/invoices/Workspace.jsx` | Liste, Rechnungsblatt, Nachweise, Bereitschaft, Aktionen und Diskussion. | `workspace.test.jsx` |
| `frontend/features/catalog/CatalogPages.jsx` | Lieferanten-/Projekt-/Kostenstellenseiten; Fachwerte nicht automatisch übersetzen. | `catalog-settings.test.jsx` |
| `frontend/features/settings/Settings.jsx` | Administration, Team/Vertretung, Regeln und persönliche Einstellungen. | `catalog-settings.test.jsx` |
| `frontend/features/accounting/Exports.jsx + features/audit/AuditPage.jsx` | Berechtigte Übergabe, Download, Verlauf, Cursor-Seiten und Rechnungslinks. | `accounting-audit.test.jsx` |
| `frontend/components/Dialogs.jsx` | Native Dialoge, Validierungsattribute, Sprache und erfasster Transaktionskontext. | `dialogs.test.jsx` |
| `frontend/App.jsx + components/Shell.jsx + components/ui.jsx` | Aufbau, Routenschutz, Lade-/Fehlerzustand, zugängliche Namen und gemeinsame Elemente. | `shell-app.test.jsx` |
| `frontend/main.jsx + styles.css` | React19-Start, Browser-Abonnement, Tastatur, responsives Layout und Fokus. | `workflows.spec.js, modules.spec.js` |

React-Tests verwenden echte React-DOM-Komponenten mit jsdom und Aktions-Spies. Zustands-/HTTP-Tests prüfen die echten Klassen separat. Browser-Journeys prüfen das Zusammenspiel mit dem echten Backend. Die Offline-Vorschau ist eine zusätzliche, ausdrücklich begrenzte Demonstration.

## 13. Checkliste für Moduländerungen

Fachregel und Rechte schriftlich festlegen; Negativfall zuerst testen; DTO und validierte Eingaben ändern; erforderliche neue Migration anlegen; Versionierung/Transaktion/Audit erhalten; EN/DE und Tastaturbedienung aktualisieren; Unit-, SQL- und Browservertrag passend erweitern; Testmatrix, API-Referenz und Changelog aktualisieren; vollständigen Qualitätslauf und Berichte prüfen.

## Testinfrastruktur: Multipart-Request-Builder (0.2.2)

`ModuleIntegrationTest.pdf(String)` liefert `MockMultipartHttpServletRequestBuilder`. Datumsfelder fehlen absichtlich, damit die Integrationsfälle für fehlende Datumsangaben aussagekräftig bleiben. `.param(...)` und Security-Postprozessoren bleiben ohne Cast verkettbar. `MultipartRequestBuilderTest` verwendet echte Spring-Builder und simuliert nur SQL-Nachschlagewerte. Sechs Fälle prüfen Request-Struktur, exakte Dateibytes und Typsicherheit; sie ersetzen keine Datenbanktests. Siehe [Fehlerdiagnose](TROUBLESHOOTING.md) und [Prüfnachweis](../testing/VERIFICATION.md).

## 13. Responsive Abmeldung und Formularbeschriftung (0.2.2)

**Quellcode:** `frontend/src/components/ui.jsx`, `frontend/src/features/settings/Settings.jsx`, `frontend/src/styles.css`, `frontend/src/store.ts`, `frontend/src/api.ts`.

`Field` akzeptiert optional eine seitenweit eindeutige `id`. Dieser Zweig erwartet genau ein Eingabe-/Select-/Textarea-Element, erzeugt ein separates natives Label und verbindet Hinweise mit `aria-describedby`. Hinweise gehören nicht zum zugänglichen Namen. Bestehende Aufrufer ohne ID bleiben kompatibel. Die Einstellungen verwenden `settings-threshold`, `settings-language` und `settings-number-format`.

Die feste Seitenleiste scrollt vertikal; direkte Kinder werden nicht zusammengeschoben. Bis 1120px steht die Abmeldung in der Kopfzeile zur Verfügung. Bei niedrigen Desktopfenstern entfällt nur der dekorative Hinweis.

`WorkspaceStore.logout()` verwirft verspätete private Antworten, entfernt vertrauliche Daten und blockiert eine konkurrierende Anmeldung bis zum Abschluss. Doppelte Abmeldung wird unterdrückt. `finally` löst die Sperre; der HTTP-Client verwirft den CSRF-Cache auch im Fehlerfall. Ein Netzwerkfehler ist keine Garantie für eine beendete Serversitzung.

**Tests:** Identitäts-/HTTP-Kerntests, explizite Field-/EN-DE-Settings-Komponentenfälle, `responsive-auth.spec.js` und Administration/Einstellungen in `modules.spec.js`. `test-ui-regressions.py` ist ausschließlich für die gekennzeichnete Kompatibilitätsvorschau bestimmt.
