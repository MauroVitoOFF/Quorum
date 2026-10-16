package dev.qrm.wardrobe.mc;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/** Divise di lavoro: skin cambiate da un armadietto, sui punti di interazione di tipo {@code wardrobe}. */
@Mod(WardrobeMod.MOD_ID)
public final class WardrobeMod {
    public static final String MOD_ID = "qrm_wardrobe";
    public static final String POINT_TYPE = "wardrobe";

    public WardrobeMod(IEventBus modBus) {}
}
