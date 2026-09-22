# GBUXML

Ein offenes Austauschformat für Gefährdungsbeurteilungen

Gefährdungsbeurteilungen bilden seit Einführung der Betriebssicherheitsverordnung vor mehr als 20 Jahren eine wesentliche Grundlage für sicheres Arbeiten. Überall dort, wo bei einer Tätigkeit Gefährdungen auftreten können, müssen diese systematisch ermittelt, bewertet und geeignete Schutzmaßnahmen festgelegt werden. In der Praxis betrifft dies einen großen Teil der betrieblichen Tätigkeiten.

Gefährdungsbeurteilungen sind dabei weit mehr als Dokumente zur Erfüllung formaler Anforderungen. Ihr eigentlicher Nutzen entsteht durch Information und Informationsaustausch: Gefährdungen, Risiken, Schutzmaßnahmen, Verantwortlichkeiten und der Stand ihrer Umsetzung müssen den beteiligten Personen zugänglich sein. Eine Gefährdungsbeurteilung, die nach ihrer Erstellung lediglich als isolierte Datei oder ausgedrucktes Dokument abgelegt wird, kann dieses Potenzial nur eingeschränkt erfüllen.

Trotz der großen Bedeutung von Gefährdungsbeurteilungen existiert bislang kein allgemein nutzbares, offenes und herstellerunabhängiges Datenformat, mit dem die darin enthaltenen Informationen strukturiert zwischen unterschiedlichen Anwendungen und Organisationen ausgetauscht werden können.

In der Praxis existieren zahlreiche Vorlagen, Excel-Dateien, PDF-Dokumente, proprietäre Anwendungen und branchenspezifische Lösungen. Auch Berufsgenossenschaften und andere Institutionen stellen Hilfsmittel zur Erstellung von Gefährdungsbeurteilungen bereit. Die dabei verwendeten Datenstrukturen sind jedoch häufig auf die jeweilige Anwendung zugeschnitten und nicht als offener Standard für den maschinenlesbaren Datenaustausch konzipiert.

Genau hier setzt GBUXML an.

## Die Idee

GBUXML soll ein offenes, dokumentiertes und herstellerunabhängiges XML-Format für Gefährdungsbeurteilungen schaffen.

Ziel ist nicht, eine bestimmte Methode zur Durchführung einer Gefährdungsbeurteilung vorzuschreiben. Stattdessen soll eine gemeinsame technische Grundlage geschaffen werden, mit der die wesentlichen Informationen einer Gefährdungsbeurteilung strukturiert gespeichert und zwischen unterschiedlichen Systemen ausgetauscht werden können.

Die grundlegende Struktur folgt dabei einer nachvollziehbaren Hierarchie:

Gewerk → Bereich → Prozess/Tätigkeit → Gefährdung → Maßnahme

Zusätzlich können allgemeine Informationen zur Gefährdungsbeurteilung wie Unternehmen, Ersteller, Erstellungsdatum, Gültigkeitsbereich, Fachkraft für Arbeitssicherheit und Betriebsarzt hinterlegt werden.

Gefährdungen können mit einem Risiko versehen werden. Zu jeder Gefährdung können beliebig viele Maßnahmen einschließlich Handlungsbedarf, Soll-Termin, Verantwortlichem, Ist-Termin und Bestätigung der Umsetzung gespeichert werden.

Das Format wird durch ein XML Schema (XSD) formal beschrieben. Dadurch können GBUXML-Dateien unabhängig von der verwendeten Software automatisiert auf ihre strukturelle Gültigkeit geprüft werden.

## Warum ein offenes Format?

Ein einheitliches Austauschformat eröffnet Möglichkeiten, die mit isolierten Dokumenten nur schwer realisierbar sind.

Eine Gefährdungsbeurteilung könnte beispielsweise von einem Unternehmen erstellt, an einen Auftragnehmer übergeben, dort in einer anderen Software geöffnet und anschließend ergänzt werden. Informationen könnten automatisiert ausgewertet, archiviert oder in andere Arbeitsschutzsysteme übernommen werden.

Langfristig könnten unterschiedliche Programme GBUXML importieren und exportieren, ohne dass Anwender an eine bestimmte Software oder einen bestimmten Hersteller gebunden sind.

GBUXML verfolgt deshalb bewusst einen Open-Source- und Open-Format-Ansatz.

