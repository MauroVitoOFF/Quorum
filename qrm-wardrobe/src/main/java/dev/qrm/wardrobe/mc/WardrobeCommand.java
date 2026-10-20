package dev.qrm.wardrobe.mc;

import com.mojang.brigadier.Command;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** {@code /qrm wardrobe reload}: rilegge la cartella delle divise. Solo operatori. */
final class WardrobeCommand {
    private WardrobeCommand() {}

    static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("qrm").then(Commands.literal("wardrobe")
                .requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("reload").executes(c -> {
                    int n = WardrobeServer.reload();
                    c.getSource().sendSuccess(() -> Component.literal(n + " divise caricate."), true);
                    return Command.SINGLE_SUCCESS;
                }))));
    }
}
