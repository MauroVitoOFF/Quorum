package dev.qrm.arrival.mc;

import com.mojang.brigadier.Command;
import dev.qrm.arrival.logic.ArrivalPoint;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** {@code /qrm arrival setspawn} e {@code /qrm arrival reload}. Solo operatori. */
final class ArrivalCommand {
    private ArrivalCommand() {}

    static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("qrm").then(Commands.literal("arrival")
                .requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("setspawn").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    ArrivalServer.files().saveSpawn(new ArrivalPoint(p.level().dimension().identifier().toString(),
                            p.getX(), p.getY(), p.getZ(), p.getYRot()));
                    c.getSource().sendSuccess(() -> Component.translatableWithFallback(
                            "qrm_arrival.command.spawn_set", "Punto di arrivo impostato."), true);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("reload").executes(c -> {
                    ArrivalServer.reload();
                    c.getSource().sendSuccess(() -> Component.translatableWithFallback(
                            "qrm_arrival.command.reloaded", "Impostazioni dell'arrivo ricaricate."), true);
                    return Command.SINGLE_SUCCESS;
                }))));
    }
}
