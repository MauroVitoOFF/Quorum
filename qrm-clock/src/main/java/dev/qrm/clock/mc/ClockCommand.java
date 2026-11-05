package dev.qrm.clock.mc;

import com.mojang.brigadier.Command;
import dev.qrm.api.clock.GameTime;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** {@code /qrm clock}: che ora, che giorno e che stagione e' nel gioco. */
final class ClockCommand {
    private ClockCommand() {}

    static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("qrm")
                .then(Commands.literal("clock").executes(c -> status(c.getSource()))));
    }

    private static int status(CommandSourceStack source) {
        GameTime t = ClockServer.now();
        Component time = Component.literal(String.format("%02d:%02d", t.hour(), t.minute()));
        Component date = Component.translatable("qrm_clock.date", t.date().getDayOfMonth(),
                Component.translatable("qrm_clock.month." + t.date().getMonthValue()), t.date().getYear());
        Component season = Component.translatable("qrm_clock.season." + t.season().name().toLowerCase(Locale.ROOT));
        source.sendSuccess(() -> Component.translatable("qrm_clock.status", time, date, season), false);
        return Command.SINGLE_SUCCESS;
    }
}
