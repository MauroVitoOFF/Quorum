package dev.qrm.vehicles.mc;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.qrm.vehicles.logic.VehicleDef;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import java.util.Optional;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** {@code /qrm vehicle spawn <id>}: crea un veicolo davanti a chi digita, per gli operatori. */
final class VehicleCommand {

    private VehicleCommand() {}

    private static boolean allowed(CommandSourceStack s) {
        return s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER) || s.getServer().isSingleplayer();
    }

    static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("qrm").then(Commands.literal("vehicle")
                .requires(VehicleCommand::allowed)
                .then(Commands.literal("spawn").then(Commands.argument("id", StringArgumentType.word())
                        .suggests((ctx, b) -> {
                            VehicleDef.ALL.forEach(d -> b.suggest(d.id()));
                            return b.buildFuture();
                        })
                        .executes(c -> spawn(c.getSource(), StringArgumentType.getString(c, "id")))))));
    }

    private static int spawn(CommandSourceStack src, String id) throws CommandSyntaxException {
        Optional<EntityType<QrmVehicleEntity>> type = VehicleEntities.typeOf(id);
        if (type.isEmpty()) {
            src.sendFailure(Component.translatable("qrm_vehicles.cmd.unknown", id));
            return 0;
        }
        ServerPlayer player = src.getPlayerOrException();
        ServerLevel level = src.getLevel();
        QrmVehicleEntity vehicle = type.get().create(level, EntitySpawnReason.COMMAND);
        if (vehicle == null) {
            src.sendFailure(Component.translatable("qrm_vehicles.cmd.failed"));
            return 0;
        }
        Vec3 look = player.getLookAngle();
        Vec3 ahead = new Vec3(look.x, 0, look.z).normalize().scale(4);
        vehicle.setPos(player.getX() + ahead.x, player.getY(), player.getZ() + ahead.z);
        vehicle.setYRot(player.getYRot());
        level.addFreshEntity(vehicle);
        src.sendSuccess(() -> Component.translatable("qrm_vehicles.cmd.spawned"), true);
        return Command.SINGLE_SUCCESS;
    }
}
