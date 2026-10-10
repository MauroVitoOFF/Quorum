package dev.qrm.neoforge;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.qrm.api.QRM;
import dev.qrm.api.point.PointFace;
import dev.qrm.api.point.PointPosition;
import dev.qrm.api.point.PointService;
import dev.qrm.api.point.PointType;
import dev.qrm.api.point.PointTypeRegistry;
import dev.qrm.neoforge.server.PointCommandLogic;
import dev.qrm.neoforge.server.PointWorld;
import dev.qrm.neoforge.server.StaffAudit;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** {@code /qrm admin point ...}: già riservato agli operatori dal ramo admin. La logica sta in {@link PointCommandLogic}. */
final class PointCommands {
    private static final double REACH = 6.0;
    private static final SuggestionProvider<CommandSourceStack> TYPES = (context, builder) -> {
        List<String> ids;
        try {
            ids = QRM.get(PointTypeRegistry.class).all().stream().map(PointType::id).sorted().toList();
        } catch (RuntimeException e) {
            ids = List.of();
        }
        return SharedSuggestionProvider.suggest(ids, builder);
    };

    private PointCommands() {}

    static LiteralArgumentBuilder<CommandSourceStack> adminPoint() {
        return Commands.literal("point")
                .then(Commands.literal("create")
                        .then(Commands.argument("type", StringArgumentType.word()).suggests(TYPES)
                                .then(Commands.literal("job")
                                        .then(grade(Commands.argument("job", StringArgumentType.word())
                                                .suggests(CommandSuggestions.JOBS), "job", "job",
                                                CommandSuggestions.GRADES)))
                                .then(Commands.literal("org")
                                        .then(grade(Commands.argument("org", StringArgumentType.word())
                                                .suggests(CommandSuggestions.ORGS), "org", "org",
                                                CommandSuggestions.RANKS)))))
                .then(Commands.literal("set")
                        .then(Commands.literal("grade").then(Commands.argument("grade", StringArgumentType.word())
                                .executes(StaffAudit.audited("cmd.point.set", c -> {
                                    PointPosition t = target(c);
                                    return t == null ? noTarget(c)
                                            : reply(c, logic().setGrade(t, StringArgumentType.getString(c, "grade")));
                                }))))
                        .then(Commands.literal("face").then(Commands.argument("face", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                        List.of("all", "here", "north", "south", "east", "west"), b))
                                .executes(StaffAudit.audited("cmd.point.set", c -> {
                                    PointPosition t = target(c);
                                    return t == null ? noTarget(c) : reply(c, logic().setFace(t,
                                            StringArgumentType.getString(c, "face"), lookedFace(c)));
                                }))))
                        .then(Commands.literal("public").then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(StaffAudit.audited("cmd.point.set", c -> {
                                    PointPosition t = target(c);
                                    return t == null ? noTarget(c)
                                            : reply(c, logic().setPublic(t, BoolArgumentType.getBool(c, "value")));
                                }))))
                        .then(Commands.literal("name").then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(StaffAudit.audited("cmd.point.set", c -> {
                                    PointPosition t = target(c);
                                    return t == null ? noTarget(c)
                                            : reply(c, logic().setName(t, StringArgumentType.getString(c, "name")));
                                })))))
                .then(Commands.literal("move").then(Commands.argument("id", IntegerArgumentType.integer(1))
                        .executes(StaffAudit.audited("cmd.point.move", c -> {
                            PointPosition t = target(c);
                            return t == null ? noTarget(c)
                                    : reply(c, logic().move(IntegerArgumentType.getInteger(c, "id"), t));
                        }))))
                .then(Commands.literal("remove")
                        .executes(StaffAudit.audited("cmd.point.remove", c -> {
                            PointPosition t = target(c);
                            return t == null ? noTarget(c) : reply(c, logic().remove(null, t));
                        }))
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .executes(StaffAudit.audited("cmd.point.remove",
                                        c -> reply(c, logic().remove(IntegerArgumentType.getInteger(c, "id"), null))))))
                .then(Commands.literal("list")
                        .executes(c -> lines(c, logic().list(null, null)))
                        .then(Commands.literal("job").then(Commands.argument("job", StringArgumentType.word())
                                .suggests(CommandSuggestions.JOBS)
                                .executes(c -> lines(c, logic().list("job", StringArgumentType.getString(c, "job"))))))
                        .then(Commands.literal("org").then(Commands.argument("org", StringArgumentType.word())
                                .suggests(CommandSuggestions.ORGS)
                                .executes(c -> lines(c, logic().list("org", StringArgumentType.getString(c, "org")))))))
                .then(Commands.literal("info").executes(c -> {
                    PointPosition t = target(c);
                    return t == null ? noTarget(c) : lines(c, logic().info(t));
                }));
    }

    /** {@code <proprietario> [grado]} con il completamento del grado/rank in base al proprietario già scritto. */
    private static RequiredArgumentBuilder<CommandSourceStack, String> grade(
            RequiredArgumentBuilder<CommandSourceStack, String> owner, String ownerArg, String kind,
            SuggestionProvider<CommandSourceStack> gradeSuggestions) {
        return owner
                .executes(StaffAudit.audited("cmd.point.create", c -> create(c, kind, ownerArg, null)))
                .then(Commands.argument("grade", StringArgumentType.word()).suggests(gradeSuggestions)
                        .executes(StaffAudit.audited("cmd.point.create",
                                c -> create(c, kind, ownerArg, StringArgumentType.getString(c, "grade")))));
    }

    private static int create(CommandContext<CommandSourceStack> c, String kind, String ownerArg, String grade)
            throws CommandSyntaxException {
        PointPosition t = target(c);
        if (t == null) return noTarget(c);
        return reply(c, logic().create(StringArgumentType.getString(c, "type"), kind,
                StringArgumentType.getString(c, ownerArg), grade, t, lookedFace(c)));
    }

    private static PointCommandLogic logic() {
        return new PointCommandLogic(QRM.get(PointService.class), PointWorld::inactiveReason);
    }

    /** Il blocco inquadrato dal giocatore, o {@code null} se non ne inquadra uno. */
    private static PointPosition target(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer player = c.getSource().getPlayerOrException();
        HitResult hit = player.pick(REACH, 1.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK) return null;
        return PointWorld.positionOf(player.level(), ((BlockHitResult) hit).getBlockPos());
    }

    /** La faccia laterale del blocco inquadrato, o {@code null} se si guarda sopra/sotto o nessun blocco. */
    private static PointFace lookedFace(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer player = c.getSource().getPlayerOrException();
        HitResult hit = player.pick(REACH, 1.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK) return null;
        return switch (((BlockHitResult) hit).getDirection()) {
            case NORTH -> PointFace.NORTH;
            case SOUTH -> PointFace.SOUTH;
            case EAST -> PointFace.EAST;
            case WEST -> PointFace.WEST;
            default -> null;
        };
    }

    private static int noTarget(CommandContext<CommandSourceStack> c) {
        return QrmCommands.fail(c, "Inquadra un blocco (entro " + (int) REACH + " blocchi).");
    }

    private static int reply(CommandContext<CommandSourceStack> c, PointCommandLogic.Reply reply) {
        return reply.ok() ? QrmCommands.ok(c, reply.text()) : QrmCommands.fail(c, reply.text());
    }

    private static int lines(CommandContext<CommandSourceStack> c, List<String> lines) {
        lines.forEach(l -> QrmCommands.ok(c, l));
        return 1;
    }
}
