# BelegFlow Benutzerhandbuch

Version 0.2.0 | [English](../en/USER_GUIDE.md)

## 1. Drei getrennte Entscheidungen

Die Basisprüfung untersucht, ob unterstuetzte Rechnungsfelder gelesen werden können und ob die vorgesehenen Summen- und Datumsprüfungen bestehen. Sie zertifiziert keine Rechnung nach einem deutschen E-Rechnungsstandard. Die fachliche Freigabe beantwortet, ob eine unabhängige verantwortliche Person Aufwand, Zuordnung und Leistungsnachweise akzeptiert. Das Buchhaltungspaket übergibt freigegebene Informationen an einen getrennten Buchhaltungsprozess. Exportiert bedeutet weder gebucht noch bezahlt.

Der normale Ablauf lautet `Eingegangen -> Geprüft -> Wartet auf Freigabe -> Freigegeben -> Exportiert`. Offene Rückfragen führen in die Klärung. Nach dokumentierter Beantwortung kehrt der Vorgang zur Prüfung zurück. Eine Ablehnung braucht eine Begründung. Finance kann eine abgelehnte Rechnung begründet wieder öffnen; vorherige Freigaben gelten dann nicht unverändert weiter.

## 2. Anmeldung und Einstellungen

Das von der Administration bereitgestellte Konto verwenden. Gross-/Kleinschreibung im Passwort beachten. Sitzungen laufen nach 30 Minuten Inaktivitaet ab. Konto- und Berechtigungswechsel können eine vorhandene Sitzung durch eine Authentifizierungsversion ungültig machen. Danach erneut anmelden, statt ein altes Formular wiederholt abzusenden.

Die erste Oberflaechensprache ist Englisch, auch bei deutscher Browsersprache. Im Kopfbereich EN oder DE wählen. Die Auswahl angemeldeter Benutzer wird gespeichert. In den Einstellungen kann das Zahlen-/Datumsformat unabhängig davon zwischen englischer und deutscher Darstellung wechseln. Eine Rechnung bleibt in EUR; Formatwechsel sind keine Währungsumrechnung. Zeitpunkte werden für Europe/Berlin dargestellt. Helles/dunkles Design wird lokal im Browser gespeichert.

Rollenwechsel und Ein-Klick-Anmeldung sind ausschliesslich Demo-Funktionen. Im Produktivmodus erscheinen diese Hilfen nicht.

## 3. Rollen und Sichtbarkeit

| Rolle | Verantwortung |
|---|---|
| Finance | Importieren, prüfen, zuordnen, Klärung dokumentieren, Freigabe anfordern und exportieren; Stammdaten verwalten. |
| Freigebende Person | Zugeordnete Vorgaenge bearbeiten, verantwortete Leistungen abnehmen und Rechnungen unabhängig freigeben oder ablehnen. |
| Administration | Finance-Funktionen, Benutzer, Regeln, Vertretungen und unabhängige zweite Freigabe. |
| Lesender Zugriff | Daten der Organisation lesen, aber keine Entscheidungen oder Exporte erzeugen. |

Administration umgeht nicht automatisch die erste Zuordnung oder das Verbot der Selbstfreigabe. Der Ersteller darf seine eigene Rechnung nicht freigeben. Die erste Unterschrift stammt von der zugeordneten Person oder einer aktuell gültigen Vertretung. Die zweite muss von einer anderen administrativen Person stammen, die ebenfalls nicht der Ersteller ist.

Alle aktiven Organisationsmitglieder dürfen grundsätzlich die Rechnungen ihrer Organisation und deren Dokumente lesen. Eine Projektzuordnung beschränkt Aktionen, nicht die Sichtbarkeit. Organisationen sind voneinander getrennt; eine gemeinsame Benutzeridentität mit mehreren Organisationsmitgliedschaften ist in dieser Version nicht vorgesehen.

## 4. Arbeitsbereich

Links befinden sich Suche, Statusfilter und die seitenweise Rechnungsliste. In der Mitte stehen strukturierte Rechnungsdaten oder Leistungsnachweise. Rechts erscheinen Bereitschaft, Entscheidungen, Verlauf und Diskussionen. Die dargestellte Rechnung ist aus Feldern erzeugt und keine Original-PDF-/XML-Vorschau. Zum Originaldownload die entsprechende Schaltfläche verwenden.

