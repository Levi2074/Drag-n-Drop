package de.listdrag;

import de.listdrag.mixin.EntryListWidgetAccessor;
import de.listdrag.mixin.PackEntryAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.screen.pack.ResourcePackOrganizer;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.option.ServerList;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Linksklick auf einen Eintrag der Server- oder Texturepack-Liste, Maus bewegen: Der Eintrag hängt
 * leicht durchsichtig am Cursor. Beim Ziehen erscheinen am Eintrag unter dem Cursor zwei dünne Linien:
 * oben grün (davor einfügen), unten rot (danach einfügen). Die Hälfte, in der der Cursor steht, ist
 * die aktive (dicker). Loslassen übernimmt, Loslassen außerhalb einer Liste bricht ab.
 */
final class DragHandler {

    private static final double DRAG_THRESHOLD = 4.0;
    private static final int GREEN = 0x55FF55;
    private static final int RED = 0xFF5555;

    private static boolean pending;
    private static boolean dragging;
    private static Click blockedClick;      // Texturepack-Screen: Klick zurückgehalten, falls doch kein Drag
    private static double startX;
    private static double startY;
    private static double lastX;
    private static double lastY;

    private static EntryListWidget<?> srcList;
    private static Object grabbed;
    private static int srcIndex;
    private static int serverCount;
    private static int grabOffsetX;
    private static int grabOffsetY;
    private static int ghostW;
    private static int ghostH;
    private static Text label = Text.empty();
    private static Text subLabel = null;

    // Ziel (aus dem letzten Render)
    private static EntryListWidget<?> dropList;
    private static Object dropHovered;
    private static boolean dropAfter;

    // nur Texturepacks
    private static ResourcePackOrganizer.Pack grabbedPack;
    private static String packName;
    private static boolean packWasEnabled;

    private DragHandler() {
    }

    static void reset() {
        pending = false;
        dragging = false;
        blockedClick = null;
        srcList = null;
        grabbed = null;
        grabbedPack = null;
        packName = null;
        dropList = null;
        dropHovered = null;
        subLabel = null;
    }

    // ---------------------------------------------------------------- Eingabe

    /** @return true = Klick normal weiterverarbeiten, false = Klick unterdrücken */
    static boolean onPress(Screen screen, Click click) {
        try {
            lastX = click.x();
            lastY = click.y();
            if (click.button() != 0) {
                return true;
            }
            reset();

            for (EntryListWidget<?> list : lists(screen)) {
                if (!list.isMouseOver(click.x(), click.y())) {
                    continue;
                }
                Object entry = hovered(list);
                if (entry == null) {
                    return true;
                }
                int idx = list.children().indexOf(entry);
                if (idx < 0) {
                    return true;
                }

                boolean blockPress = false;
                if (screen instanceof MultiplayerScreen mp) {
                    ServerList sl = mp.getServerList();
                    if (idx >= sl.size()) {
                        return true; // LAN-/Scanner-Einträge nicht verschiebbar
                    }
                    serverCount = sl.size();
                    label = Text.literal(sl.get(idx).name);
                    subLabel = Text.literal(sl.get(idx).address);
                } else {
                    ResourcePackOrganizer.Pack pack = packOf(entry);
                    if (pack == null) {
                        return true;
                    }
                    grabbedPack = pack;
                    label = pack.getDisplayName();
                    packName = label.getString();
                    packWasEnabled = pack.isEnabled();
                    // Ein Klick auf ein Pack schaltet es sofort um – deshalb erst beim Loslassen
                    // abspielen, falls es kein Drag war.
                    blockPress = true;
                }

                srcList = list;
                grabbed = entry;
                srcIndex = idx;
                startX = click.x();
                startY = click.y();
                pending = true;

                ghostW = Math.max(60, list.getRowWidth() - 4);
                ghostH = 32;
                int[] b = bounds(list, entry);
                grabOffsetX = (int) (click.x() - list.getRowLeft());
                grabOffsetY = b != null ? (int) (click.y() - b[0]) : ghostH / 2;

                if (blockPress) {
                    blockedClick = click;
                    return false;
                }
                return true;
            }
        } catch (Throwable t) {
            fail(t);
        }
        return true;
    }

    static boolean onRelease(Screen screen, Click click) {
        lastX = click.x();
        lastY = click.y();
        if (click.button() != 0 || !pending) {
            return true;
        }
        boolean wasDragging = dragging;
        Click replay = blockedClick;
        try {
            if (wasDragging) {
                finish(screen);
            } else if (replay != null) {
                screen.mouseClicked(replay, false); // normaler Klick (z. B. Pack an-/ausschalten)
            }
        } catch (Throwable t) {
            fail(t);
        }
        reset();
        return !wasDragging; // Release nur schlucken, wenn wirklich gezogen wurde
    }

    // ---------------------------------------------------------------- Rendern