Die Spezifikation soll öffentlich verfügbar sein. Jeder soll das Format implementieren, GBUXML-Dateien erzeugen, lesen, validieren und – abhängig von der gewählten Lizenz – in eigene Anwendungen integrieren können.

## Der GBUXML Viewer

Ein offenes Dateiformat ist nur dann praktisch nutzbar, wenn die darin enthaltenen Informationen auch ohne spezielle kommerzielle Software betrachtet werden können.

Deshalb gehört zum Projekt ein eigener GBUXML Viewer.

Der Viewer soll eine möglichst einfache Referenzimplementierung des Formats darstellen und auf den gängigen Desktop-Plattformen lauffähig sein.

Er soll unter anderem:

- GBUXML-Dateien öffnen und gegen das zugehörige XSD-Schema validieren,
- allgemeine Informationen einer Gefährdungsbeurteilung darstellen,
- Gewerke, Bereiche, Prozesse, Gefährdungen und Maßnahmen übersichtlich in Tabellenform anzeigen,
- zwischen einzelnen Bereichen ähnlich wie zwischen Tabellenblättern wechseln,
- eine Übersicht über vorhandene Gefährdungen und deren Risikoeinstufungen erzeugen,
- offene und abgeschlossene Maßnahmen auswerten,
- einen optionalen Bearbeitungsmodus bereitstellen,
- Änderungen wieder als valide GBUXML-Datei speichern und
- wiederverwendbare Stammdaten für Gewerke, Bereiche, Prozesse, Gefährdungen und Maßnahmen verwalten.

Der Viewer soll dabei nicht zum proprietären Mittelpunkt des Formats werden. GBUXML ist das eigentliche Projekt; der Viewer ist eine Referenzanwendung für dieses Format.

Andere Entwickler sollen ausdrücklich eigene Anwendungen, Viewer, Editoren, Konverter oder Schnittstellen auf Basis von GBUXML entwickeln können.

## Technischer Stack

Für die Referenzimplementierung des GBUXML Viewers wird ein bewusst überschaubarer und langfristig wartbarer Java-Stack verwendet.

### Java 21

Java 21 LTS bildet die Grundlage der Anwendung.

Die Verwendung einer Long-Term-Support-Version bietet eine stabile Basis für die langfristige Weiterentwicklung des Projekts. Java ermöglicht zudem eine weitgehend plattformunabhängige Implementierung der Geschäftslogik.

### JavaFX

Die grafische Benutzeroberfläche wird mit JavaFX umgesetzt.

JavaFX eignet sich insbesondere für die geplanten tabellarischen Ansichten und ermöglicht unter anderem:

- editierbare Tabellen,
- Auswahlfelder innerhalb von Tabellen,
- Navigation zwischen Bereichen,
- Dialoge und Formulare,
- Validierungsanzeigen,
- Such- und Filterfunktionen sowie
- eine plattformübergreifende Desktop-Oberfläche.

Die Oberfläche kann über CSS gestaltet werden, wodurch Darstellung und Anwendungslogik weitgehend voneinander getrennt bleiben.

### JAXB

JAXB (Jakarta XML Binding) übernimmt die Abbildung zwischen XML und Java-Objekten.

Eine GBUXML-Datei kann dadurch in ein objektorientiertes Java-Datenmodell eingelesen werden. Umgekehrt können Änderungen am Datenmodell wieder als XML gespeichert werden.

Vereinfacht entsteht damit die Verarbeitungskette:

GBUXML → JAXB → Java-Datenmodell → JavaFX

und beim Speichern entsprechend:

JavaFX → Java-Datenmodell → JAXB → GBUXML

### XSD – XML Schema Definition

Das XSD-Schema ist ein zentraler Bestandteil von GBUXML.

Es definiert formal, wie eine gültige GBUXML-Datei aufgebaut sein muss. Dazu gehören beispielsweise:

- erlaubte Elemente,
- hierarchische Beziehungen,
- Datentypen,
- Pflichtfelder,
- optionale Felder und
- Kardinalitäten.

GBUXML soll dadurch nicht lediglich eine Konvention darstellen, sondern ein technisch eindeutig beschriebenes und maschinell validierbares Austauschformat sein.

Das XSD gehört zur öffentlichen Spezifikation und kann somit auch von Anwendungen verwendet werden, die nichts mit dem offiziellen Viewer zu tun haben.

### SQLite

