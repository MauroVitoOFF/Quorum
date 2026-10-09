package dev.qrm.neoforge;

import dev.qrm.neoforge.server.StaffAudit;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.qrm.api.QRM;
import dev.qrm.api.staff.StaffAccessService;
import dev.qrm.neoforge.server.StaffAccess;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

/** {@code /qrm admin staff grant|revoke|list}: permessi del pannello staff per account (solo operatori). */
final class StaffCommands {
    private StaffCommands() {}

    static LiteralArgumentBuilder<CommandSourceStack> adminStaff() {
        return Commands.literal("staff")
                .then(Commands.literal("grant").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("node", StringArgumentType.greedyString()).executes(StaffAudit.audited("cmd.staff.grant", c -> change(c, true))))))
                .then(Commands.literal("revoke").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("node", StringArgumentType.greedyString()).executes(StaffAudit.audited("cmd.staff.revoke", c -> change(c, false))))))
                .then(Commands.literal("list").then(Commands.argument("player", EntityArgument.player())
                        .executes(StaffCommands::list)));
    }

    private static int change(CommandContext<CommandSourceStack> c, boolean grant) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(c, "player");
        String node = StringArgumentType.getString(c, "node").strip();
        StaffAccessService service = QRM.get(StaffAccessService.class);
        try {
            boolean changed = grant ? service.grant(target.getUUID(), node) : service.revoke(target.getUUID(), node);
            if (!changed) return QrmCommands.fail(c, grant ? "Aveva già questo permesso." : "Non aveva questo permesso.");
        } catch (IllegalArgumentException e) {
            return QrmCommands.fail(c, "Nodo non valido: servono pattern 'staff.…' (es. staff.jobs, staff.*) oppure *.");
        }
        StaffAccess.sync(target);
        return QrmCommands.ok(c, (grant ? "Concesso " : "Tolto ") + node + " a " + target.getGameProfile().name() + ".");
    }

    private static int list(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(c, "player");
        Set<String> grants = QRM.get(StaffAccessService.class).grantsOf(target.getUUID());
        String text = grants.isEmpty() ? "nessuno" : String.join(", ", grants);
        return QrmCommands.ok(c, target.getGameProfile().name() + " - permessi staff: " + text
                + (StaffAccess.isOp(target) ? " (operatore: ha tutto)" : ""));
    }
}
