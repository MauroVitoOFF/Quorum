package dev.qrm.bank.mc;

import dev.qrm.bank.logic.CashDenominations;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BankItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BankMod.MOD_ID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BankMod.MOD_ID);

    /** Tutti i tagli condividono un'unica definizione di modello (assets/qrm_bank/items/cash.json). */
    private static final Identifier CASH_MODEL = Identifier.fromNamespaceAndPath(BankMod.MOD_ID, "cash");
    private static final Map<String, Map<Long, DeferredItem<CashItem>>> CASH = new LinkedHashMap<>();

    public static final DeferredBlock<AtmBlock> ATM =
            BLOCKS.registerBlock("atm", AtmBlock::new, props -> props.strength(3.5F).requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> ATM_ITEM = ITEMS.registerSimpleBlockItem(ATM);

    private BankItems() {}

    /** Da chiamare nel costruttore del mod, prima di registrare {@link #ITEMS}. */
    public static void registerCash(CashDenominations denominations) {
        for (String currency : denominations.currencies()) {
            Map<Long, DeferredItem<CashItem>> byAmount = new LinkedHashMap<>();
            for (long amount : denominations.descending(currency)) {
                byAmount.put(amount, ITEMS.registerItem(
                        CashDenominations.itemName(currency, amount), props -> new CashItem(props, currency, amount)));
            }
            CASH.put(currency, byAmount);
        }
    }

    /**
     * Nome e modello dei tagli. Non si possono dare dalle proprietà dell'item: in 26.2 {@code Item.Properties}
     * imposta {@code ITEM_NAME} e {@code ITEM_MODEL} dall'id dopo i componenti del mod, sovrascrivendoli. Qui si
     * correggono i componenti predefiniti, così tutti i tagli condividono un solo nome e un solo modello.
     */
    public static void onModifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        CashDenominations denominations = BankMod.denominations();
        CASH.forEach((currency, byAmount) -> byAmount.forEach((amount, item) -> {
            Component name = Component.translatable("item.qrm_bank.cash", denominations.label(currency, amount));
            event.modify(item, builder -> builder.set(DataComponents.ITEM_NAME, name)
                    .set(DataComponents.ITEM_MODEL, CASH_MODEL));
        }));
    }

    public static DeferredItem<CashItem> item(String currency, long amount) {
        Map<Long, DeferredItem<CashItem>> byAmount = CASH.get(currency);
        DeferredItem<CashItem> item = byAmount == null ? null : byAmount.get(amount);
        if (item == null) throw new IllegalArgumentException("no cash item for " + currency + " " + amount);
        return item;
    }
}
