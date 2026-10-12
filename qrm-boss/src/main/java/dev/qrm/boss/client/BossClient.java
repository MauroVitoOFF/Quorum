package dev.qrm.boss.client;

import dev.qrm.client.api.theme.ThemeFragments;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/** Parte client del menu boss. Caricata solo sul client: il server dedicato non la tocca mai. */
@Mod(value = "qrm_boss", dist = Dist.CLIENT)
public final class BossClient {
    public BossClient(IEventBus modBus) {
        // Chiavi di tema del modulo: iniziano per "qrm_boss."; un resource pack può sostituirne i valori.
        ThemeFragments.register("qrm_boss", Identifier.fromNamespaceAndPath("qrm_boss", "theme/qrm.json"));
    }
}