Auf kleinen Displays zwischen Liste, Dokument und Entscheidung wechseln. Die Navigation ist auch über die Befehlssuche erreichbar; am Computer mit Strg/Cmd+K. Deaktivierte Weiterleitung bedeutet, dass mindestens eine angezeigte Voraussetzung noch nicht erfüllt ist.

## 5. Rechnung importieren

Rechnung importieren wählen und genau eine Datei hochladen. Originale sind auf 10 MB begrenzt. Unterstützt werden passende XML-Dokumente und Dateien mit PDF-Signatur. Eine vollständige PDF-Analyse oder Schadsoftwareprüfung ist nicht eingebaut.

XML: begrenzte Extraktion von UBL Invoice und CII mit Lieferantenname, Rechnungsnummer, Datum, Fälligkeit, Währung, Beschreibung und Summen. Basisprüfungen betreffen unterstuetzte Summen, EUR, Pflichtfelder und Datumsreihenfolge. Das ist kein allgemeiner XML-Konverter und kein offizieller Schematron-Validator. Die mitgelieferten XML-Dateien sind Extraktionsbeispiele, keine zertifizierten XRechnungen. Gutschriften, Teilzahlungen, Vorauszahlungen und eingebettete ZUGFeRD-Extraktion sind nicht abgedeckt.

PDF: Lieferant wählen und Rechnungsnummer, Beschreibung, Daten, Netto, Steuer und Brutto manuell eintragen. OCR ist nicht enthalten. Die API verwendet Dezimalzeichenfolgen mit Punkt; Geldwerte werden nicht mit binären Gleitkommazahlen berechnet. Positionserfassung, steuerliche Kontierung und USt-ID-Verifikation fehlen bewusst.

Projekt, Kostenstelle und freigebende Person können beim Import oder später zugeordnet werden. Ein XML-Lieferantenname kann einem aktiven Datensatz zugeordnet werden oder einen neuen erzeugen. Die Zuordnung fachlich prüfen, besonders bei ähnlichen Firmennamen.

Gleiche Originalbytes in derselben Organisation öffnen die vorhandene Rechnung wieder. Eine andere Datei mit gleichem normalisiertem Lieferanten-/Rechnungsnummernschlüssel erzeugt einen Dublettenhinweis.

## 6. Prüfung und Zuordnung

Als Finance eine eingegangene Rechnung anhand des Originals prüfen und auf Geprüft setzen. Über Zuordnung bearbeiten Projekt, Kostenstelle, Freigabe und Fälligkeit festlegen. Das Projekt bestimmt, ob ein Leistungsnachweis erforderlich ist und wer ihn abnehmen darf. Fehlende Zuordnungen oder fehlgeschlagene Basisprüfungen verhindern die Weiterleitung.

Ein Projektwechsel im bearbeitbaren Zustand setzt die Leistungsabnahme zurück. Auch eine neue Nachweisdatei erfordert eine erneute Abnahme des jetzt vorhandenen Nachweisbestands. Nach der Weiterleitung/Freigabe sind normale Metadatenänderungen gesperrt. Das Original wird durch eine Zuordnung nicht umgeschrieben.

Bei falschen Summen oder nicht unterstuetzter Rechnungssemantik ein korrigiertes Original anfordern oder den Fall außerhalb der Anwendung klären. Es gibt keinen beliebigen Schalter zum Erzwingen einer gültigen Rechnung und keinen stillen Finanzdateneditor für eingegangene XML-Dateien.

## 7. Leistungsnachweise

Vorhandene Nachweise herunterladen und einen Servicebericht, ein Übergabeprotokoll oder einen anderen Leistungsbeleg anhängen. Erlaubt sind PDF, PNG, JPEG und begrenzter Klartext. Pro Rechnung maximal 20 Nachweise; pro Datei maximal 10 MB, bei Text 128 KB. Keine unnötigen personenbezogenen Informationen anhängen.

Der verantwortliche Projektmanager oder eine gültige Vertretung prüft den Inhalt und akzeptiert die Leistung mit einer Notiz. Der Ersteller darf seinen eigenen erforderlichen Nachweis nicht abnehmen. Eine Notiz ohne Datei reicht nicht. Die Abnahme ist eine fachliche Bestätigung, keine qualifizierte elektronische Signatur und keine gesetzliche Prüfbescheinigung.

Finance darf erst weiterleiten, wenn auch Basisprüfung, Zuordnung, Dublettenprüfung und Rückfragen passen. Leistungsabnahme und Rechnungsfreigabe bleiben getrennt.

## 8. Dubletten und Klärung

