# BelegFlow Benutzerhandbuch

Version 0.1.0 | [English](../en/USER_GUIDE.md)

## 1. Drei getrennte Entscheidungen

Die Basisprüfung untersucht, ob unterstuetzte Rechnungsfelder gelesen werden können und ob die vorgesehenen Summen- und Datumsprüfungen bestehen. Sie zertifiziert keine Rechnung nach einem deutschen E-Rechnungsstandard. Die fachliche Freigabe beantwortet, ob eine unabhängige verantwortliche Person Aufwand, Zuordnung und Leistungsnachweise akzeptiert. Das Buchhaltungspaket übergibt freigegebene Informationen an einen getrennten Buchhaltungsprozess. Exportiert bedeutet weder gebucht noch bezahlt.

Der normale Ablauf lautet `Eingegangen -> Geprüft -> Wartet auf Freigabe -> Freigegeben -> Exportiert`. Offene Rückfragen führen in die Klaerung. Nach dokumentierter Beantwortung kehrt der Vorgang zur Prüfung zurück. Eine Ablehnung braucht eine Begruendung. Finance kann eine abgelehnte Rechnung begruendet wieder oeffnen; vorherige Freigaben gelten dann nicht unverändert weiter.

## 2. Anmeldung und Einstellungen

Das von der Administration bereitgestellte Konto verwenden. Gross-/Kleinschreibung im Passwort beachten. Sitzungen laufen nach 30 Minuten Inaktivitaet ab. Konto- und Berechtigungswechsel können eine vorhandene Sitzung durch eine Authentifizierungsversion ungültig machen. Danach erneut anmelden, statt ein altes Formular wiederholt abzusenden.

Die erste Oberflaechensprache ist Englisch, auch bei deutscher Browsersprache. Im Kopfbereich EN oder DE wählen. Die Auswahl angemeldeter Benutzer wird gespeichert. In den Einstellungen kann das Zahlen-/Datumsformat unabhängig davon zwischen englischer und deutscher Darstellung wechseln. Eine Rechnung bleibt in EUR; Formatwechsel sind keine Währungsumrechnung. Zeitpunkte werden für Europe/Berlin dargestellt. Helles/dunkles Design wird lokal im Browser gespeichert.

Rollenwechsel und Ein-Klick-Anmeldung sind ausschliesslich Demo-Funktionen. Im Produktivmodus erscheinen diese Hilfen nicht.

## 3. Rollen und Sichtbarkeit

| Rolle | Verantwortung |
|---|---|
| Finance | Importieren, prüfen, zuordnen, Klaerung dokumentieren, Freigabe anfordern und exportieren; Stammdaten verwalten. |
| Freigebende Person | Zugeordnete Vorgaenge bearbeiten, verantwortete Leistungen abnehmen und Rechnungen unabhängig freigeben oder ablehnen. |
| Administration | Finance-Funktionen, Benutzer, Regeln, Vertretungen und unabhängige zweite Freigabe. |
| Lesender Zugriff | Daten der Organisation lesen, aber keine Entscheidungen oder Exporte erzeugen. |

Administration umgeht nicht automatisch die erste Zuordnung oder das Verbot der Selbstfreigabe. Der Ersteller darf seine eigene Rechnung nicht freigeben. Die erste Unterschrift stammt von der zugeordneten Person oder einer aktuell gültigen Vertretung. Die zweite muss von einer anderen administrativen Person stammen, die ebenfalls nicht der Ersteller ist.

Alle aktiven Organisationsmitglieder duerfen grundsaetzlich die Rechnungen ihrer Organisation und deren Dokumente lesen. Eine Projektzuordnung beschraenkt Aktionen, nicht die Sichtbarkeit. Organisationen sind voneinander getrennt; eine gemeinsame Benutzeridentitaet mit mehreren Organisationsmitgliedschaften ist in dieser Version nicht vorgesehen.

## 4. Arbeitsbereich

Links befinden sich Suche, Statusfilter und die seitenweise Rechnungsliste. In der Mitte stehen strukturierte Rechnungsdaten oder Leistungsnachweise. Rechts erscheinen Bereitschaft, Entscheidungen, Verlauf und Diskussionen. Die dargestellte Rechnung ist aus Feldern erzeugt und keine Original-PDF-/XML-Vorschau. Zum Originaldownload die entsprechende Schaltflaeche verwenden.

Auf kleinen Displays zwischen Liste, Dokument und Entscheidung wechseln. Die Navigation ist auch über die Befehlssuche erreichbar; am Computer mit Strg/Cmd+K. Deaktivierte Weiterleitung bedeutet, dass mindestens eine angezeigte Voraussetzung noch nicht erfuellt ist.

## 5. Rechnung importieren

Rechnung importieren wählen und genau eine Datei hochladen. Originale sind auf 10 MB begrenzt. Unterstuetzt werden passende XML-Dokumente und Dateien mit PDF-Signatur. Eine vollständige PDF-Analyse oder Schadsoftwareprüfung ist nicht eingebaut.

