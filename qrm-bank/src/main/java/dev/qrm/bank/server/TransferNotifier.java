package dev.qrm.bank.server;

import dev.qrm.api.QRM;
import dev.qrm.api.character.CharacterService;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.economy.CurrencyRegistry;
import dev.qrm.api.id.CharacterId;
import dev.qrm.bank.api.BankTransferEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Avvisa in chat chi riceve un bonifico, se è online con quel personaggio attivo. I bonifici verso un'organizzazione
 * non generano avvisi.
 */
public final class TransferNotifier {
    private TransferNotifier() {}

    public static void onTransfer(BankTransferEvent event) {
        if (event.toCharacter() == null) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        CharacterService characters = QRM.get(CharacterService.class);
        PlayerCharacter recipient = characters.find(event.toCharacter()).orElse(null);
        if (recipient == null) return;
        ServerPlayer player = server.getPlayerList().getPlayer(recipient.playerId().value());
        if (player == null) return;
        // Solo se il personaggio che riceve è quello in uso: gli altri lo vedranno nello storico.
        boolean active = characters.activeOf(recipient.playerId()).map(PlayerCharacter::id)
                .filter(event.toCharacter()::equals).isPresent();
        if (!active) return;

        Component amount = Component.literal(format(event));
        Component from = Component.literal(nameOf(characters, event.from()));
        player.sendSystemMessage(event.note().isEmpty()
                ? Component.translatable("qrm_bank.msg.received", amount, from)
                : Component.translatable("qrm_bank.msg.received_note", amount, from, event.note()));
    }

    private static String nameOf(CharacterService characters, CharacterId id) {
        return characters.find(id).map(pc -> pc.firstName() + " " + pc.lastName()).orElse("?");
    }

    private static String format(BankTransferEvent event) {
        String code = event.amount().currency();
        long units = event.amount().amount();
        return QRM.get(CurrencyRegistry.class).find(code).map(c -> c.format(units)).orElse(units + " " + code);
    }
}