Eine mögliche Dublette mit Original und vorhandenen Einträgen vergleichen. Eine korrigierte Datei ist nicht automatisch ein weiterer zahlbarer Aufwand. Finance darf den Hinweis nur mit einer nachvollziehbaren Begründung ausräumen. Das löscht keine Dokumente und verhindert nicht automatisch eine Doppelzahlung in einem externen System.

Eine Rückfrage setzt einen bearbeitbaren oder wartenden Vorgang in Klärung und erzeugt eine offene Frage. Kommentare liefern Kontext, beantworten die Frage aber nicht automatisch. Finance dokumentiert jede Auflösung und beendet danach die Klärung. Solange Fragen offen sind, bleibt die Weiterleitung gesperrt. Bereits vorhandene Freigaben werden beim Wechsel in Klärung zurückgesetzt.

Eine Ablehnung braucht eine Begründung. Die Rechnung bleibt sichtbar. Finance darf sie begründet wieder öffnen und erneut prüfen lassen. Export ist kein Umweg um diese Regeln.

## 9. Eine oder zwei Freigaben

Bei der Weiterleitung wird festgehalten, ob nach dem aktuellen Bruttoschwellenwert eine zweite Freigabe erforderlich ist. Der Standard liegt bei EUR 10.000 einschließlich des Grenzbetrags. Spätere Regelwechsel entfernen diese Anforderung bei bereits weitergeleiteten Rechnungen nicht heimlich.

Unterhalb des Schwellenwerts entscheidet die zugeordnete unabhängige Person. Bei hohem Wert bleibt die Rechnung nach der ersten Freigabe wartend, bis eine andere administrative Person die zweite erteilt. Ersteller und erste freigebende Person dürfen nicht die zweite Freigabe übernehmen. Auch administrative Rechte erlauben keine Selbstfreigabe.

Eine veraltete Bearbeitung erzeugt einen Versionskonflikt. Aktualisieren, Verlauf ansehen und erst dann neu entscheiden. Keine veralteten Aktionen blind wiederholen.

## 10. Vertretung und Erinnerungen

Die Administration bestimmt vertretene Person, Vertretung sowie Start- und Endzeitpunkt. Der Start ist eingeschlossen, das Ende ausgeschlossen. Maßgeblich ist die Gültigkeit zum Zeitpunkt der Aktion. Im Verlauf steht die tatsächlich handelnde Person. Selbstfreigabe- und Unabhängigkeitsregeln bleiben erhalten.

Erinnerungen betreffen wartende Rechnungen, deren Fälligkeit bis einschließlich heute plus drei Kalendertage liegt; überfällige Rechnungen sind enthalten. Die Datumsbasis ist Europe/Berlin, keine feiertagsabhängige Arbeitstagsberechnung. Standardmäßig läuft der Job alle 15 Minuten. Pro Benutzer/Rechnung/Tag werden doppelte Hinweise verhindert. Die Hinweise erscheinen an der Glocke. Es werden keine E-Mails oder SMS versandt. Administration kann einen Lauf manuell anstoßen.

## 11. Buchhaltungspaket

Finance oder Administration exportiert freigegebene Rechnungen einzeln oder in Gruppen bis 20, bei maximal 50 MB Quelldokumenten. Bei größeren Dateien kleinere Pakete bilden. ZIP, SHA-256 und Rechnungszuordnung werden gespeichert. Eine Wiederholung mit gleichem Idempotenzschlüssel und gleichem Inhalt liefert das vorhandene Paket.

Das Serverpaket enthält `invoices.csv`, `audit.csv`, `manifest.csv`, englische/deutsche Hinweise, `originals/` und `evidence/`. CSV verwendet UTF-8 mit BOM, Semikolon, ISO-Daten und Dezimalpunkte. Felder sind zitiert; gefährliche Tabellenformelpräfixe werden neutralisiert. Das Manifest enthält Hashes der Daten- und Dokumentdateien, nicht seinen eigenen Hash. Originale bleiben bytegetreu. Der erneute Download eines vorhandenen Pakets erzeugt keine neue fachliche Freigabe.

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
| Sitzung widerrufen | Neu anmelden; Konto oder Rechte könnten geändert worden sein. |
| Keine Erinnerungs-E-Mail | Diese Version erinnert nur innerhalb der Anwendung. |
| Unbekannter Fehler | Referenznummer und Zeitpunkt an die Administration geben, nicht vertrauliche Inhalte. |

