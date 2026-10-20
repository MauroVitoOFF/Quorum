package dev.qrm.immersion.mc;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.qrm.immersion.logic.BodyPose;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /qrm dummy ...} per gli operatori: un manichino di prova che accetta ogni presentazione e assume le pose, per
 * provare il modulo da soli. Il manichino e' quello di Minecraft; {@code pose}, {@code remove} e {@code clear} agiscono solo
 * su quelli creati da {@code spawn} (portano l'etichetta {@value #DUMMY_TAG}).
 */
final class ImmersionCommand {
    private static final double NEAREST_RANGE = 16.0;
    private static final int MAX_CLEAR_RADIUS = 64;
    /** Etichetta dei manichini creati da {@code dummy spawn} (resta salvata con l'entita'). */
    static final String DUMMY_TAG = "qrm_dummy";

    private ImmersionCommand() {}

    private static boolean isOp(CommandSourceStack s) {
        return s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> pose = Commands.literal("pose");
        pose.then(Commands.literal("hands_up").executes(c -> setPose(c.getSource(), BodyPose.HANDS_UP)));
        pose.then(Commands.literal("point").executes(c -> setPose(c.getSource(), BodyPose.POINTING)));
        pose.then(Commands.literal("none").executes(c -> setPose(c.getSource(), BodyPose.NONE)));

        var dummy = Commands.literal("dummy")
                .requires(ImmersionCommand::isOp)
                .then(Commands.literal("spawn")
                        .executes(c -> spawn(c.getSource(), null))
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(c -> spawn(c.getSource(), StringArgumentType.getString(c, "name")))))
                .then(pose)
                .then(Commands.literal("remove").executes(c -> remove(c.getSource())))
                .then(Commands.literal("clear")
                        .executes(c -> clear(c.getSource(), (int) NEAREST_RANGE))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, MAX_CLEAR_RADIUS))
                                .executes(c -> clear(c.getSource(), IntegerArgumentType.getInteger(c, "radius")))));
        event.getDispatcher().register(Commands.literal("qrm").then(dummy));
    }

    /** I manichini creati da {@code dummy spawn} portano questa etichetta: gli altri comandi toccano solo quelli. */
    static boolean isDummy(Mannequin m) {
        return m.entityTags().contains(DUMMY_TAG);
    }

    private static java.util.List<Mannequin> dummiesNear(CommandSourceStack source, double radius) {
        Vec3 at = source.getPosition();
        return source.getLevel().getEntitiesOfClass(Mannequin.class, new AABB(at, at).inflate(radius),
                ImmersionCommand::isDummy);
    }

    private static int spawn(CommandSourceStack source, String name) {
        ServerLevel level = source.getLevel();
        Mannequin dummy = Mannequin.create(net.minecraft.world.entity.EntityTypes.MANNEQUIN, level);
        if (dummy == null) return 0;
        Vec3 at = source.getPosition();
        dummy.snapTo(at.x, at.y, at.z, source.getRotation().y + 180.0F, 0.0F);
        dummy.setCustomName(Component.literal(name == null || name.isBlank() ? "Mario Rossi" : name));
        dummy.setCustomNameVisible(false);
        dummy.addTag(DUMMY_TAG);
        level.addFreshEntity(dummy);
        source.sendSuccess(() -> Component.translatable("qrm_immersion.cmd.dummy.spawned", dummy.getDisplayName()), true);
        return Command.SINGLE_SUCCESS;
    }

    private static Mannequin nearest(CommandSourceStack source) {
        Vec3 at = source.getPosition();
        return dummiesNear(source, NEAREST_RANGE).stream()
                .min(java.util.Comparator.comparingDouble(m -> m.distanceToSqr(at)))
                .orElse(null);
    }

    private static int setPose(CommandSourceStack source, BodyPose pose) {
        Mannequin nearest = nearest(source);
        if (nearest == null) {
            source.sendFailure(Component.translatable("qrm_immersion.cmd.dummy.none_near"));
            return 0;
        }
        ImmersionServer.setPose(nearest, pose);
        source.sendSuccess(() -> Component.translatable("qrm_immersion.cmd.dummy.pose",
                nearest.getDisplayName(), Component.translatable("qrm_immersion.pose." + pose.name().toLowerCase(java.util.Locale.ROOT))), true);
        return Command.SINGLE_SUCCESS;
    }

    /** Toglie il manichino di prova piu' vicino (entro {@value #NEAREST_RANGE} blocchi). */
    private static int remove(CommandSourceStack source) {
        Mannequin nearest = nearest(source);
        if (nearest == null) {
            source.sendFailure(Component.translatable("qrm_immersion.cmd.dummy.none_near"));
            return 0;
        }
        net.minecraft.network.chat.Component name = nearest.getDisplayName();
        nearest.discard();
        source.sendSuccess(() -> Component.translatable("qrm_immersion.cmd.dummy.removed", name), true);
        return Command.SINGLE_SUCCESS;
    }

    /** Toglie tutti i manichini di prova entro il raggio dato. */
    private static int clear(CommandSourceStack source, int radius) {
        var found = dummiesNear(source, radius);
        if (found.isEmpty()) {
            source.sendFailure(Component.translatable("qrm_immersion.cmd.dummy.none_in_radius", radius));
            return 0;
        }
        found.forEach(Mannequin::discard);
        int n = found.size();
        source.sendSuccess(() -> Component.translatable("qrm_immersion.cmd.dummy.cleared", n, radius), true);
        return n;
    }
}