    static void onRender(Screen screen, DrawContext context, int mouseX, int mouseY) {
        if (!pending) {
            return;
        }
        lastX = mouseX;
        lastY = mouseY;
        try {
            if (!dragging) {
                if (Math.hypot(mouseX - startX, mouseY - startY) < DRAG_THRESHOLD) {
                    return;
                }
                dragging = true;
            }

            // Liste unter dem Cursor + Eintrag darin
            dropList = null;
            dropHovered = null;
            for (EntryListWidget<?> list : lists(screen)) {
                if (list.isMouseOver(mouseX, mouseY)) {
                    dropList = list;
                }
            }
            if (dropList != null) {
                dropHovered = hovered(dropList);
            }

            // Linien (nur wo ein Ablegen sinnvoll ist)
            boolean linesAllowed = dropList != null && dropHovered != null;
            if (linesAllowed && screen instanceof MultiplayerScreen) {
                linesAllowed = dropList == srcList && dropList.children().indexOf(dropHovered) < serverCount;
            }
            if (linesAllowed && screen instanceof PackScreen) {
                linesAllowed = !(dropList == srcList && !packWasEnabled); // inaktive Packs sind nicht sortierbar
            }
            if (linesAllowed) {
                int[] b = bounds(dropList, dropHovered);
                if (b != null) {
                    dropAfter = mouseY >= (b[0] + b[1]) / 2;
                    int x = dropList.getRowLeft();
                    int w = dropList.getRowWidth();
                    // obere Linie = grün (davor), untere Linie = rot (danach); aktive ist dicker
                    line(context, x, b[0] - 1, w, !dropAfter ? 2 : 1, GREEN, !dropAfter);
                    line(context, x, b[1] - 1, w, dropAfter ? 2 : 1, RED, dropAfter);
                }
            }

            // Der gezogene Eintrag, leicht durchsichtig, hängt am Cursor
            int gx = mouseX - grabOffsetX;
            int gy = mouseY - grabOffsetY;
            TextRenderer tr = MinecraftClient.getInstance().textRenderer;
            context.fill(gx, gy, gx + ghostW, gy + ghostH, 0x80101010);
            context.fill(gx, gy, gx + ghostW, gy + 1, 0xA0FFFFFF);
            context.fill(gx, gy + ghostH - 1, gx + ghostW, gy + ghostH, 0xA0FFFFFF);
            context.fill(gx, gy, gx + 1, gy + ghostH, 0xA0FFFFFF);
            context.fill(gx + ghostW - 1, gy, gx + ghostW, gy + ghostH, 0xA0FFFFFF);
            context.drawTextWithShadow(tr, label, gx + 6, gy + 6, 0xB0FFFFFF);
            if (subLabel != null) {
                context.drawTextWithShadow(tr, subLabel, gx + 6, gy + 18, 0x90AAAAAA);
            }
        } catch (Throwable t) {
            fail(t);
        }
    }

    private static void line(DrawContext c, int x, int y, int w, int thickness, int rgb, boolean active) {
        int alpha = active ? 0xFF : 0x99;
        c.fill(x, y, x + w, y + thickness, (alpha << 24) | rgb);
    }

    // ---------------------------------------------------------------- Übernehmen

    private static void finish(Screen screen) {
        if (dropList == null) {
            return; // außerhalb einer Liste losgelassen = abbrechen
        }

        if (screen instanceof MultiplayerScreen mp) {
            if (dropList != srcList || dropHovered == null) {
                return;
            }
            int hoveredIdx = dropList.children().indexOf(dropHovered);
            if (hoveredIdx < 0 || hoveredIdx >= serverCount) {
                return;
            }
            int ins = hoveredIdx + (dropAfter ? 1 : 0);
            int newIdx = ins > srcIndex ? ins - 1 : ins;
            if (newIdx == srcIndex) {
                return;
            }
            ServerList sl = mp.getServerList();
            int step = Integer.signum(newIdx - srcIndex);
            for (int i = srcIndex; i != newIdx; i += step) {
                sl.swapEntries(i, i + step);
            }
            sl.saveFile();
            ((MultiplayerServerListWidget) srcList).setServers(sl);
            return;
        }

        if (screen instanceof PackScreen) {
            int srcPackIdx = packIndex(srcList, grabbed);

            if (dropList == srcList) {
                if (!packWasEnabled || dropHovered == null || srcPackIdx < 0) {
                    return;
                }
                int hp = packIndex(dropList, dropHovered);
                if (hp < 0) {
                    return;
                }
                int ins = hp + (dropAfter ? 1 : 0);
                int newIdx = ins > srcPackIdx ? ins - 1 : ins;
                if (newIdx == srcPackIdx) {
                    return;
                }
                repositionPack(screen, packName, newIdx);
            } else if (packWasEnabled) {
                grabbedPack.disable(); // aktiv -> inaktiv
            } else {
                // inaktiv -> aktiv, dann an die Zielposition schieben
                int ins = packCount(dropList);
                if (dropHovered != null) {
                    int hp = packIndex(dropList, dropHovered);
                    if (hp >= 0) {
                        ins = hp + (dropAfter ? 1 : 0);
                    }
                }
                grabbedPack.enable();
                repositionPack(screen, packName, ins);
            }
        }
    }

