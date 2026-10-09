package dev.qrm.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * Scorciatoie ai comandi {@code /qrm ...}: una radice per argomento ({@code /job}, {@code /money}, ...) con i verbi come
 * sottocomandi. Riusano gli stessi nodi ed esecutori dei comandi {@code /qrm}, che restano invariati; i sottocomandi per
 * operatori sono nascosti agli altri giocatori. Si spengono con {@code commands.shortAliases}.
 */
final class ShortCommands {
    private ShortCommands() {}

    static void register(CommandDispatcher<CommandSourceStack> d, boolean enabled) {
        if (!enabled) return;
        d.register(JobCommands.jobRoot());
        d.register(JobCommands.duty());
        d.register(money());
        d.register(Commands.literal("balance").executes(QrmCommands::balance));
        d.register(Commands.literal("bal").executes(QrmCommands::balance));
        d.register(Commands.literal("pay").then(QrmCommands.playerAmountCurrency(QrmCommands::pay)));
        d.register(QrmCommands.characterNode("char").executes(QrmCommands::characterList));
        d.register(OrgCommands.orgRoot());
        d.register(JobCommands.adminPerm().requires(QrmCommands::isOp));
    }

    /** {@code /money}: i propri saldi; {@code pay} per tutti, {@code give} e {@code take} per gli operatori. */
    private static LiteralArgumentBuilder<CommandSourceStack> money() {
        return Commands.literal("money")
                .executes(QrmCommands::balance)
                .then(Commands.literal("pay").then(QrmCommands.playerAmountCurrency(QrmCommands::pay)))
                .then(Commands.literal("give").requires(QrmCommands::isOp)
                        .then(QrmCommands.playerAmountCurrency(QrmCommands::grant)))
                .then(Commands.literal("take").requires(QrmCommands::isOp)
                        .then(QrmCommands.playerAmountCurrency(QrmCommands::take)));
    }
}
