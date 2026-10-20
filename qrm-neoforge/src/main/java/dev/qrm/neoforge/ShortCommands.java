package dev.qrm.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * Scorciatoie ai comandi {@code /qrm ...} usati dai giocatori ({@code /job}, {@code /money}, {@code /char}, ...). Sono gli
 * stessi nodi di {@code /qrm <argomento>}; i sottocomandi per operatori sono nascosti agli altri giocatori. {@code /bal} e
 * {@code /pay} sono le forme rapide di {@code /money} e {@code /money pay}. I comandi solo-operatore ({@code perm},
 * {@code bank}, ...) vivono soltanto sotto {@code /qrm}. Si spengono con {@code commands.shortAliases}.
 */
final class ShortCommands {
    private ShortCommands() {}

    static void register(CommandDispatcher<CommandSourceStack> d, boolean enabled) {
        if (!enabled) return;
        d.register(JobCommands.jobRoot());
        d.register(JobCommands.duty());
        d.register(QrmCommands.money());
        d.register(Commands.literal("bal").executes(QrmCommands::balance));
        d.register(Commands.literal("pay").then(QrmCommands.playerAmountCurrency(QrmCommands::pay)));
        d.register(QrmCommands.characterNode("char"));
        d.register(OrgCommands.orgRoot());
    }
}
