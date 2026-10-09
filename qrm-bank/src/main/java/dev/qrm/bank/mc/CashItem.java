package dev.qrm.bank.mc;

import net.minecraft.world.item.Item;

/** Banconota di un taglio: il valore sta nella definizione dell'item, non in dati per-istanza (niente falsificazioni). */
public final class CashItem extends Item {
    private final String currency;
    private final long amount;

    public CashItem(Item.Properties properties, String currency, long amount) {
        super(properties);
        this.currency = currency;
        this.amount = amount;
    }

    public String currency() {
        return currency;
    }

    /** Valore in unità minime. */
    public long amount() {
        return amount;
    }
}