XML: begrenzte Extraktion von UBL Invoice und CII mit Lieferantenname, Rechnungsnummer, Datum, Fälligkeit, Währung, Beschreibung und Summen. Basisprüfungen betreffen unterstuetzte Summen, EUR, Pflichtfelder und Datumsreihenfolge. Das ist kein allgemeiner XML-Konverter und kein offizieller Schematron-Validator. Die mitgelieferten XML-Dateien sind Extraktionsbeispiele, keine zertifizierten XRechnungen. Gutschriften, Teilzahlungen, Vorauszahlungen und eingebettete ZUGFeRD-Extraktion sind nicht abgedeckt.

PDF: Lieferant wählen und Rechnungsnummer, Beschreibung, Daten, Netto, Steuer und Brutto manuell eintragen. OCR ist nicht enthalten. Die API verwendet Dezimalzeichenfolgen mit Punkt; Geldwerte werden nicht mit binaeren Gleitkommazahlen berechnet. Positionserfassung, steuerliche Kontierung und USt-ID-Verifikation fehlen bewusst.

Projekt, Kostenstelle und freigebende Person können beim Import oder spaeter zugeordnet werden. Ein XML-Lieferantenname kann einem aktiven Datensatz zugeordnet werden oder einen neuen erzeugen. Die Zuordnung fachlich prüfen, besonders bei aehnlichen Firmennamen.

Gleiche Originalbytes in derselben Organisation oeffnen die vorhandene Rechnung wieder. Eine andere Datei mit gleichem normalisiertem Lieferanten-/Rechnungsnummernschlüssel erzeugt einen Dublettenhinweis.

## 6. Prüfung und Zuordnung

Als Finance eine eingegangene Rechnung anhand des Originals prüfen und auf Geprüft setzen. Über Zuordnung bearbeiten Projekt, Kostenstelle, Freigabe und Fälligkeit festlegen. Das Projekt bestimmt, ob ein Leistungsnachweis erforderlich ist und wer ihn abnehmen darf. Fehlende Zuordnungen oder fehlgeschlagene Basisprüfungen verhindern die Weiterleitung.

Ein Projektwechsel im bearbeitbaren Zustand setzt die Leistungsabnahme zurück. Auch eine neue Nachweisdatei erfordert eine erneute Abnahme des jetzt vorhandenen Nachweisbestands. Nach der Weiterleitung/Freigabe sind normale Metadatenänderungen gesperrt. Das Original wird durch eine Zuordnung nicht umgeschrieben.

Bei falschen Summen oder nicht unterstuetzter Rechnungssemantik ein korrigiertes Original anfordern oder den Fall ausserhalb der Anwendung klaeren. Es gibt keinen beliebigen Schalter zum Erzwingen einer gültigen Rechnung und keinen stillen Finanzdateneditor für eingegangene XML-Dateien.

## 7. Leistungsnachweise

Vorhandene Nachweise herunterladen und einen Servicebericht, ein Übergabeprotokoll oder einen anderen Leistungsbeleg anhängen. Erlaubt sind PDF, PNG, JPEG und begrenzter Klartext. Pro Rechnung maximal 20 Nachweise; pro Datei maximal 10 MB, bei Text 128 KB. Keine unnoetigen personenbezogenen Informationen anhängen.

Der verantwortliche Projektmanager oder eine gültige Vertretung prüft den Inhalt und akzeptiert die Leistung mit einer Notiz. Der Ersteller darf seinen eigenen erforderlichen Nachweis nicht abnehmen. Eine Notiz ohne Datei reicht nicht. Die Abnahme ist eine fachliche Bestätigung, keine qualifizierte elektronische Signatur und keine gesetzliche Prüfbescheinigung.

Finance darf erst weiterleiten, wenn auch Basisprüfung, Zuordnung, Dublettenprüfung und Rückfragen passen. Leistungsabnahme und Rechnungsfreigabe bleiben getrennt.

## 8. Dubletten und Klaerung

Eine mögliche Dublette mit Original und vorhandenen Eintraegen vergleichen. Eine korrigierte Datei ist nicht automatisch ein weiterer zahlbarer Aufwand. Finance darf den Hinweis nur mit einer nachvollziehbaren Begruendung ausraeumen. Das löscht keine Dokumente und verhindert nicht automatisch eine Doppelzahlung in einem externen System.

Eine Rückfrage setzt einen bearbeitbaren oder wartenden Vorgang in Klaerung und erzeugt eine offene Frage. Kommentare liefern Kontext, beantworten die Frage aber nicht automatisch. Finance dokumentiert jede Aufloesung und beendet danach die Klaerung. Solange Fragen offen sind, bleibt die Weiterleitung gesperrt. Bereits vorhandene Freigaben werden beim Wechsel in Klaerung zurückgesetzt.

