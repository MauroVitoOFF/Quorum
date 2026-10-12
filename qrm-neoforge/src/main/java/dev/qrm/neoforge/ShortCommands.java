package dev.qrm.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * Scorciatoie ai comandi {@code /qrm ...}: una radice per argomento ({@code /job}, {@code /money}, ...) con i verbi come
 * sottocomandi. Sono gli stessi nodi di {@code /qrm <argomento>}: i due rami hanno sempre gli stessi sottocomandi. I sottocomandi per
 * operatori sono nascosti agli altri giocatori. Si spengono con {@code commands.shortAliases}.
 */
final class ShortCommands {
    private ShortCommands() {}

    static void register(CommandDispatcher<CommandSourceStack> d, boolean enabled) {
        if (!enabled) return;
        d.register(JobCommands.jobRoot());
        d.register(JobCommands.duty());
        d.register(QrmCommands.money());
        d.register(Commands.literal("balance").executes(QrmCommands::balance));
        d.register(Commands.literal("bal").executes(QrmCommands::balance));
        d.register(Commands.literal("pay").then(QrmCommands.playerAmountCurrency(QrmCommands::pay)));
        d.register(QrmCommands.characterNode("char"));
        d.register(OrgCommands.orgRoot());
        d.register(JobCommands.perm());
    }
}
