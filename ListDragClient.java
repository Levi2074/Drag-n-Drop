package de.listdrag;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.pack.PackScreen;

public class ListDragClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof MultiplayerScreen) && !(screen instanceof PackScreen)) {
                return;
            }

            DragHandler.reset();

            ScreenMouseEvents.allowMouseClick(screen).register(
                    (s, click) -> DragHandler.onPress(s, click));
            ScreenMouseEvents.allowMouseRelease(screen).register(
                    (s, click) -> DragHandler.onRelease(s, click));
            ScreenEvents.afterRender(screen).register(
                    (s, context, mouseX, mouseY, tickDelta) -> DragHandler.onRender(s, context, mouseX, mouseY));
        });
    }
}