Eine Ablehnung braucht eine Begruendung. Die Rechnung bleibt sichtbar. Finance darf sie begruendet wieder oeffnen und erneut prüfen lassen. Export ist kein Umweg um diese Regeln.

## 9. Eine oder zwei Freigaben

Bei der Weiterleitung wird festgehalten, ob nach dem aktuellen Bruttoschwellenwert eine zweite Freigabe erforderlich ist. Der Standard liegt bei EUR 10.000 einschliesslich des Grenzbetrags. Spaetere Regelwechsel entfernen diese Anforderung bei bereits weitergeleiteten Rechnungen nicht heimlich.

Unterhalb des Schwellenwerts entscheidet die zugeordnete unabhängige Person. Bei hohem Wert bleibt die Rechnung nach der ersten Freigabe wartend, bis eine andere administrative Person die zweite erteilt. Ersteller und erste freigebende Person duerfen nicht die zweite Freigabe übernehmen. Auch administrative Rechte erlauben keine Selbstfreigabe.

Eine veraltete Bearbeitung erzeugt einen Versionskonflikt. Aktualisieren, Verlauf ansehen und erst dann neu entscheiden. Keine veralteten Aktionen blind wiederholen.

## 10. Vertretung und Erinnerungen

Die Administration bestimmt vertretene Person, Vertretung sowie Start- und Endzeitpunkt. Der Start ist eingeschlossen, das Ende ausgeschlossen. Massgeblich ist die Gültigkeit zum Zeitpunkt der Aktion. Im Verlauf steht die tatsächlich handelnde Person. Selbstfreigabe- und Unabhängigkeitsregeln bleiben erhalten.

Erinnerungen betreffen wartende Rechnungen, deren Fälligkeit bis einschliesslich heute plus drei Kalendertage liegt; überfällige Rechnungen sind enthalten. Die Datumsbasis ist Europe/Berlin, keine feiertagsabhängige Arbeitstagsberechnung. Standardmaessig läuft der Job alle 15 Minuten. Pro Benutzer/Rechnung/Tag werden doppelte Hinweise verhindert. Die Hinweise erscheinen an der Glocke. Es werden keine E-Mails oder SMS versandt. Administration kann einen Lauf manuell anstossen.

## 11. Buchhaltungspaket

Finance oder Administration exportiert freigegebene Rechnungen einzeln oder in Gruppen bis 20, bei maximal 50 MB Quelldokumenten. Bei größen Dateien kleinere Pakete bilden. ZIP, SHA-256 und Rechnungszuordnung werden gespeichert. Eine Wiederholung mit gleichem Idempotenzschlüssel und gleichem Inhalt liefert das vorhandene Paket.

Das Serverpaket enthält `invoices.csv`, `audit.csv`, `manifest.csv`, englische/deutsche Hinweise, `originals/` und `evidence/`. CSV verwendet UTF-8 mit BOM, Semikolon, ISO-Daten und Dezimalpunkte. Felder sind zitiert; gefaehrliche Tabellenformelpraefixe werden neutralisiert. Das Manifest enthält Hashes der Daten- und Dokumentdateien, nicht seinen eigenen Hash. Originale bleiben bytegetreu. Der erneute Download eines vorhandenen Pakets erzeugt keine neue fachliche Freigabe.

Das Format ist generisch und **kein DATEV-Importformat**. Feldzuordnung, Konten, Steuerbehandlung und Importkontrollen mit der Buchhaltung abstimmen. Exportiert bedeutet nur, dass das Paket erstellt wurde. Empfang, Buchung oder Zahlung in einem anderen System sind damit nicht bestätigt.

## 12. Fehlerhilfe

| Problem | Vorgehen |
|---|---|
| Weiterleitung gesperrt | Alle Bereitschaftspunkte prüfen: Daten, Zuordnung, Dublette, Abnahme, offene Fragen. |
| Abnahme nicht erlaubt | Projektverantwortung/Vertretung, Erstellerverbot, Zustand und vorhandene Datei prüfen. |
| Erste Freigabe reicht nicht | Zweite, unabhängige administrative Freigabe prüfen. |
| Versionskonflikt | Aktualisieren und den neuen Verlauf fachlich bewerten. |
| XML ungültig | Unterstuetzte Dokumentfamilie, sichere XML-Struktur und Pflichtfelder prüfen. |
| Import wiederverwendet | Das bytegleiche Original liegt bereits vor. |
| Sitzung widerrufen | Neu anmelden; Konto oder Rechte koennten geändert worden sein. |
| Keine Erinnerungs-E-Mail | Diese Version erinnert nur innerhalb der Anwendung. |
| Unbekannter Fehler | Referenznummer und Zeitpunkt an die Administration geben, nicht vertrauliche Inhalte. |

Verdaechtigen Zugriff auf fremde Organisationen sofort melden. Echte Rechnungen gehoeren weder in die öffentliche Portfolio-Demo noch in die Offline-Vorschau.