    /** Schiebt das aktive Pack mit diesem Namen Schritt für Schritt auf den Zielindex. */
    private static void repositionPack(Screen screen, String name, int targetIndex) {
        boolean invert = false;   // falls "Start" im Spiel unten statt oben bedeutet
        int lastIdx = -1;
        int lastWanted = 0;       // -1 = nach oben gewollt, +1 = nach unten gewollt

        for (int n = 0; n < 512; n++) {
            EntryListWidget<?> selected = selectedList(screen);
            if (selected == null) {
                return;
            }
            int idx = -1;
            ResourcePackOrganizer.Pack found = null;
            int packPos = 0;
            for (Object e : selected.children()) {
                ResourcePackOrganizer.Pack p = packOf(e);
                if (p == null) {
                    continue;
                }
                if (name.equals(p.getDisplayName().getString())) {
                    idx = packPos;
                    found = p;
                    break;
                }
                packPos++;
            }
            if (found == null || idx == targetIndex) {
                return;
            }

            if (lastWanted != 0 && lastIdx >= 0 && (idx - lastIdx) == -lastWanted) {
                invert = !invert;
            }

            int wanted = idx > targetIndex ? -1 : 1;
            boolean towardStart = (wanted < 0) != invert;
            if (towardStart) {
                if (!found.canMoveTowardStart()) {
                    return;
                }
                found.moveTowardStart();
            } else {
                if (!found.canMoveTowardEnd()) {
                    return;
                }
                found.moveTowardEnd();
            }
            lastIdx = idx;
            lastWanted = wanted;
        }
    }

    // ---------------------------------------------------------------- Hilfsfunktionen

    /** Eintrag unter dem Cursor (letzte bekannte Mausposition), oder null. */
    private static Object hovered(EntryListWidget<?> list) {
        for (Object e : list.children()) {
            if (e instanceof Element element && element.isMouseOver(lastX, lastY)) {
                return e;
            }
        }
        return null;
    }

    /** {Oberkante, Unterkante} der Zeile in Bildschirmkoordinaten, oder null. */
    private static int[] bounds(EntryListWidget<?> list, Object entry) {
        int idx = list.children().indexOf(entry);
        if (idx < 0) {
            return null;
        }
        if ((Object) list instanceof EntryListWidgetAccessor accessor) {
            try {
                return new int[]{accessor.listdrag$getRowTop(idx), accessor.listdrag$getRowBottom(idx)};
            } catch (Throwable ignored) {
                // weiter mit Fallback
            }
        }
        // Fallback: Grenzen der Zeile durch Abtasten bestimmen
        if (entry instanceof Element element) {
            int x = (int) lastX;
            int y = (int) lastY;
            if (!element.isMouseOver(x, y)) {
                return null;
            }
            int top = y;
            while (top > y - 80 && element.isMouseOver(x, top - 1)) {
                top--;
            }
            int bottom = y;
            while (bottom < y + 80 && element.isMouseOver(x, bottom + 1)) {
                bottom++;
            }
            return new int[]{top, bottom + 1};
        }
        return null;
    }

    private static List<EntryListWidget<?>> lists(Screen screen) {
        List<EntryListWidget<?>> result = new ArrayList<>();
        for (Element e : screen.children()) {
            if (e instanceof EntryListWidget<?> list) {
                result.add(list);
            }
        }
        return result;
    }

    /** Rechte Liste im Texturepack-Screen = aktive Packs. */
    private static EntryListWidget<?> selectedList(Screen screen) {
        EntryListWidget<?> best = null;
        for (EntryListWidget<?> list : lists(screen)) {
            if (best == null || list.getX() > best.getX()) {
                best = list;
            }
        }
        return best;
    }

    private static ResourcePackOrganizer.Pack packOf(Object entry) {
        if (entry instanceof PackEntryAccessor accessor) {
            return accessor.listdrag$getPack();
        }
        return null;
    }

    /** Index unter den Pack-Einträgen (Überschriften o. Ä. werden nicht mitgezählt). */
    private static int packIndex(EntryListWidget<?> list, Object entry) {
        int pos = 0;
        for (Object e : list.children()) {
            if (packOf(e) == null) {
                continue;
            }
            if (e == entry) {
                return pos;
            }
            pos++;
        }
        return -1;
    }

    private static int packCount(EntryListWidget<?> list) {
        int n = 0;
        for (Object e : list.children()) {
            if (packOf(e) != null) {
                n++;
            }
        }
        return n;
    }

    private static void fail(Throwable t) {
        System.err.println("[listdrag] Fehler: " + t);
        t.printStackTrace();
        reset();
    }
}
