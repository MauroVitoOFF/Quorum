package dev.qrm.medical.mc;

import dev.qrm.api.QRM;
import dev.qrm.api.character.CharacterService;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.data.ModDataService;
import dev.qrm.api.id.CharacterId;
import dev.qrm.api.id.PlayerId;
import dev.qrm.api.permission.PermissionService;
import dev.qrm.medical.logic.InjuryRecord;
import dev.qrm.medical.logic.InjuryType;
import dev.qrm.medical.logic.MedicalProfile;
import dev.qrm.medical.logic.MedicalRules;
import dev.qrm.medical.logic.MedicalState;
import dev.qrm.medical.logic.MedicalStorage;
import dev.qrm.medical.logic.ModDataMedicalStorage;
import dev.qrm.medical.net.MedicalPayloads;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** Stato medico sul server: i profili dei giocatori online e tutto ciò che li cambia. */
public final class MedicalServer {
    static final Identifier DOWNED_SPEED = Identifier.fromNamespaceAndPath(MedicalMod.MOD_ID, "downed_speed");
    static final Identifier DOWNED_JUMP = Identifier.fromNamespaceAndPath(MedicalMod.MOD_ID, "downed_jump");
    static final Identifier CARRY_SPEED = Identifier.fromNamespaceAndPath(MedicalMod.MOD_ID, "carry_speed");
    static final String NODE_ALERT = "medical.alert.receive";
    static final String NODE_TREAT = "medical.treat";
    private static final int SAVE_EVERY = 200;
    private static final int STATUS_EVERY = 20;
    /** Quanto dura una RCP dopo l'ultimo clic: il clic tenuto premuto si ripete ogni 4 tick circa. */
    private static final int RCP_GRACE = 12;
    private static final double REACH = 4.5;

    /** Il profilo caricato per un giocatore e per quale Character (nulla se non ne ha una attiva). */
    static final class Entry {
        final CharacterId character;
        final MedicalProfile profile;
        MedicalState applied = MedicalState.HEALTHY;

        Entry(CharacterId character, MedicalProfile profile) {
            this.character = character;
            this.profile = profile;
        }
    }

    private record Revival(UUID target, int endTick) {}

    private static final Map<UUID, Entry> ENTRIES = new HashMap<>();
    /** Ferito → chi gli fa la RCP, e fino a quale tick. */
    private static final Map<UUID, UUID> RCP_BY = new HashMap<>();
    private static final Map<UUID, Integer> RCP_UNTIL = new HashMap<>();
    /** Chi trasporta → il ferito trasportato. */
    private static final Map<UUID, UUID> CARRYING = new HashMap<>();
    /** Medico → rianimazione in corso. */
    private static final Map<UUID, Revival> REVIVALS = new HashMap<>();
    private static final Map<UUID, Integer> ALERT_AT = new HashMap<>();
    private static MedicalStorage storage;

    private MedicalServer() {}

    static synchronized MedicalStorage storage() {
        if (storage == null) storage = new ModDataMedicalStorage(QRM.get(ModDataService.class));
        return storage;
    }

    static synchronized void reset() {
        storage = null;
        ENTRIES.clear();
        RCP_BY.clear();
        RCP_UNTIL.clear();
        CARRYING.clear();
        REVIVALS.clear();
        ALERT_AT.clear();
    }

    // ---- profili ----

    static Entry entryOf(ServerPlayer p) {
        return ENTRIES.computeIfAbsent(p.getUUID(), id -> {
            Optional<CharacterId> character = QRM.get(CharacterService.class).activeOf(new PlayerId(id))
                    .map(PlayerCharacter::id);
            MedicalProfile profile = character.flatMap(storage()::read).orElseGet(MedicalProfile::new);
            return new Entry(character.orElse(null), profile);
        });
    }

    public static MedicalState stateOf(ServerPlayer p) {
        return entryOf(p).profile.state();
    }

    static boolean incapacitated(Player p) {
        return p instanceof ServerPlayer sp && entryOf(sp).profile.state().incapacitated();
    }

    private static void save(Entry e) {
        if (e.character != null) storage().write(e.character, e.profile);
    }

    /** Cambio di Character o uscita: si salva, si toglie ogni effetto e si ricarica al prossimo tick. */
    static void forget(UUID player) {
        Entry e = ENTRIES.remove(player);
        if (e != null) save(e);
        RCP_BY.remove(player);
        RCP_UNTIL.remove(player);
        CARRYING.remove(player);
        REVIVALS.remove(player);
        ALERT_AT.remove(player);
        RCP_BY.values().removeIf(player::equals);
        CARRYING.values().removeIf(player::equals);
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerPlayer p = server == null ? null : server.getPlayerList().getPlayer(player);
        if (p != null) {
            clearModifiers(p);
            send(p, new MedicalProfile(), false);
        }
    }

