package de.listdrag.mixin;

import net.minecraft.client.gui.screen.pack.ResourcePackOrganizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Macht das (private) Pack-Objekt eines Eintrags in der Texturepack-Liste lesbar. */
@Mixin(targets = "net.minecraft.client.gui.screen.pack.PackListWidget$ResourcePackEntry")
public interface PackEntryAccessor {

    @Accessor("pack")
    ResourcePackOrganizer.Pack listdrag$getPack();
}