Für lokale, vom jeweiligen GBUXML-Dokument unabhängige Daten wird SQLite eingesetzt.

Insbesondere die Stammdaten sollen nicht an eine einzelne Gefährdungsbeurteilung gebunden sein. Ein Anwender soll beispielsweise häufig verwendete Prozesse, Gefährdungen oder Maßnahmen hinterlegen und anschließend in unterschiedlichen Gefährdungsbeurteilungen wiederverwenden können.

SQLite eignet sich dafür, weil keine separate Datenbankinstallation oder ein Datenbankserver erforderlich ist. Die Datenbank kann lokal durch die Anwendung verwaltet werden.

Die Trennung bleibt damit eindeutig:

- GBUXML = austauschbare Gefährdungsbeurteilung
- SQLite = lokale Stammdaten des Viewers

Das eigentliche Austauschformat bleibt vollständig unabhängig von SQLite.

### jpackage

Ein wichtiges Ziel des Projekts ist eine möglichst unkomplizierte Nutzung.

Anwender sollen nicht zunächst eine Java-Entwicklungsumgebung einrichten oder manuell Abhängigkeiten installieren müssen.

Mit jpackage können deshalb plattformspezifische Anwendungspakete einschließlich der benötigten Java-Runtime erstellt werden.

So können beispielsweise Installationspakete für Windows, Linux und macOS bereitgestellt werden, während die eigentliche Anwendung auf einer gemeinsamen Java-Codebasis entwickelt wird.

## Projektziele

GBUXML verfolgt langfristig vier zentrale Ziele:

- Offenheit – Das Datenformat und seine Spezifikation sollen öffentlich dokumentiert und frei implementierbar sein.
- Interoperabilität – Gefährdungsbeurteilungen sollen zwischen unterschiedlichen Anwendungen und Organisationen ausgetauscht werden können.
- Nachvollziehbarkeit – Die Datenstruktur soll möglichst einfach, logisch und auch außerhalb einer bestimmten Software verständlich bleiben.
- Herstellerunabhängigkeit – Die langfristige Nutzbarkeit einer Gefährdungsbeurteilung darf nicht davon abhängen, ob eine bestimmte Software oder ein bestimmter Anbieter weiterhin existiert.

## Mitmachen

GBUXML soll nicht als abgeschlossenes proprietäres Format entstehen, sondern als offenes Projekt.

Diskussionen über die Struktur des XML-Formats, Erweiterungen des XSD-Schemas, Anforderungen aus unterschiedlichen Branchen, Verbesserungsvorschläge für den Viewer und alternative Implementierungen sind ausdrücklich willkommen.

Das langfristige Ziel ist einfach:

Eine Gefährdungsbeurteilung sollte nicht an die Software gebunden sein, mit der sie erstellt wurde.

GBUXML soll dafür eine offene technische Grundlage schaffen.

## Installation für Endanwender

