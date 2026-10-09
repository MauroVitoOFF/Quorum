package dev.qrm.neoforge.server;

import dev.qrm.api.QRM;
import dev.qrm.api.character.CharacterService;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.id.PlayerId;
import dev.qrm.api.job.GradeDefinition;
import dev.qrm.api.job.JobDefinition;
import dev.qrm.api.job.JobRegistry;
import dev.qrm.api.job.JobService;
import dev.qrm.api.org.OrganizationService;
import dev.qrm.api.org.Rank;
import dev.qrm.api.moderation.ModerationService;
import dev.qrm.api.moderation.Mute;
import dev.qrm.api.permission.PermissionEffect;
import dev.qrm.api.permission.PermissionService;
import dev.qrm.api.staff.StaffLogEntry;
import dev.qrm.api.staff.StaffLogService;
import dev.qrm.neoforge.net.PlayerAction;
import dev.qrm.neoforge.net.PlayerResult;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** Registra le esecuzioni delle azioni della scheda giocatore. Ogni azione compare qui quando viene implementata. */
final class PlayerActionHandlers {
    /** Giocatori congelati dallo staff (si azzera al logout). */
    static final FreezeRegistry<ServerLevel> FROZEN = new FreezeRegistry<>();

    /** Lunghezza massima di una nota dello staff. */
    static final int NOTE_MAX = 200;

    private PlayerActionHandlers() {}

    static void install(Map<PlayerAction, PlayerActionServer.Handler> handlers) {
        handlers.put(PlayerAction.HEAL, (staff, target, arg) -> heal(target));
        handlers.put(PlayerAction.TELEPORT_TO, (staff, target, arg) -> teleport(staff, target));
        handlers.put(PlayerAction.BRING, (staff, target, arg) -> teleport(target, staff));
        handlers.put(PlayerAction.SET_GAMEMODE, (staff, target, arg) -> gameMode(target, arg));
        handlers.put(PlayerAction.FREEZE, (staff, target, arg) -> freeze(target));
        handlers.put(PlayerAction.GIVE_ITEM, (staff, target, arg) -> giveItem(target, arg));
        handlers.put(PlayerAction.KICK, PlayerActionHandlers::kick);
        handlers.put(PlayerAction.BAN, PlayerActionHandlers::ban);
        handlers.put(PlayerAction.VIEW_CHARACTER, (staff, target, arg) -> viewCharacter(target));
        handlers.put(PlayerAction.VIEW_PERMISSIONS, (staff, target, arg) -> viewPermissions(target));
        handlers.put(PlayerAction.HISTORY, (staff, target, arg) -> history(target));
        handlers.put(PlayerAction.MUTE, PlayerActionHandlers::mute);
        handlers.put(PlayerAction.UNMUTE, (staff, target, arg) -> unmute(target));
        handlers.put(PlayerAction.WARN, PlayerActionHandlers::warn);
        handlers.put(PlayerAction.NOTE, PlayerActionHandlers::note);
        handlers.put(PlayerAction.WARNINGS, (staff, target, arg) -> warnings(target));
        handlers.put(PlayerAction.NOTES, (staff, target, arg) -> notes(target));
    }

    private static PlayerActionServer.Outcome viewCharacter(ServerPlayer target) {
        Optional<PlayerCharacter> active = QRM.get(CharacterService.class).activeOf(new PlayerId(target.getUUID()));
        if (active.isEmpty()) return PlayerActionServer.Outcome.of(PlayerResult.NO_CHARACTER);
        PlayerCharacter c = active.get();
        JobRegistry registry = QRM.get(JobRegistry.class);
        Optional<InfoLines.JobView> job = QRM.get(JobService.class).assignmentOf(c.id()).map(a -> {
            Optional<JobDefinition> def = registry.find(a.jobId());
            String jobLabel = def.map(JobDefinition::label).orElse(a.jobId());
            String gradeLabel = def.flatMap(d -> d.grade(a.gradeId())).map(GradeDefinition::label).orElse(a.gradeId());
            return new InfoLines.JobView(jobLabel, gradeLabel, a.onDuty());
        });
        OrganizationService orgs = QRM.get(OrganizationService.class);
        List<InfoLines.OrgView> views = orgs.organizationsOf(c.id()).stream().map(o -> {
            Optional<String> rank = orgs.membersOf(o.id()).stream().filter(m -> m.character().equals(c.id())).findFirst()
                    .flatMap(m -> orgs.ranksOf(o.id()).stream().filter(r -> r.id().equals(m.rankId())).findFirst()
                            .map(Rank::label));
            return new InfoLines.OrgView(o.label(), rank);
        }).toList();
        return new PlayerActionServer.Outcome(PlayerResult.OK,
                InfoLines.character(c.firstName(), c.lastName(), c.birthDate(), c.status().name(), job, views));
    }