Verdächtigen Zugriff auf fremde Organisationen sofort melden. Echte Rechnungen gehören weder in die öffentliche Portfolio-Demo noch in die Offline-Vorschau.


## 13. Lieferanten, Projekte und Kostenstellen pflegen

Als Finance oder Administration Lieferanten öffnen und Lieferant hinzufügen wählen. Erkennbaren Namen, gegebenenfalls Rechnungs-E-Mail/USt-Referenz und Aktivstatus eingeben. Speichern, suchen und über Bearbeiten korrigieren. Alte Lieferanten deaktivieren; vorhandene Rechnungen werden dadurch nicht gelöscht. Steuerreferenzen werden gespeichert, nicht behördlich validiert.

Unter Projekte ein Projekt mit eindeutigem Code/Namen und geeigneter Projektverantwortung anlegen. Erforderlichen Leistungsnachweis aktiviert lassen, wenn die Leistung vor Freigabe abgenommen werden muss. Kostenstelle mit Code und Namen hinzufügen. Danach zur Rechnung zurückkehren und Zuordnung bearbeiten. Anlegen ist implementiert; allgemeine Massenpflege, Löschung und ERP-Synchronisation nicht.

## 14. Administration und persönliche Einstellungen

Nur Administration darf Mitglieder anlegen/bearbeiten. Unter Arbeitsbereichseinstellungen Mitglied hinzufügen wählen; Name, E-Mail, Rolle und anfängliches Passwort mit 14-128 Zeichen eintragen. Zugangsdaten sicher weitergeben, nicht über öffentliche Issues. Ein leeres Passwort bei Bearbeitung erhält das bisherige. Rollen-/Aktivstatuswechsel widerrufen laufende Sitzungen. Selbstdeaktivierung/-herabstufung ist gesperrt. Passwort-Reset-E-Mail und Selbstregistrierung fehlen bewusst.

Bruttoschwellenwert bearbeiten und speichern. Er gilt für neue Weiterleitungen, nicht für stilles Umschreiben historischer Entscheidungen. Vertretung mit zwei geeigneten Mitgliedern und begrenztem Zeitraum anlegen. Widerruf beendet künftige delegierte Aktionen; vergangene Akteure bleiben im Verlauf.

Oberflächensprache und Zahlen-/Datumsformat getrennt einstellen. Deutsches Zahlenformat schaltet die Oberfläche nicht um; Sprachwechsel ändert keine Beträge. Hell/Dunkel gilt für den lokalen Browser.

## 15. Hinweise, Audit und Support

Glocke öffnen, Hinweis zur Rechnung verfolgen, Zustand prüfen und gezielt gelesen markieren. Leserecht an einer Rechnung verleiht kein Recht auf persönliche Hinweise anderer Mitglieder. Administration kann Erinnerungen manuell prüfen; ein erneuter Lauf erzeugt nicht absichtlich denselben Tageshinweis erneut.

Im Audit-Verlauf Ereignisse ansehen und Rechnungslinks folgen. Bei Bedarf ältere Einträge laden. Kommentare liefern Kontext, Audit-Ereignisse dokumentieren konkrete Operationen. Fehler nicht durch Verbergen historischer Einträge beheben. Supportanfrage mit Version, ungefährem Zeitpunkt, Aktion und Referenznummer stellen. Keine echten Rechnungen, Passwörter oder Cookies auf GitHub hochladen.

## 16. Vollständige Abnahmeübung

In der isolierten Demo die geprüfte `KE-2026-1048` wählen; es existiert auch eine getrennte Dublette. Finance sieht die fehlende Leistungsabnahme. Als Leon Nachweis öffnen, herunterladen und begründet abnehmen. Als Finance weiterleiten, als Leon unabhängig freigeben und als Finance Buchhaltungspaket erzeugen. Originale, CSV und Manifest prüfen. Nach jedem Kontowechsel Person und Rechnungsnummer kontrollieren. Diese manuelle Übung ersetzt nicht die automatisierten Tests.

`HIT-26041` besitzt im Server-Testbestand bereits eine erste Freigabe. Eine andere administrative Person kann die zweite erteilen. Ein Viewer darf durch URL-Wechsel oder wiederholte API-Requests keine Schreibrechte erhalten. Solche Negativprüfungen nur in der wegwerfbaren Testinstanz ausführen.

Für Build-/Startprobleme [Fehlerdiagnose](TROUBLESHOOTING.md), für automatisierte Prüfung [Tests](TESTING.md) lesen.