Wer die Software einfach nutzen möchte, ohne selbst zu bauen, findet fertige Installationspakete auf der [Releases-Seite](https://github.com/klaus4772/GBUXML/releases):

- **Windows**: `.msi` – Doppelklick zum Installieren
- **macOS**: `.dmg` – öffnen und die App in den `Applications`-Ordner ziehen (da das Paket nicht signiert ist, muss beim ersten Start ggf. per Rechtsklick → „Öffnen" bestätigt werden, da macOS Gatekeeper sonst warnt)
- **Linux**: `.deb` – z. B. mit `sudo apt install ./GBUXML-<version>.deb`

Jede dieser Dateien enthält eine eigene, gebündelte Java-Runtime; es muss keine Java-Umgebung separat installiert werden.

Die Installationspakete werden automatisch von GitHub Actions (`.github/workflows/release.yml`) gebaut, sobald ein Git-Tag im Format `vX.Y.Z` (z. B. `v1.1.0`) gepusht wird. Der Workflow baut parallel auf Windows-, macOS- und Linux-Runnern mit `jpackage` und veröffentlicht die drei Installer als Anhänge des zugehörigen GitHub Release.

## Lizenz

GBUXML (Format-Spezifikation, XSD und Referenz-Viewer) steht unter der
[Apache License 2.0](LICENSE). Damit kann jeder das Format und den
Viewer-Code frei nutzen, verändern und auch in eigene, auch proprietäre
Anwendungen integrieren. Die Lizenz enthält zudem eine explizite
Patent-Freigabe aller Mitwirkenden.

Die gebauten Installationspakete bündeln zusätzlich einige
Drittanbieter-Bibliotheken unter ihren jeweils eigenen (permissiven)
Lizenzen; Details dazu stehen in [THIRD-PARTY-LICENSES.md](THIRD-PARTY-LICENSES.md).

## Ausführen

Wichtig: JavaFX darf nicht wie eine normale Java-Klasse gestartet werden. In IntelliJ muss die App über Maven gestartet werden, damit die JavaFX-Module korrekt auf dem Module Path liegen.

```bash
mvn clean package
mvn javafx:run
```

Wenn du die App direkt in IntelliJ starten möchtest:

1. `Run` → `Edit Configurations...`
2. Neue Konfiguration hinzufügen
3. `Maven` auswählen
4. `Goals`: `javafx:run`
5. `Working directory`: Projektordner

Die Projektdatei `.idea/runConfigurations/GBUXMLApplication.xml` enthält bereits eine passende IntelliJ-Konfiguration.

## SQLite

Die Anwendung verwendet SQLite als lokale Datenbank unter:

```text
%USERPROFILE%\.gbuxml\gbuxml.db
```

Dort werden Stammdaten für Kategorien wie Gewerke, Bereiche, Prozesse, Gefährdungen und Maßnahmen gespeichert.

## jpackage für Portable-Distributionen

Nach einem erfolgreichen Build erzeugt Maven automatisch ein plattformspezifisches, lauffähiges App-Image.

```bash
mvn clean package
```

Erstellt wird dann je nach Betriebssystem ein Paket im Ordner:

- Windows: `target\jpackage\windows\GBUXML`
- Linux: `target/jpackage/linux/GBUXML`
- macOS: `target/jpackage/mac/GBUXML`

Zusätzlich werden auch portable ZIP-Dateien erzeugt, z. B.:

- `target\GBUXML-portable-windows.zip`
- `target/GBUXML-portable-linux.zip`
- `target/GBUXML-portable-mac.zip`

Für ein direktes jpackage-Beispiel ohne Maven:

```powershell
C:\Program Files\Java\jdk-21.0.12\bin\jpackage.exe --type app-image --name GBUXML --app-version 1.0 --vendor "GBUXML" --input target --main-jar GBUXML-1.0-SNAPSHOT.jar --main-class com.gbuxml.GBUXMLLauncher --dest target\jpackage\windows
```

Hinweis: Ein echter Windows-Installer (`.exe`/`.msi`) benötigt zusätzlich WiX. Für einfache Verteilung ist das automatische `app-image`-Paket mit ZIP-Datei die beste und unkomplizierteste Lösung.

## Wichtige Dateien

- `src/main/resources/gbuxml.xsd` – XML-Schema
- `src/main/resources/sample-gbuxml.xml` – generisches Beispiel-Datei
- `src/main/resources/sample-geruestbau-gbuxml.xml` – Beispiel aus der Excel-Datei `Söll Gerüstbau Gefährdungsbeurteilung.xlsx`, soweit im XML-Format abgebildet
- `src/main/java/com/gbuxml/GBUXMLApplication.java` – GUI-Viewer
- `src/main/java/com/gbuxml/PdfExportService.java` – PDF-Export für Druck und Ausdruck
- `pom.xml` – Java 21 + JavaFX + JAXB + SQLite + PDFBox-Konfiguration

## IDE-Sprache wechseln

### IntelliJ IDEA / JetBrains IDE

1. `File` → `Project Structure`
2. `Project` oder `Modules`
3. `SDK` / `Language level` auswählen
4. Java 21 als SDK setzen
5. Falls nötig: `Settings` → `Plugins` → `JavaFX`/`Java`-Support prüfen

### VS Code

1. Unten rechts auf das Interpreter-Symbol klicken
2. `Select Interpreter` wählen
3. Java 21 oder passende JDK auswählen

### Python oder andere Sprache

Falls du stattdessen Python, C# oder ein anderes Frontend nutzen willst:

1. Neue Projektvorlage erstellen
2. SDK/Interpreter wählen
3. Abhängig von der Sprache die passenden Pakete/Plugins installieren

Für dieses Projekt bleibt Java 21 mit JavaFX die beste Wahl, weil sie die gewünschten GUI-, XML-, Datenbank- und Packaging-Anforderungen am saubersten abdeckt.