    /** Campi: durata (vuota = permanente, es. {@code 7d}) e motivo (facoltativo). */
    private static PlayerActionServer.Outcome mute(ServerPlayer staff, ServerPlayer target, String arg) {
        String[] f = Reasons.fields(arg, 2);
        Duration duration = BanDuration.parse(f[0]).orElse(null);
        if (duration == null) return PlayerActionServer.Outcome.of(PlayerResult.INVALID);
        String reason = Reasons.clean(f[1], "nessun motivo indicato");
        ModerationService moderation = QRM.get(ModerationService.class);
        moderation.mute(target.getUUID(), target.getGameProfile().name(), Optional.of(staff.getUUID()),
                staff.getGameProfile().name(), reason, duration.isZero() ? Optional.empty() : Optional.of(duration));
        Mute mute = moderation.activeMute(target.getUUID()).orElseThrow();
        target.sendSystemMessage(Component.literal(MuteNotice.applied(mute, ZoneId.systemDefault())));
        return PlayerActionServer.Outcome.of(PlayerResult.OK);
    }

    private static PlayerActionServer.Outcome unmute(ServerPlayer target) {
        boolean was = QRM.get(ModerationService.class).unmute(target.getUUID());
        if (!was) return PlayerActionServer.Outcome.of(PlayerResult.NOT_MUTED);
        target.sendSystemMessage(Component.literal(MuteNotice.lifted()));
        return PlayerActionServer.Outcome.of(PlayerResult.OK);
    }

    /** Campo: motivo (obbligatorio). */
    private static PlayerActionServer.Outcome warn(ServerPlayer staff, ServerPlayer target, String arg) {
        String reason = Reasons.clean(Reasons.fields(arg, 1)[0], "");
        if (reason.isEmpty()) return PlayerActionServer.Outcome.of(PlayerResult.INVALID);
        int count = QRM.get(ModerationService.class).warn(target.getUUID(), target.getGameProfile().name(),
                Optional.of(staff.getUUID()), staff.getGameProfile().name(), reason);
        target.sendSystemMessage(Component.literal(MuteNotice.warned(count, reason)));
        return PlayerActionServer.Outcome.of(PlayerResult.OK);
    }

    /** Campo: testo della nota (obbligatorio, al massimo 200 caratteri). */
    private static PlayerActionServer.Outcome note(ServerPlayer staff, ServerPlayer target, String arg) {
        String text = Reasons.clean(Reasons.fields(arg, 1)[0], "", NOTE_MAX);
        if (text.isEmpty()) return PlayerActionServer.Outcome.of(PlayerResult.INVALID);
        QRM.get(ModerationService.class).addNote(target.getUUID(), target.getGameProfile().name(),
                Optional.of(staff.getUUID()), staff.getGameProfile().name(), text);
        return PlayerActionServer.Outcome.of(PlayerResult.OK);
    }

    private static PlayerActionServer.Outcome warnings(ServerPlayer target) {
        return new PlayerActionServer.Outcome(PlayerResult.OK, ModerationLines.warningsOf(
                QRM.get(ModerationService.class).warningsOf(target.getUUID(), PageServer.PAGE), ZoneId.systemDefault()));
    }

    private static PlayerActionServer.Outcome notes(ServerPlayer target) {
        return new PlayerActionServer.Outcome(PlayerResult.OK, ModerationLines.notes(
                QRM.get(ModerationService.class).notesOf(target.getUUID(), PageServer.PAGE), ZoneId.systemDefault()));
    }

    private static PlayerActionServer.Outcome history(ServerPlayer target) {
        List<StaffLogEntry> entries = QRM.get(StaffLogService.class).about(target.getUUID(), PageServer.PAGE);
        return new PlayerActionServer.Outcome(PlayerResult.OK, LogLines.build(entries, ZoneId.systemDefault()));
    }

    private static PlayerActionServer.Outcome viewPermissions(ServerPlayer target) {
        Optional<PlayerCharacter> active = QRM.get(CharacterService.class).activeOf(new PlayerId(target.getUUID()));
        if (active.isEmpty()) return PlayerActionServer.Outcome.of(PlayerResult.NO_CHARACTER);
        PermissionService perms = QRM.get(PermissionService.class);
        Map<String, Boolean> overrides = new HashMap<>();
        perms.overridesOf(active.get().id()).forEach((node, effect) -> overrides.put(node, effect == PermissionEffect.GRANT));
        return new PlayerActionServer.Outcome(PlayerResult.OK,
                InfoLines.permissions(perms.effective(active.get().id()), overrides));
    }