    // ---- stati ----

    /** Il giocatore cade a terra, con la ferita che l'ha fatto cadere. */
    static void down(ServerPlayer p, InjuryRecord injury) {
        Entry e = entryOf(p);
        if (injury != null) e.profile.addInjury(injury);
        if (e.profile.state() == MedicalState.HEALTHY) MedicalRules.down(e.profile);
        p.setHealth(1.0F);
        apply(p, e);
        save(e);
        p.sendSystemMessage(Component.translatable("qrm_medical.down"));
    }

    /** Rianimazione: torna in piedi con i cuori indicati (mezzi punti vita = 2 per cuore). */
    static void revive(ServerPlayer p, float health) {
        Entry e = entryOf(p);
        MedicalRules.revive(e.profile);
        RCP_BY.remove(p.getUUID());
        RCP_UNTIL.remove(p.getUUID());
        CARRYING.values().removeIf(p.getUUID()::equals);
        p.setHealth(Math.min(health, p.getMaxHealth()));
        p.setPose(Pose.STANDING);
        apply(p, e);
        save(e);
    }

    /** Morte clinica: ricovero. L'inventario resta, la vita sale al 50%, il giocatore va in ospedale. */
    static void hospitalize(ServerPlayer p) {
        Entry e = entryOf(p);
        MedicalRules.revive(e.profile);
        RCP_BY.remove(p.getUUID());
        RCP_UNTIL.remove(p.getUUID());
        CARRYING.values().removeIf(p.getUUID()::equals);
        p.removeAllEffects();
        p.setHealth(p.getMaxHealth() * 0.5F);
        p.setPose(Pose.STANDING);
        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, MedicalRules.CONVALESCENCE_TICKS, 0));
        p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, MedicalRules.CONVALESCENCE_TICKS, 0));
        Optional<MedicalStorage.Hospital> h = storage().hospital();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (h.isPresent() && server != null) {
            ServerLevel level = server.getLevel(
                    ResourceKey.create(Registries.DIMENSION, Identifier.parse(h.get().dimension())));
            if (level != null) {
                p.teleportTo(level, h.get().x(), h.get().y(), h.get().z(), Set.of(), h.get().yaw(), 0.0F, true);
            }
        }
        apply(p, e);
        save(e);
        p.sendSystemMessage(Component.translatable("qrm_medical.hospitalized"));
    }

    /** Il giocatore è morto davvero (/kill, vuoto): lo stato medico non lo segue. */
    static void onRealDeath(ServerPlayer p) {
        Entry e = entryOf(p);
        MedicalRules.revive(e.profile);
        RCP_BY.remove(p.getUUID());
        RCP_UNTIL.remove(p.getUUID());
        CARRYING.values().removeIf(p.getUUID()::equals);
        clearModifiers(p);
        send(p, e.profile, false);
        save(e);
    }

    /** Colpo diretto a un ferito a terra: perde i sensi. */
    static void criticalHit(ServerPlayer p) {
        Entry e = entryOf(p);
        if (MedicalRules.criticalHit(e.profile)) {
            apply(p, e);
            save(e);
        }
    }

    static void record(ServerPlayer p, InjuryRecord r) {
        entryOf(p).profile.addInjury(r);
    }

    /** Applica o toglie i modificatori e dice al client lo stato. */
    private static void apply(ServerPlayer p, Entry e) {
        MedicalState state = e.profile.state();
        e.applied = state;
        clearModifiers(p);
        if (state.incapacitated()) {
            double slow = state == MedicalState.UNCONSCIOUS ? -1.0 : -0.9;
            AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.addOrUpdateTransientModifier(new AttributeModifier(DOWNED_SPEED, slow,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
            AttributeInstance jump = p.getAttribute(Attributes.JUMP_STRENGTH);
            if (jump != null) {
                jump.addOrUpdateTransientModifier(new AttributeModifier(DOWNED_JUMP, -1.0,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }
        send(p, e.profile, rcpActive(p));
    }

    private static void clearModifiers(ServerPlayer p) {
        for (var attr : List.of(Attributes.MOVEMENT_SPEED, Attributes.JUMP_STRENGTH)) {
            AttributeInstance i = p.getAttribute(attr);
            if (i == null) continue;
            i.removeModifier(DOWNED_SPEED);
            i.removeModifier(DOWNED_JUMP);
        }
    }

    private static void send(ServerPlayer p, MedicalProfile profile, boolean rcp) {
        if (!p.connection.hasChannel(MedicalPayloads.Status.TYPE)) return;
        PacketDistributor.sendToPlayer(p, new MedicalPayloads.Status(profile.state().ordinal(),
                profile.bleedOutTicks(), profile.downedTicks(), rcp));
    }

    // ---- RCP ----

    static boolean rcpActive(ServerPlayer patient) {
        Integer until = RCP_UNTIL.get(patient.getUUID());
        MinecraftServer server = patient.level().getServer();
        if (until == null || server == null || until < server.getTickCount()) return false;
        ServerPlayer rescuer = server.getPlayerList().getPlayer(RCP_BY.get(patient.getUUID()));
        return rescuer != null && rescuer.isShiftKeyDown() && !incapacitated(rescuer)
                && rescuer.distanceToSqr(patient) <= REACH * REACH;
    }

    /** Un clic della RCP: rinnova la finestra e, una volta al secondo, mostra e fa sentire che sta funzionando. */
    static void rcpClick(ServerPlayer rescuer, ServerPlayer patient) {
        MinecraftServer server = rescuer.level().getServer();
        if (server == null) return;
        int now = server.getTickCount();
        boolean fresh = !rcpActive(patient);
        RCP_BY.put(patient.getUUID(), rescuer.getUUID());
        RCP_UNTIL.put(patient.getUUID(), now + RCP_GRACE);
        if (fresh || now % 20 < 4) {
            ((ServerLevel) patient.level()).sendParticles(ParticleTypes.HEART, patient.getX(), patient.getY() + 0.6,
                    patient.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
            patient.level().playSound(null, patient.blockPosition(), SoundEvents.NOTE_BLOCK_BASEDRUM.value(),
                    SoundSource.PLAYERS, 0.6F, 0.8F);
            rescuer.sendSystemMessage(Component.translatable("qrm_medical.rcp.doing"), true);
        }
        if (fresh) apply(patient, entryOf(patient));
    }

    // ---- rianimazione con il defibrillatore ----

    static void startRevival(ServerPlayer medic, ServerPlayer patient) {
        MinecraftServer server = medic.level().getServer();
        if (server == null) return;
        if (REVIVALS.containsKey(medic.getUUID())) return;
        REVIVALS.put(medic.getUUID(), new Revival(patient.getUUID(), server.getTickCount() + MedicalRules.DEFIBRILLATOR_TICKS));
        medic.level().playSound(null, medic.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.6F);
        medic.sendSystemMessage(Component.translatable("qrm_medical.defib.charging"), true);
    }

    // ---- trasporto ----

    static boolean toggleCarry(ServerPlayer carrier) {
        if (CARRYING.remove(carrier.getUUID()) != null) {
            stopCarry(carrier);
            return false;
        }
        ServerPlayer best = null;
        double bestDist = 3.5 * 3.5;
        for (ServerPlayer o : carrier.level().getServer().getPlayerList().getPlayers()) {
            if (o == carrier || o.level() != carrier.level() || !incapacitated(o)) continue;
            double d = o.distanceToSqr(carrier);
            if (d < bestDist && !CARRYING.containsValue(o.getUUID())) {
                best = o;
                bestDist = d;
            }
        }
        if (best == null) return false;
        CARRYING.put(carrier.getUUID(), best.getUUID());
        AttributeInstance speed = carrier.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.addOrUpdateTransientModifier(new AttributeModifier(CARRY_SPEED, -0.5,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        return true;
    }

    private static void stopCarry(ServerPlayer carrier) {
        AttributeInstance speed = carrier.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(CARRY_SPEED);
    }

    // ---- tick ----

    static void tick(MinecraftServer server) {
        int now = server.getTickCount();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            Entry e = entryOf(p);
            MedicalState state = e.profile.state();
            if (state != e.applied) apply(p, e);
            if (!state.incapacitated()) continue;
            tickPatient(p, e, now);
        }
        tickCarry(server);
        tickRevivals(server, now);
    }

    private static void tickPatient(ServerPlayer p, Entry e, int now) {
        p.setPose(Pose.SWIMMING);
        p.setSprinting(false);
        if (p.getHealth() != 1.0F) p.setHealth(1.0F);
        if (p.containerMenu != p.inventoryMenu) p.closeContainer();
        boolean rcp = rcpActive(p);
        MedicalState changed = MedicalRules.tick(e.profile, rcp, now);
        if (changed == MedicalState.CLINICALLY_DEAD) {
            hospitalize(p);
            return;
        }
        if (changed != null) {
            apply(p, e);
            save(e);
        }
        if (now % STATUS_EVERY == 0) send(p, e.profile, rcp);
        if (now % SAVE_EVERY == 0) save(e);
    }

    private static void tickCarry(MinecraftServer server) {
        for (Iterator<Map.Entry<UUID, UUID>> it = CARRYING.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, UUID> c = it.next();
            ServerPlayer carrier = server.getPlayerList().getPlayer(c.getKey());
            ServerPlayer patient = server.getPlayerList().getPlayer(c.getValue());
            if (carrier == null || patient == null || incapacitated(carrier) || !incapacitated(patient)
                    || carrier.level() != patient.level()) {
                if (carrier != null) stopCarry(carrier);
                it.remove();
                continue;
            }
            if (carrier.distanceToSqr(patient) > 2.0 * 2.0) {
                Vec3 look = carrier.getLookAngle();
                Vec3 spot = carrier.position().add(-look.x * 0.9, 0.0, -look.z * 0.9);
                patient.connection.teleport(spot.x, spot.y, spot.z, patient.getYRot(), patient.getXRot());
            }
        }
    }

    private static void tickRevivals(MinecraftServer server, int now) {
        for (Iterator<Map.Entry<UUID, Revival>> it = REVIVALS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Revival> r = it.next();
            ServerPlayer medic = server.getPlayerList().getPlayer(r.getKey());
            ServerPlayer patient = server.getPlayerList().getPlayer(r.getValue().target());
            boolean ok = medic != null && patient != null && incapacitated(patient) && !incapacitated(medic)
                    && medic.getMainHandItem().is(MedicalItems.DEFIBRILLATOR.get())
                    && medic.distanceToSqr(patient) <= REACH * REACH;
            if (!ok) {
                if (medic != null) medic.sendSystemMessage(Component.translatable("qrm_medical.defib.interrupted"), true);
                it.remove();
                continue;
            }
            int left = r.getValue().endTick() - now;
            if (left > 0) {
                if (left % 10 == 0) {
                    medic.level().playSound(null, medic.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT,
                            SoundSource.PLAYERS, 0.3F, 2.0F);
                    int pct = 100 - left * 100 / MedicalRules.DEFIBRILLATOR_TICKS;
                    medic.sendSystemMessage(Component.translatable("qrm_medical.defib.progress", pct), true);
                }
                continue;
            }
            it.remove();
            revive(patient, 6.0F);
            patient.level().playSound(null, patient.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                    SoundSource.PLAYERS, 0.6F, 1.8F);
            patient.sendSystemMessage(Component.translatable("qrm_medical.revived"));
            medic.sendSystemMessage(Component.translatable("qrm_medical.defib.done", patient.getDisplayName()), true);
        }
    }

    // ---- richieste del client ----

    public static void onAction(Player player, int kind) {
        if (!(player instanceof ServerPlayer p)) return;
        Entry e = entryOf(p);
        if (!e.profile.state().incapacitated()) return;
        if (kind == MedicalPayloads.ACTION_SURRENDER) {
            if (!MedicalRules.canSurrender(e.profile)) return;
            hospitalize(p);
        } else if (kind == MedicalPayloads.ACTION_ALERT) {
            alert(p);
        }
    }

    private static void alert(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        if (server == null) return;
        int now = server.getTickCount();
        Integer last = ALERT_AT.get(p.getUUID());
        if (last != null && now - last < MedicalRules.ALERT_COOLDOWN_TICKS) {
            p.sendSystemMessage(Component.translatable("qrm_medical.alert.cooldown"), true);
            return;
        }
        ALERT_AT.put(p.getUUID(), now);
        BlockPos pos = p.blockPosition();
        Component msg = Component.translatable("qrm_medical.alert.received", p.getDisplayName(), pos.getX(),
                pos.getY(), pos.getZ());
        PermissionService perms = QRM.get(PermissionService.class);
        CharacterService characters = QRM.get(CharacterService.class);
        int sent = 0;
        for (ServerPlayer o : server.getPlayerList().getPlayers()) {
            if (o == p) continue;
            boolean may = characters.activeOf(new PlayerId(o.getUUID()))
                    .map(c -> perms.has(c.id(), NODE_ALERT)).orElse(false);
            if (!may) continue;
            o.sendSystemMessage(msg);
            o.level().playSound(null, o.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS,
                    1.0F, 0.7F);
            sent++;
        }
        // Chi gestisce le chiamate (es. il dispatch di polizia) la riceve come evento: nessuna dipendenza fra moduli.
        QRM.events().post(new dev.qrm.api.emergency.EmergencyAlertEvent(p.getUUID(), p.getGameProfile().name(),
                p.level().dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ(),
                dev.qrm.api.emergency.EmergencyAlertEvent.Kind.MEDICAL, "Persona ferita a terra"));
        p.sendSystemMessage(Component.translatable(sent > 0 ? "qrm_medical.alert.sent" : "qrm_medical.alert.nobody"));
    }

    // ---- ispezione e cure ----

    static boolean mayTreat(ServerPlayer p) {
        return QRM.get(CharacterService.class).activeOf(new PlayerId(p.getUUID()))
                .map(c -> QRM.get(PermissionService.class).has(c.id(), NODE_TREAT)).orElse(false);
    }

    static void inspect(ServerPlayer viewer, ServerPlayer patient) {
        Entry e = entryOf(patient);
        MedicalProfile prof = e.profile;
        boolean rcp = rcpActive(patient);
        viewer.sendSystemMessage(Component.translatable("qrm_medical.inspect.header", patient.getDisplayName()));
        viewer.sendSystemMessage(Component.translatable("qrm_medical.inspect.state."
                + prof.state().name().toLowerCase(java.util.Locale.ROOT)));
        viewer.sendSystemMessage(Component.translatable(MedicalRules.pulseWeak(prof, rcp)
                ? "qrm_medical.inspect.pulse.weak" : "qrm_medical.inspect.pulse.stable"));
        if (mayTreat(viewer)) {
            int s = prof.bleedOutTicks() / 20;
            viewer.sendSystemMessage(Component.translatable("qrm_medical.inspect.time", s / 60, String.format("%02d", s % 60)));
        }
        showInjuries(viewer::sendSystemMessage, prof);
    }

    static void showInjuries(java.util.function.Consumer<Component> out, MedicalProfile prof) {
        List<InjuryRecord> all = prof.injuries();
        if (all.isEmpty()) {
            out.accept(Component.translatable("qrm_medical.inspect.noinjuries"));
            return;
        }
        long nowMs = System.currentTimeMillis();
        for (int i = all.size() - 1; i >= 0; i--) {
            InjuryRecord r = all.get(i);
            long sec = Math.max(0, (nowMs - r.timestamp()) / 1000);
            out.accept(Component.translatable("qrm_medical.inspect.injury",
                    Component.translatable(r.type().langKey()), String.format("%.1f", r.severity() / 2.0F),
                    sec / 60, String.format("%02d", sec % 60)));
        }
    }

    /** Cura: kit e bende. Un kit su un ferito lo stabilizza, non lo rialza. */
    static boolean treat(ServerPlayer actor, ServerPlayer patient, ItemStack stack, float heal, boolean stabilize) {
        Entry e = entryOf(patient);
        boolean down = e.profile.state().incapacitated();
        if (down) {
            if (!stabilize) {
                actor.sendSystemMessage(Component.translatable("qrm_medical.treat.needs_kit"), true);
                return false;
            }
            MedicalRules.stabilize(e.profile);
            apply(patient, e);
            save(e);
        } else {
            boolean convalescent = patient.hasEffect(MobEffects.WEAKNESS) || patient.hasEffect(MobEffects.SLOWNESS);
            if (patient.getHealth() >= patient.getMaxHealth() && !convalescent) {
                actor.sendSystemMessage(Component.translatable("qrm_medical.treat.nothing"), true);
                return false;
            }
            patient.heal(heal);
            patient.removeEffect(MobEffects.WEAKNESS);
            patient.removeEffect(MobEffects.SLOWNESS);
        }
        patient.level().playSound(null, patient.blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1.0F, 1.0F);
        if (!actor.isCreative()) stack.shrink(1);
        patient.sendSystemMessage(Component.translatable("qrm_medical.treat.done"), true);
        return true;
    }

    /** Admin: porta il giocatore a terra. */
    public static void adminDown(ServerPlayer p) {
        if (entryOf(p).profile.state() != MedicalState.HEALTHY) return;
        down(p, new InjuryRecord(InjuryType.OTHER, System.currentTimeMillis(), 0.0F));
    }

    public static MedicalProfile profileOf(ServerPlayer p) {
        return entryOf(p).profile;
    }

    public static void adminRevive(ServerPlayer p) {
        revive(p, p.getMaxHealth());
    }
}
