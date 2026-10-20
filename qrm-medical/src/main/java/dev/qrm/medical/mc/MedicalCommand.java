package dev.qrm.medical.mc;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.qrm.medical.logic.MedicalState;
import dev.qrm.medical.logic.MedicalStorage;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** {@code /qrm medical ...} per gli operatori; {@code /carry} (e {@code /qrm carry}) per tutti. */
final class MedicalCommand {
    private MedicalCommand() {}

    private static boolean isOp(CommandSourceStack s) {
        return s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    static void onRegisterCommands(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("qrm").then(Commands.literal("medical")
                .requires(MedicalCommand::isOp)
                .then(Commands.literal("down").then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> {
                            ServerPlayer p = EntityArgument.getPlayer(c, "player");
                            MedicalServer.adminDown(p);
                            MedicalStaffLog.command(c.getSource(), "down", p);
                            c.getSource().sendSuccess(() -> Component.translatable("qrm_medical.cmd.down", p.getDisplayName()), true);
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(Commands.literal("revive").then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> {
                            ServerPlayer p = EntityArgument.getPlayer(c, "player");
                            MedicalServer.adminRevive(p);
                            MedicalStaffLog.command(c.getSource(), "revive", p);
                            c.getSource().sendSuccess(() -> Component.translatable("qrm_medical.cmd.revive", p.getDisplayName()), true);
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(Commands.literal("inspect").then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> {
                            ServerPlayer p = EntityArgument.getPlayer(c, "player");
                            var profile = MedicalServer.profileOf(p);
                            c.getSource().sendSuccess(() -> Component.translatable("qrm_medical.cmd.inspect",
                                    p.getDisplayName(), Component.translatable("qrm_medical.inspect.state."
                                            + profile.state().name().toLowerCase(java.util.Locale.ROOT))), false);
                            MedicalServer.showInjuries(m -> c.getSource().sendSuccess(() -> m, false), profile);
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(Commands.literal("sethospital").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    MedicalServer.storage().setHospital(new MedicalStorage.Hospital(
                            p.level().dimension().identifier().toString(), p.getX(), p.getY(), p.getZ(), p.getYRot()));
                    c.getSource().sendSuccess(() -> Component.translatable("qrm_medical.cmd.sethospital",
                            String.format("%.1f %.1f %.1f", p.getX(), p.getY(), p.getZ())), true);
                    return Command.SINGLE_SUCCESS;
                }))));
        dispatcher.register(Commands.literal("carry").executes(MedicalCommand::carry));
        dispatcher.register(Commands.literal("qrm").then(Commands.literal("carry").executes(MedicalCommand::carry)));
    }

    private static int carry(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        if (MedicalServer.stateOf(p) != MedicalState.HEALTHY) return 0;
        boolean started = MedicalServer.toggleCarry(p);
        p.sendSystemMessage(Component.translatable(started ? "qrm_medical.carry.start" : "qrm_medical.carry.stop"), true);
        return Command.SINGLE_SUCCESS;
    }
}