    private static PlayerActionServer.Outcome giveItem(ServerPlayer target, String arg) {
        ItemSpec spec = ItemSpec.parse(Reasons.fields(arg, 1)[0]).orElse(null);
        if (spec == null) return PlayerActionServer.Outcome.of(PlayerResult.INVALID);
        Identifier id = Identifier.tryBuild(spec.namespace(), spec.path());
        Item item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item == null || item == Items.AIR) return PlayerActionServer.Outcome.of(PlayerResult.UNKNOWN_ITEM);
        int remaining = spec.amount();
        while (remaining > 0) {
            int n = Math.min(remaining, new ItemStack(item).getMaxStackSize());
            ItemStack stack = new ItemStack(item, n);
            if (!target.getInventory().add(stack)) target.drop(stack, false);
            remaining -= n;
        }
        return PlayerActionServer.Outcome.of(PlayerResult.OK);
    }

    private static PlayerActionServer.Outcome kick(ServerPlayer staff, ServerPlayer target, String arg) {
        String reason = Reasons.clean(Reasons.fields(arg, 1)[0], "nessun motivo indicato");
        target.connection.disconnect(Component.literal("Sei stato espulso dallo staff.\nMotivo: " + reason));
        return PlayerActionServer.Outcome.of(PlayerResult.OK);
    }

    /** Campi: durata (vuota = permanente, es. {@code 7d}) e motivo. Usa la lista ban di Minecraft. */
    private static PlayerActionServer.Outcome ban(ServerPlayer staff, ServerPlayer target, String arg) {
        String[] f = Reasons.fields(arg, 2);
        Duration duration = BanDuration.parse(f[0]).orElse(null);
        if (duration == null) return PlayerActionServer.Outcome.of(PlayerResult.INVALID);
        String reason = Reasons.clean(f[1], "nessun motivo indicato");
        Date now = new Date();
        Date expires = duration.isZero() ? null : Date.from(Instant.now().plus(duration));
        target.level().getServer().getPlayerList().getBans().add(new UserBanListEntry(
                new NameAndId(target.getGameProfile()), now, staff.getGameProfile().name(), expires, reason));
        String until = expires == null ? "permanente" : "fino al " + expires;
        target.connection.disconnect(Component.literal("Sei stato bannato (" + until + ").\nMotivo: " + reason));
        return PlayerActionServer.Outcome.of(PlayerResult.OK);
    }

    private static PlayerActionServer.Outcome heal(ServerPlayer target) {
        target.setHealth(target.getMaxHealth());
        target.getFoodData().setFoodLevel(20);
        target.getFoodData().setSaturation(20.0F);
        target.clearFire();
        return PlayerActionServer.Outcome.of(PlayerResult.OK);
    }

    /** Porta {@code who} nel punto in cui si trova {@code to}, anche se in un'altra dimensione. */
    private static PlayerActionServer.Outcome teleport(ServerPlayer who, ServerPlayer to) {
        if (who == to) return PlayerActionServer.Outcome.of(PlayerResult.INVALID);
        who.teleportTo(to.level(), to.getX(), to.getY(), to.getZ(), Set.of(), to.getYRot(), to.getXRot(), true);
        return PlayerActionServer.Outcome.of(PlayerResult.OK);
    }

    private static PlayerActionServer.Outcome gameMode(ServerPlayer target, String arg) {
        GameType type = GameModes.parse(arg).map(PlayerActionHandlers::toGameType).orElse(null);
        if (type == null) return PlayerActionServer.Outcome.of(PlayerResult.INVALID);
        target.setGameMode(type);
        return PlayerActionServer.Outcome.of(PlayerResult.OK);
    }

    private static GameType toGameType(String id) {
        return switch (id) {
            case "creative" -> GameType.CREATIVE;
            case "adventure" -> GameType.ADVENTURE;
            case "spectator" -> GameType.SPECTATOR;
            default -> GameType.SURVIVAL;
        };
    }

    private static PlayerActionServer.Outcome freeze(ServerPlayer target) {
        boolean nowFrozen = FROZEN.toggle(target.getUUID(),
                new FreezeRegistry.Anchor<>(target.level(), target.getX(), target.getY(), target.getZ(), target.getYRot(),
                        target.getXRot()));
        target.sendSystemMessage(Component.literal(nowFrozen ? "Sei stato congelato dallo staff."
                : "Sei stato scongelato."));
        return PlayerActionServer.Outcome.of(nowFrozen ? PlayerResult.FROZEN : PlayerResult.UNFROZEN);
    }

    /** Tick del giocatore: se è congelato e si è mosso (o ha cambiato mondo) lo riporta al punto di blocco. */
    static void enforceFreeze(ServerPlayer player) {
        if (FROZEN.isEmpty()) return;
        FROZEN.anchorOf(player.getUUID()).ifPresent(a -> {
            boolean moved = player.level() != a.level() || Math.abs(player.getX() - a.x()) > 0.01
                    || Math.abs(player.getY() - a.y()) > 0.01 || Math.abs(player.getZ() - a.z()) > 0.01;
            if (moved) {
                player.teleportTo(a.level(), a.x(), a.y(), a.z(), Set.of(), a.yRot(), a.xRot(), false);
                player.setDeltaMovement(0, 0, 0);
            }
        });
    }
}
