package dev.qrm.police.mc;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.qrm.police.logic.CallType;
import dev.qrm.police.logic.Citizen;
import dev.qrm.police.logic.PoliceSeed;
import java.util.Locale;
import java.util.Random;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** {@code /quorum mdt ...}: dati e chiamate di prova, per gli operatori (e in singleplayer per chiunque). */
final class PoliceCommand {
    private static final String[][] SAMPLES = {
            {"MEDICAL", "Uomo a terra incosciente"},
            {"POLICE", "Rapina in corso"},
            {"SHOOTING", "Segnalazione colpi d'arma da fuoco"}};

    private PoliceCommand() {}

    private static boolean allowed(CommandSourceStack s) {
        return s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER) || s.getServer().isSingleplayer();
    }

    static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("quorum").then(Commands.literal("mdt")
                .requires(PoliceCommand::allowed)
                .then(Commands.literal("seed").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Citizen self = PoliceServer.selfCitizen(p);
                    PoliceSeed.populate(PoliceServer.db(), self.id, self.firstName, self.lastName,
                            System.currentTimeMillis(), new Random());
                    PoliceServer.changed(c.getSource().getServer());
                    int n = PoliceServer.db().citizens().size();
                    c.getSource().sendSuccess(() -> Component.translatable("qrm_police.cmd.seed", n), true);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("clear").executes(c -> {
                    PoliceServer.db().clear();
                    PoliceServer.changed(c.getSource().getServer());
                    c.getSource().sendSuccess(() -> Component.translatable("qrm_police.cmd.clear"), true);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("simulatecall")
                        .executes(c -> simulate(c.getSource(), null))
                        .then(Commands.argument("tipo", StringArgumentType.word())
                                .suggests((ctx, b) -> {
                                    for (String[] s : SAMPLES) b.suggest(s[0].toLowerCase(Locale.ROOT));
                                    return b.buildFuture();
                                })
                                .executes(c -> simulate(c.getSource(), StringArgumentType.getString(c, "tipo")))))));
    }

    private static int simulate(CommandSourceStack src, String kind) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        String[] sample = SAMPLES[new Random().nextInt(SAMPLES.length)];
        if (kind != null) {
            CallType wanted = CallType.parse(kind);
            for (String[] s : SAMPLES) if (s[0].equals(wanted.name())) sample = s;
        }
        BlockPos pos = PoliceServer.randomNear(p, 100, 200);
        PoliceServer.raiseCall(src.getServer(), CallType.parse(sample[0]), sample[1],
                p.level().dimension().identifier().toString(), pos.getX(), p.blockPosition().getY(), pos.getZ(),
                "Simulazione");
        String desc = sample[1];
        src.sendSuccess(() -> Component.translatable("qrm_police.cmd.simulated", desc, pos.getX(), pos.getZ()), true);
        return Command.SINGLE_SUCCESS;
    }
}
