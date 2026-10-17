package dev.qrm.medical.mc;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Gli oggetti del comparto EMS. Il loro effetto sta in {@link MedicalEvents}: sono oggetti semplici. */
public final class MedicalItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MedicalMod.MOD_ID);

    public static final DeferredItem<Item> DEFIBRILLATOR =
            ITEMS.registerSimpleItem("defibrillator", props -> props.stacksTo(1));
    public static final DeferredItem<Item> MEDKIT = ITEMS.registerSimpleItem("medkit", props -> props.stacksTo(8));
    public static final DeferredItem<Item> BANDAGE = ITEMS.registerSimpleItem("bandage", props -> props.stacksTo(16));

    private MedicalItems() {}
}
