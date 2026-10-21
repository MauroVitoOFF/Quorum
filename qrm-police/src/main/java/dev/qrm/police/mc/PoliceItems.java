package dev.qrm.police.mc;

import dev.qrm.police.weapon.mc.MagazineItem;
import dev.qrm.police.weapon.mc.PistolItem;
import dev.qrm.police.weapon.mc.WeaponComponents;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Gli oggetti del comparto polizia. */
public final class PoliceItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PoliceMod.MOD_ID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(PoliceMod.MOD_ID);

    public static final DeferredItem<PoliceTabletItem> TABLET =
            ITEMS.registerItem("police_tablet", PoliceTabletItem::new, props -> props.stacksTo(1));
    public static final DeferredBlock<PoliceComputerBlock> COMPUTER = BLOCKS.registerBlock("police_computer",
            PoliceComputerBlock::new, props -> props.strength(3.5F).requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> COMPUTER_ITEM = ITEMS.registerSimpleBlockItem(COMPUTER);

    /** Pistola d'ordinanza: nasce scarica, con id segnaposto sostituito dal server al primo tick. */
    public static final DeferredItem<PistolItem> PISTOL = ITEMS.registerItem("police_pistol", PistolItem::new,
            props -> props.stacksTo(1).component(WeaponComponents.PISTOL_STATE.get(), WeaponComponents.DEFAULT_PISTOL));
    /** Caricatore: uno per pila, perché ognuno ha il proprio id; nasce pieno. */
    public static final DeferredItem<MagazineItem> MAGAZINE = ITEMS.registerItem("police_magazine", MagazineItem::new,
            props -> props.stacksTo(1).component(WeaponComponents.MAGAZINE.get(), WeaponComponents.DEFAULT_MAGAZINE));

    private PoliceItems() {}
}
