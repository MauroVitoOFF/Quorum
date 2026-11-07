package dev.qrm.vehicles.client;

import dev.qrm.vehicles.logic.VehicleHitboxes;
import dev.qrm.vehicles.mc.QrmVehicleEntity;
import dev.qrm.vehicles.mc.VehicleServer;
import dev.qrm.vehicles.net.VehiclePayloads;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * Il giocatore a piedi di fronte ai veicoli: l'ingombro disegnato nel modello lo ferma, e la zona di interazione
 * che sta guardando (se c'è) illumina il pezzo corrispondente. Il clic destro su una zona manda l'uso al server.
 * Tutto lato client; il server riverifica il puntamento prima di fare qualsiasi cosa.
 */
public final class VehicleTargeting {
    private VehicleTargeting() {}

    /** Il veicolo e la zona guardati in questo momento. */
    private static int targetVehicle = -1;
    private static String targetZone = "";

    /** L'osso da illuminare su questo veicolo, o {@code null} se nessuna sua zona è puntata. */
    public static @Nullable String highlightedBone(QrmVehicleEntity vehicle) {
        return vehicle.getId() == targetVehicle && !targetZone.isEmpty() ? vehicle.hitboxes().highlightBone(targetZone) : null;
    }

    /** Il veicolo puntato adesso, o {@code null}. */
    static @Nullable QrmVehicleEntity targetVehicle() {
        Minecraft mc = Minecraft.getInstance();
        if (targetVehicle < 0 || mc.level == null) return null;
        return mc.level.getEntity(targetVehicle) instanceof QrmVehicleEntity v ? v : null;
    }

    /** La zona puntata adesso, o {@code null}. */
    static @Nullable String targetZone() {
        return targetZone.isEmpty() ? null : targetZone;
    }

    /** La zona puntata su questo veicolo (per la vista di debug), o {@code null}. */
    public static @Nullable String targetedZone(QrmVehicleEntity vehicle) {
        return vehicle.getId() == targetVehicle && !targetZone.isEmpty() ? targetZone : null;
    }

    private static void clear() {
        targetVehicle = -1;
        targetZone = "";
    }

    /** Da registrare sul bus di NeoForge, lato client. */
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || player.isSpectator()) {
            clear();
            resetHold();
            return;
        }
        List<QrmVehicleEntity> near = mc.level.getEntitiesOfClass(QrmVehicleEntity.class,
                player.getBoundingBox().inflate(VehicleServer.INTERACT_REACH + 6));
        // Da seduti l'auto non spinge fuori, ma le zone restano usabili.
        if (player.getVehicle() == null) push(player, near);
        target(mc, player, near);
        tickHold(mc);
    }

    /** Tick di pressione prolungata sulla chiave: oltre questa soglia il blocchetto gira. */
    private static final int HOLD_TICKS = 8;
    private static final String KEY_ZONE = "hit_ignition_key";
    private static int holdTicks = -1;
    private static boolean holdSent;

    private static void resetHold() {
        holdTicks = -1;
        holdSent = false;
    }

    private static boolean onKey() {
        return targetVehicle >= 0 && targetZone.equals(KEY_ZONE);
    }

    /** Un clic breve sulla chiave la inserisce o la toglie; tenuto premuto gira il blocchetto. */
    private static void tickHold(Minecraft mc) {
        if (holdTicks < 0) return;
        if (!onKey()) {
            resetHold();
            return;
        }
        if (!mc.options.keyUse.isDown()) {
            if (!holdSent) {
                ClientPacketDistributor.sendToServer(new VehiclePayloads.Interact(targetVehicle, targetZone, false));
            }
            resetHold();
            return;
        }
        holdTicks++;
        if (!holdSent && holdTicks >= HOLD_TICKS) {
            holdSent = true;
            ClientPacketDistributor.sendToServer(new VehiclePayloads.Interact(targetVehicle, targetZone, true));
        }
    }

    private static void push(LocalPlayer player, List<QrmVehicleEntity> near) {
        for (QrmVehicleEntity v : near) {
            VehicleHitboxes h = v.hitboxes();
            if (!h.hasSolid()) continue;
            double[] d = h.pushOut(player.getX(), player.getY(), player.getZ(), v.getX(), v.getY(), v.getZ(),
                    v.getYRot());
            if (d == null) continue;
            player.setPos(player.getX() + d[0], player.getY() + d[1], player.getZ() + d[2]);
            Vec3 m = player.getDeltaMovement();
            if (d[1] > 0) {
                player.setDeltaMovement(m.x, Math.max(0, m.y), m.z);
                player.setOnGround(true);
            } else {
                // Si perde solo la parte di moto che spingeva contro l'auto.
                double len = Math.hypot(d[0], d[2]);
                double nx = d[0] / len;
                double nz = d[2] / len;
                double into = m.x * nx + m.z * nz;
                if (into < 0) player.setDeltaMovement(m.x - nx * into, m.y, m.z - nz * into);
            }
        }
    }

    private static void target(Minecraft mc, LocalPlayer player, List<QrmVehicleEntity> near) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        // Un blocco davanti alla zona la nasconde.
        double reach = VehicleServer.INTERACT_REACH;
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            reach = Math.min(reach, mc.hitResult.getLocation().distanceTo(eye));
        }
        QrmVehicleEntity best = null;
        VehicleHitboxes.Hit bestHit = null;
        for (QrmVehicleEntity v : near) {
            VehicleHitboxes.Hit hit = v.hitboxes().raycast(eye.x, eye.y, eye.z, look.x, look.y, look.z, reach, v.getX(),
                    v.getY(), v.getZ(), v.getYRot(), v.hitPose(), VehicleHitboxes.AIM_MARGIN);
            if (hit != null && (bestHit == null || hit.distance() < bestHit.distance())) {
                best = v;
                bestHit = hit;
            }
        }
        if (best == null) {
            clear();
        } else {
            targetVehicle = best.getId();
            targetZone = bestHit.zone();
        }
    }

    /** Il clic destro su una zona la usa, invece di fare quel che farebbe il gioco. */
    public static void onUse(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem() || targetVehicle < 0 || targetZone.isEmpty()) return;
        event.setCanceled(true);
        event.setSwingHand(false);
        if (onKey()) {
            // La scelta tra clic e pressione prolungata si prende al rilascio (o alla soglia).
            if (holdTicks < 0) {
                holdTicks = 0;
                holdSent = false;
            }
            return;
        }
        ClientPacketDistributor.sendToServer(new VehiclePayloads.Interact(targetVehicle, targetZone, false));
    }
}
