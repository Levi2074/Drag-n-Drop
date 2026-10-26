# Drag n Drop (Fabric 1.21.11, clientseitig)

Einträge in der **Serverliste** und der **Texturepack-Liste** per Drag & Drop verschieben.
Die Oberflächen selbst werden nicht verändert.

## Benutzung
1. **Linksklick auf einen Eintrag halten** und die Maus bewegen.
2. Der Eintrag hängt leicht durchsichtig am Cursor.
3. Am Eintrag unter dem Cursor erscheinen zwei dünne Linien:
   - **oben grün** = davor einfügen
   - **unten rot** = danach einfügen
   Die Hälfte, in der der Cursor steht, ist aktiv (dickere Linie).
4. **Loslassen** übernimmt die Position. Loslassen außerhalb einer Liste bricht ab.

Ein normaler Klick (ohne Ziehen) funktioniert weiter wie im Spiel (Server auswählen, Pack an-/ausschalten).

### Serverliste
Reihenfolge ändern; wird sofort in `servers.dat` gespeichert. (LAN-Einträge sind nicht verschiebbar.)

### Texturepacks
- Aktive Packs (rechte Liste) innerhalb der Liste umsortieren.
- Pack von links nach rechts ziehen = aktivieren (an der Position unter dem Cursor), von rechts nach links = deaktivieren.
- Fest eingestellte Packs (z. B. Vanilla/Server-Packs) lassen sich wie im Spiel nicht verschieben.

## Bauen
Java 21 nötig. Beim ersten Start lädt `gradlew` Gradle selbst herunter.

    Windows:     gradlew.bat build
    Linux/macOS: ./gradlew build

Fertige Mod: `build/libs/drag-n-drop-1.1.0.jar` (NICHT die -sources.jar) in den mods-Ordner.
Benötigt: Fabric Loader, Fabric API.
