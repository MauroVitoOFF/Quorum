package dev.qrm.bank.server;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.qrm.api.QRM;
import dev.qrm.api.character.CharacterService;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.bank.logic.StaffBankLogic;
import dev.qrm.bank.mc.BankItems;
import dev.qrm.bank.mc.BankMod;
import dev.qrm.bank.mc.PlayerCash;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** {@code /qrm bank ...} e {@code /qrm admin audit bank}: amministrazione della banca, solo operatori. */
public final class BankCommands {
    private BankCommands() {}

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        // Sotto /qrm, come tutti i moduli: Brigadier fonde i nodi con lo stesso nome (qrm, admin, audit).
        event.getDispatcher().register(Commands.literal("qrm")
                .then(Commands.literal("bank")
                        .requires(BankCommands::isOp)
                        .then(Commands.literal("atm").then(Commands.literal("give")
                                .executes(c -> giveAtm(c.getSource(), c.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> giveAtm(c.getSource(), EntityArgument.getPlayer(c, "player"))))))
                        .then(Commands.literal("balance").then(name().executes(c -> balance(c.getSource(), nameOf(c)))))
                        .then(Commands.literal("freeze").then(name().executes(c -> setFrozen(c.getSource(), nameOf(c), true))))
                        .then(Commands.literal("unfreeze")
                                .then(name().executes(c -> setFrozen(c.getSource(), nameOf(c), false)))))
                // L'audit globale (/qrm admin audit) scorre i figli di questo nodo: la banca aggiunge il proprio.
                .then(Commands.literal("admin").requires(BankCommands::isOp)
                        .then(Commands.literal("audit")
                                .then(Commands.literal("bank").executes(c -> audit(c.getSource()))))));
    }

    private static boolean isOp(CommandSourceStack s) {
        return s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> name() {
        return Commands.argument("name", StringArgumentType.greedyString());
    }

    private static String nameOf(CommandContext<CommandSourceStack> c) {
        return StringArgumentType.getString(c, "name");
    }

    private static int giveAtm(CommandSourceStack source, ServerPlayer target) throws CommandSyntaxException {
        ItemStack stack = new ItemStack(BankItems.ATM_ITEM.get());
        if (!target.getInventory().add(stack)) target.drop(stack, false);
        source.sendSuccess(() -> Component.translatable("qrm_bank.cmd.atm_given", target.getName()), true);
        BankStaffLog.command(source, "atm_give", Optional.of(target.getUUID()),
                Optional.of(target.getGameProfile().name()), "");
        return 1;
    }

    private static Optional<PlayerCharacter> resolve(CommandSourceStack source, String name) {
        List<PlayerCharacter> found = QRM.get(CharacterService.class).findByName(name);
        if (found.size() != 1) {
            source.sendFailure(Component.translatable(
                    found.isEmpty() ? "qrm_bank.cmd.no_such_character" : "qrm_bank.cmd.ambiguous_character", name));
            return Optional.empty();
        }
        return Optional.of(found.get(0));
    }

    private static int balance(CommandSourceStack source, String name) {
        Optional<PlayerCharacter> pc = resolve(source, name);
        if (pc.isEmpty()) return 0;
        List<StaffBankLogic.Line> balances = BankMod.staffLogic().state(pc.get().id()).balances();
        String text = balances.isEmpty()
                ? "-" : balances.stream().map(StaffBankLogic.Line::text).collect(Collectors.joining(", "));
        String full = pc.get().firstName() + " " + pc.get().lastName();
        source.sendSuccess(() -> Component.translatable("qrm_bank.cmd.balance", full, text), false);
        return 1;
    }

    private static int setFrozen(CommandSourceStack source, String name, boolean frozen) {
        Optional<PlayerCharacter> pc = resolve(source, name);
        if (pc.isEmpty()) return 0;
        BankMod.staffLogic().setFrozen(pc.get().id(), frozen);
        String full = pc.get().firstName() + " " + pc.get().lastName();
        BankStaffLog.command(source, frozen ? "freeze" : "unfreeze", Optional.of(pc.get().playerId().value()),
                Optional.of(full), "");
        source.sendSuccess(() -> Component.translatable(
                frozen ? "qrm_bank.cmd.frozen" : "qrm_bank.cmd.unfrozen", full), true);
        return 1;
    }

    /** Coerenza del registro e confronto fra il contante dei giocatori online e il saldo della cassa. */
    private static int audit(CommandSourceStack source) {
        StaffBankLogic.AuditResult result = BankMod.staffLogic().audit(BankMod.denominations().currencies(),
                currency -> {
                    long held = 0;
                    for (ServerPlayer p : source.getServer().getPlayerList().getPlayers()) {
                        held += new PlayerCash(p).count(currency);
                    }
                    return held;
                });
        source.sendSuccess(() -> result.issues() == 0
                ? Component.translatable("qrm_bank.cmd.audit_ok")
                : Component.translatable("qrm_bank.cmd.audit_issues", result.issues()), false);
        for (StaffBankLogic.Excess e : result.excess()) {
            source.sendFailure(Component.translatable("qrm_bank.cmd.cash_excess", e.held(), e.currency(), e.vault()));
        }
        return result.clean() ? 1 : 0;
    }
}
