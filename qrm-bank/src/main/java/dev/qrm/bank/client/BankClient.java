package dev.qrm.bank.client;

import dev.qrm.bank.net.StaffBankStatePayload;
import dev.qrm.api.staff.StaffNodes;
import dev.qrm.client.api.staff.StaffEntries;
import dev.qrm.client.api.theme.ThemeFragments;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/** Parte client del mod banca. Caricata solo sul client: il server dedicato non la tocca mai. */
@Mod(value = "qrm_bank", dist = Dist.CLIENT)
public final class BankClient {
    public BankClient(IEventBus modBus) {
        // Chiavi di tema del modulo: iniziano per "qrm_bank."; un resource pack può sostituirne i valori.
        ThemeFragments.register("qrm_bank", Identifier.fromNamespaceAndPath("qrm_bank", "theme/qrm.json"));
        // Voce "Banca" del pannello staff (F7), sezione Economy: compare solo se il server ha la banca.
        StaffEntries.register(new StaffEntries.Entry("bank", StaffEntries.Section.ECONOMY, "qrm_bank.staff.tile",
                StaffNodes.BANK, true, BankClient::serverHasBank,
                ctx -> Minecraft.getInstance().setScreenAndShow(new StaffBankScreen(ctx))));
    }

    private static boolean serverHasBank() {
        var connection = Minecraft.getInstance().getConnection();
        return connection != null && connection.hasChannel(StaffBankStatePayload.TYPE);
    }
}
