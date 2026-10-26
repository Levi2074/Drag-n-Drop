package de.listdrag.mixin;

import net.minecraft.client.gui.widget.EntryListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Oberkante/Unterkante einer Listenzeile (protected im Spiel). */
@Mixin(EntryListWidget.class)
public interface EntryListWidgetAccessor {

    @Invoker("getRowTop")
    int listdrag$getRowTop(int index);

    @Invoker("getRowBottom")
    int listdrag$getRowBottom(int index);
}
