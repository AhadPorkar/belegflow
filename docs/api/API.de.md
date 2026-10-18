# HTTP-API: Integrationsanleitung

Basis `/api`. Gleichurspruengliche Cookie-Sitzung; keine Bearer-Tokens oder frei wählbaren Mandantenheader. Zuerst `/csrf` abrufen, Sitzung erhalten und bei jeder schreibenden Anfrage `X-CSRF-TOKEN` senden, einschliesslich Anmeldung/Abmeldung. Anmeldung ist URL-kodiert mit `username` und `password`, Erfolg HTTP 204. Nach Sitzungswechsel einen neuen CSRF-Token abrufen.

Alle UUIDs werden in der authentifizierten Organisation geprüft. Geldwerte sind Dezimalzeichenfolgen, Daten `YYYY-MM-DD`, Ereignisse ISO-Zeitpunkte. Viele Mutationen benoetigen `version`. Veraltete Aktionen nicht automatisch wiederholen. Void-Mutationen liefern HTTP 200 ohne Body.

## Freigabeaktion

```http
POST /api/invoices/INVOICE_UUID/commands
Content-Type: application/json
X-CSRF-TOKEN: TOKEN
Cookie: JSESSIONID=...

{"version":3,"action":"SUBMIT","note":""}
```

Zuvor das aktuelle Detail lesen. Erlaubte Kommandos sind REVIEW, CLARIFY, RESOLVE, SUBMIT, APPROVE, REJECT und REOPEN. Export entsteht über den Paketendpunkt. CLARIFY/REJECT/REOPEN brauchen eine Begruendung. Keine direkten Statuswerte schreiben und sichtbare Schaltflaechen nicht als Sicherheitsgrenze betrachten.

## Datentransfer

Import: Multipart `file`, optional `supplier_id`, `project_id`, `cost_center_id`, `approver_id`. Bei PDF zusätzlich Lieferant, Rechnungsnummer, Beschreibung, Datum, Fälligkeit, Netto, Steuer, Brutto und EUR. XML-Finanzfelder stammen aus dem Original. Antwort `{id,reused,duplicate_warning}`. Originaldatei maximal 10 MB.

Nachweisupload: Multipart `file` und `version`. Leistungsabnahme und Dublettenentscheidung: `{version,note}`. Zuordnung: `{version,project_id,cost_center_id,approver_id,due_date}`. Kommentare und Fragenaufloesung: `{body}`. Allgemeine Kommentare erhoehen serverseitig die Rechnungsversion nicht, eine Fragenaufloesung dagegen schon.

Export: `{idempotency_key:UUID,invoices:[{id:UUID,version:integer}]}` mit maximal 20 verschiedenen freigegebenen Rechnungen. Bei verlorener Antwort denselben Schlüssel und dieselben Daten wiederverwenden. Gleicher Schlüssel mit anderem Inhalt ergibt 409. Download erfolgt über `/exports/{id}/download`; es wird kein externer Buchhaltungsaufruf ausgeloest.

Originaldownloads erzwingen einen Anhang, liefern nosniff und `X-Content-SHA256`. Dokumentinhalte nicht als vertrauenswuerdiges HTML in eine Seite einbetten. Audit verwendet einen `before`-Cursor und Seiten bis 100. Erinnerungen sind benutzerbezogen und intern, nicht per E-Mail.

Finance/Administration duerfen Lieferanten sowie Projekte/Kostenstellen bearbeiten/anlegen. Regeln, Benutzer und Vertretungen erfordern Administration. Ein leeres Passwort bei Benutzerbearbeitung behält das alte; ein neues muss 14-128 Zeichen haben. Die initiale Einrichtung hat einen eigenen Mindestwert. E-Mail-Adressen sind global eindeutig; die aktive Administration kann sich nicht selbst deaktivieren oder herabstufen.

## Fehler

400: Eingabeformat. 401: Sitzung fehlt oder widerrufen. 403: Rechte/CSRF/Unabhängigkeit. 404: Im aktuellen Mandanten nicht gefunden. 409: Version/Idempotenz/Integritaet. 413: Dateigröße. 422: Fachliche Voraussetzung oder Validierung. Fehler enthalten `code`, unerwartete Fehler zusätzlich `reference`. Beispiele: EVIDENCE_REQUIRED, OPEN_QUESTIONS, ALLOCATION_REQUIRED, DUPLICATE_UNRESOLVED, VALIDATION_FAILED.

Die vollständige sprachneutrale Endpoint- und Request-Record-Liste steht im [englischen API-Referenzdokument](API.md). Identifikatoren werden nicht übersetzt. Controller und Request-Records sind für diese Version massgeblich; ein erfundener DATEV-Endpunkt oder eine Konformitaetsgarantie existiert nicht.
