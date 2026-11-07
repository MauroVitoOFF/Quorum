package dev.qrm.vehicles.mc;

import com.mojang.serialization.Codec;
import dev.qrm.vehicles.logic.DriveInput;
import dev.qrm.vehicles.logic.DriveState;
import dev.qrm.vehicles.logic.DrivingModel;
import dev.qrm.vehicles.logic.EngineState;
import dev.qrm.vehicles.logic.EngineStatus;
import dev.qrm.vehicles.logic.FuelTank;
import dev.qrm.vehicles.logic.Odometer;
import dev.qrm.vehicles.logic.SeatMath;
import dev.qrm.vehicles.logic.Surface;
import dev.qrm.vehicles.logic.VehicleDef;
import dev.qrm.vehicles.logic.VehicleMotionFx;
import dev.qrm.vehicles.logic.VehicleParts;
import dev.qrm.vehicles.logic.VehicleSaveData;
import dev.qrm.vehicles.logic.VehicleSeats;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Un veicolo guidabile. Il server è autorevole: simula la guida con {@link DrivingModel} a partire dall'ultimo input
 * del guidatore e la posizione arriva ai client con la normale sincronizzazione delle entità. Carburante e
 * chilometri vivono nei campi del server e sono copiati, arrotondati, nei dati sincronizzati per HUD e rendering.
 */
public final class QrmVehicleEntity extends Entity {

    private static final EntityDataAccessor<Integer> ENGINE_STATE =
            SynchedEntityData.defineId(QrmVehicleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> LIGHTS =
            SynchedEntityData.defineId(QrmVehicleEntity.class, EntityDataSerializers.BOOLEAN);
    /** Maschera di {@link VehicleParts}: portiere, cofano e freno a mano. */
    private static final EntityDataAccessor<Integer> PARTS =
            SynchedEntityData.defineId(QrmVehicleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> FUEL =
            SynchedEntityData.defineId(QrmVehicleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ODOMETER_BLOCKS =
            SynchedEntityData.defineId(QrmVehicleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SPEED =
            SynchedEntityData.defineId(QrmVehicleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> STEER =
            SynchedEntityData.defineId(QrmVehicleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> GEAR =
            SynchedEntityData.defineId(QrmVehicleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RPM =
            SynchedEntityData.defineId(QrmVehicleEntity.class, EntityDataSerializers.FLOAT);
    /** Id dell'entità seduta sul posto 0 e sul posto 1 (-1 = libero): il posto lo sceglie il server, il client lo legge. */
    private static final EntityDataAccessor<Integer> SEAT_0 =
            SynchedEntityData.defineId(QrmVehicleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SEAT_1 =
            SynchedEntityData.defineId(QrmVehicleEntity.class, EntityDataSerializers.INT);
    private static final int SYNCED_SEATS = 2;

    /** Oltre questi tick senza input il guidatore è come se avesse lasciato i comandi. */
    private static final int INPUT_TIMEOUT_TICKS = 5;
    private static final int HONK_COOLDOWN_TICKS = 6;

    private final VehicleDef def = VehicleEntities.defOf(getType());
    private final InterpolationHandler interpolation = new InterpolationHandler(this, 3);
    private FuelTank tank = new FuelTank(def.fuelCapacity(), def.fuelCapacity());
    private Odometer odometer = new Odometer(0);
    private final VehicleMotionFx fx = new VehicleMotionFx();
    private final VehicleParts.Animator partsAnimator = new VehicleParts.Animator();
    private boolean partsSnapped;
    /**
     * Impostato dal client: scostamento {destra, su, avanti} della visuale del giocatore locale in prima persona, in
     * unità del modello; null per tutti gli altri. Sul server non viene mai interrogato.
     */
    public static volatile java.util.function.Function<Entity, float[]> VIEW_OFFSET = e -> null;

    /** Marcia scelta dal guidatore: -1 retromarcia, 0 folle, da 1 le marce avanti. Il cambio è manuale. */
    private int selector;
    /** Input di sterzo ammorbidito (-1..1), solo sul server. */
    private double steerInput;
    private DriveState drive = DriveState.rest(0);
    private final VehicleParticles particles = new VehicleParticles();
    private EngineStatus engineStatus = EngineStatus.OFF;
    private boolean headingInitialised;
    private DriveInput input = DriveInput.NONE;
    private int ticksSinceInput = INPUT_TIMEOUT_TICKS;
    private long lastHonkTick = Long.MIN_VALUE / 2;

    public QrmVehicleEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    // ---- dati sincronizzati (leggibili anche dal client) ----

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ENGINE_STATE, EngineState.OFF.id());
        builder.define(LIGHTS, false);
        builder.define(PARTS, 0);
        // Chiamato dal costruttore di Entity, prima dei campi di questa classe: il tipo però c'è già.
        builder.define(FUEL, (float) VehicleEntities.defOf(getType()).fuelCapacity());
        builder.define(ODOMETER_BLOCKS, 0f);
        builder.define(SPEED, 0f);
        builder.define(STEER, 0f);
        builder.define(GEAR, 0);
        builder.define(RPM, 0f);
        builder.define(SEAT_0, -1);
        builder.define(SEAT_1, -1);
    }

    public VehicleDef def() {
        return def;
    }

    /** Effetti di movimento (ruote, assetto): vivono solo sul client. */
    public VehicleMotionFx motionFx() {
        return fx;
    }

    /** Il motore produce spinta (al minimo o in lavoro). */
    public boolean engineOn() {
        return engineState().isRunning();
    }

    public EngineState engineState() {
        return EngineState.byId(entityData.get(ENGINE_STATE));
    }

    public boolean lightsOn() {
        return entityData.get(LIGHTS);
    }

    /** Maschera di {@link VehicleParts} (portiere, cofano, freno a mano). */
    public int parts() {
        return entityData.get(PARTS);
    }

    public boolean parkingBrakeOn() {
        return VehicleParts.has(parts(), VehicleParts.PARKING_BRAKE);
    }

    public boolean keyInserted() {
        return VehicleParts.has(parts(), VehicleParts.KEY_INSERTED);
    }

    /** Valori che le parti mobili inseguono, dallo stato sincronizzato. */
    public float[] partsTargets() {
        EngineState es = engineState();
        int engine = es == EngineState.OFF ? 0 : es == EngineState.STARTING ? 2 : 1;
        return VehicleParts.targets(parts(), lightsOn(), engine, gear());
    }

    /** Animazione delle parti mobili: vive solo sul client. */
    public VehicleParts.Animator partsAnimator() {
        return partsAnimator;
    }

    public double fuelLevel() {
        return entityData.get(FUEL);
    }

    public double odometerKm() {
        return entityData.get(ODOMETER_BLOCKS) / 1000.0;
    }

    /** Velocità in avanti in blocchi per tick (negativa in retromarcia). */
    public double speed() {
        return entityData.get(SPEED);
    }

    public float steerAngle() {
        return entityData.get(STEER);
    }

    /** Marcia innestata: 1..n avanti, -1 retromarcia. */
    public int gear() {
        return entityData.get(GEAR);
    }

    /** Regime del motore in giri/min, 0 a motore spento. */
    public float rpm() {
        return entityData.get(RPM);
    }

    // ---- comandi (solo server) ----

    /** Registra l'ultimo input del guidatore. Chi chiama deve aver verificato che mandi il guidatore vero. */
    public void setDriverInput(DriveInput in) {
        this.input = in.clamped();
        this.ticksSinceInput = 0;
    }

    /** Accende o spegne il motore; a serbatoio vuoto non si accende. */
    public void toggleEngine() {
        // Senza chiave, o con il cofano aperto, il motore spento non parte.
        if (!keyInserted()) return;
        if (engineState() == EngineState.OFF && VehicleParts.has(parts(), VehicleParts.HOOD)) return;
        EngineStatus before = engineStatus;
        engineStatus = engineStatus.toggle(!tank.isEmpty(), isInWater() || isInLava());
        entityData.set(ENGINE_STATE, engineStatus.state().id());
        if (engineStatus.state() == EngineState.STARTING && before.state() != EngineState.STARTING) {
            VehicleSounds.start(def.id()).ifPresent(sound ->
                    level().playSound(null, getX(), getY(), getZ(), sound, SoundSource.NEUTRAL, 1.0f, 1.0f));
        }
    }

    public void toggleLights() {
        entityData.set(LIGHTS, !lightsOn());
        click();
    }

    /** Inserisce o toglie la chiave; col motore acceso non si toglie. */
    public void toggleKey() {
        if (engineState() != EngineState.OFF) return;
        entityData.set(PARTS, parts() ^ VehicleParts.KEY_INSERTED);
        click();
    }

    private void click() {
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.NEUTRAL, 0.6f,
                1.4f);
    }

    /** Porta il cambio a una marcia (-1..marce del veicolo); la retromarcia non si innesta andando avanti veloci. */
    public void shiftTo(int gear) {
        int target = Math.max(-1, Math.min(def.powertrain().gears(), gear));
        if (target == selector) return;
        if (target < 0 && speed() * DrivingModel.TPS > REVERSE_MAX_FORWARD_MS) return;
        selector = target;
        entityData.set(GEAR, selector);
        click();
    }

    public void shiftUp() {
        shiftTo(selector + 1);
    }

    public void shiftDown() {
        shiftTo(selector - 1);
    }

    /** Clic sulla leva: folle, retromarcia, prima, di nuovo folle. */
    private void cycleShifter() {
        shiftTo(selector == 0 ? -1 : selector < 0 ? 1 : 0);
    }

    /** Velocità (m/s) oltre la quale la retromarcia non si innesta. */
    private static final double REVERSE_MAX_FORWARD_MS = 2.0;

    /** Il freno a mano si inserisce e si toglie in ogni momento. */
    public void toggleParkingBrake() {
        entityData.set(PARTS, parts() ^ VehicleParts.PARKING_BRAKE);
        click();
    }

    /** Apre o chiude una portiera o il cofano; non si fa in marcia. */
    private void togglePart(int bit) {
        if (Math.abs(speed()) > VehicleParts.MAX_OPEN_SPEED) return;
        boolean opening = !VehicleParts.has(parts(), bit);
        entityData.set(PARTS, parts() ^ bit);
        level().playSound(null, getX(), getY(), getZ(),
                bit == VehicleParts.HOOD
                        ? (opening ? SoundEvents.IRON_TRAPDOOR_OPEN : SoundEvents.IRON_TRAPDOOR_CLOSE)
                        : (opening ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE),
                SoundSource.NEUTRAL, 0.8f, 0.9f);
    }

    /** Il clacson, con una breve pausa tra un colpo e l'altro. Il suono è provvisorio. */
    public void honk() {
        long now = level().getGameTime();
        if (now - lastHonkTick < HONK_COOLDOWN_TICKS) return;
        lastHonkTick = now;
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.NEUTRAL, 2.0f,
                0.6f);
    }

    /** Aggiunge carburante; restituisce i litri davvero aggiunti. */
    public double addFuel(double liters) {
        double added = tank.add(liters);
        syncData();
        return added;
    }

    public double fuelRoom() {
        return tank.room();
    }

    // ---- tick ----

    /**
     * Il server decide sempre la posizione: senza questo, il client del guidatore si considera autorevole sul
     * veicolo, ignora le posizioni del server e rimanda la propria (ferma), così il mezzo non si muove finché si è a bordo.
     */
    @Override
    protected boolean isLocalClientAuthoritative() {
        return false;
    }

    @Override
    public boolean isClientAuthoritative() {
        return false;
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return interpolation;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            interpolation.interpolate();
            fx.tick(speed(), steerAngle(), def.maxSpeed(), def.maxSteerDeg(), getY() - yo);
            if (!partsSnapped) {
                partsAnimator.snap(partsTargets());
                partsSnapped = true;
            }
            partsAnimator.tick(partsTargets());
            if (dev.qrm.vehicles.client.VehicleClientConfig.PARTICLES.get()) particles.tick(this, surfaceUnder());
            dev.qrm.vehicles.client.EngineSounds.tick(this);
            return;
        }
        serverTick();
    }

    /** Posa attuale delle parti mobili: le zone di interazione appese a quelle ossa la seguono (client e server). */
    public java.util.Map<String, dev.qrm.client.api.rig.BoneTransform> hitPose() {
        return VehicleParts.pose(partsAnimator.values(1f));
    }

    private void serverTick() {
        // Anche il server anima le parti, così il suo puntamento coincide con quello del client.
        if (!partsSnapped) {
            partsAnimator.snap(partsTargets());
            partsSnapped = true;
        }
        partsAnimator.tick(partsTargets());
        if (!headingInitialised) {
            // Prima volta dopo la creazione o il caricamento: il muso resta dov'era, non torna a zero.
            drive = DriveState.rest(getYRot());
            headingInitialised = true;
        }
        if (ticksSinceInput <= INPUT_TIMEOUT_TICKS) ticksSinceInput++; // fermo al limite: niente overflow da parcheggiati
        LivingEntity driver = getControllingPassenger();
        DriveInput in = driver != null && ticksSinceInput <= INPUT_TIMEOUT_TICKS ? input : DriveInput.NONE;

        boolean swimming = isInWater() || isInLava();
        if (swimming) in = DriveInput.NONE;
        // Il freno a mano tiene l'auto ferma: niente gas, freno premuto.
        if (parkingBrakeOn()) in = new DriveInput(0, in.steer(), true);
        // Il volante non scatta: l'input di sterzo si ammorbidisce prima di arrivare alla fisica.
        // Il modello classico ha già il suo volante lento: l'input gli arriva com'è.
        steerInput = def.handling() == dev.qrm.vehicles.logic.HandlingModel.CLASSIC ? in.steer()
                : dev.qrm.vehicles.logic.SteerSmoothing.step(steerInput, in.steer(),
                        Math.abs(drive.forwardSpeed()) / def.maxSpeed());
        in = new DriveInput(in.throttle(), steerInput, in.brake());
        // In marcia portiere e cofano non restano aperti.
        int moving = VehicleParts.DOOR_L | VehicleParts.DOOR_R | VehicleParts.HOOD;
        if ((parts() & moving) != 0 && Math.abs(drive.forwardSpeed()) > VehicleParts.MAX_OPEN_SPEED) {
            entityData.set(PARTS, parts() & ~moving);
        }
        engineStatus = engineStatus.tick(tank.isEmpty(), swimming, in.throttle(), drive.forwardSpeed() * DrivingModel.TPS);
        if (engineStatus.state().id() != entityData.get(ENGINE_STATE)) entityData.set(ENGINE_STATE, engineStatus.state().id());
        boolean engine = engineStatus.state().isRunning();

        drive = DrivingModel.step(def, drive, in, surfaceUnder(), engine, slope, selector);
        if (swimming) drive = drive.withMotion(drive.heading(), 0, 0);

        double vy = isNoGravity() ? 0 : (getDeltaMovement().y - getGravity()) * 0.98;
        Vec3 before = position();
        boolean resting = Math.abs(drive.vx()) < REST_SPEED && Math.abs(drive.vz()) < REST_SPEED
                && Math.abs(drive.yawRate()) < REST_YAW && bodyClear;
        Vec3 moved = moveWithBody(drive.vx(), vy, drive.vz(), resting);
        double avx = moved.x;
        double avz = moved.z;
        // Chi è fermo non cerca urti: è il veicolo in moto a trovarlo e a spingerlo.
        if (!resting) {
            double[] v = resolveVehicleCollisions(avx, avz);
            avx = v[0];
            avz = v[1];
            setDeltaMovement(avx, getDeltaMovement().y, avz);
        }
        double heading = ((drive.heading() % 360) + 360) % 360;
        drive = drive.withMotion(heading, avx, avz);
        setYRot((float) heading);

        double dist = Math.hypot(getX() - before.x, getZ() - before.z);
        updateSlope(getY() - before.y, dist);
        if (engine) tank.consume(dist, def.fuelPer100Blocks());
        odometer.add(dist);
        syncData();
    }

    /** Quanto un blocco può essere alto sopra i piedi perché la ruota lo salga invece di fermare la scocca. */
    private static final double BODY_STEP_UP = 0.6;
    /** Spinta massima per tick: se il veicolo nasce dentro un muro non scatta via di colpo. */
    private static final double BODY_MAX_PUSH = 0.5;
    /** Gravità in blocchi/tick², la stessa di giocatori e creature: senza, il veicolo resterebbe sospeso nel vuoto. */
    private static final double GRAVITY = 0.08;
    /**
     * Strada massima (blocchi) per passo di movimento. Oltre si divide il tick in più passi: la scocca è più lunga
     * della scatola dell'entità e a velocità alta il muso entrerebbe nei muri prima del controllo.
     */
    private static final double MAX_SUBSTEP = 0.5;
    private static final int MAX_SUBSTEPS = 4;
    /** Sotto queste velocità (blocchi/tick, rad/s) il veicolo è fermo: non rifà la collisione della scocca ogni tick. */
    private static final double REST_SPEED = 1e-4;
    private static final double REST_YAW = 1e-4;
    /** Raggio (blocchi) oltre la scatola dell'entità in cui cercare altri veicoli da urtare. */
    private static final double VEHICLE_SCAN = 4.0;
    /** L'ultima collisione della scocca non ha toccato niente: da fermi si può saltare. */
    private boolean bodyClear;

    @Override
    protected double getDefaultGravity() {
        return GRAVITY;
    }

    /** Le ruote salgono mezzi blocchi e lastre (non un blocco intero), come la scocca in {@link #BODY_STEP_UP}. */
    @Override
    public float maxUpStep() {
        return (float) BODY_STEP_UP;
    }

    /**
     * Muove il veicolo della velocità data (blocchi/tick) contro blocchi e scocca, in più passi se è veloce.
     * Restituisce la velocità che resta: una componente fermata da un muro si perde, così non si spinge contro.
     * Con un solo passo è esattamente il movimento di prima. {@code resting}: fermo e libero, la scocca non si ricontrolla.
     */
    private Vec3 moveWithBody(double vx, double vy, double vz, boolean resting) {
        int steps = Math.max(1, Math.min(MAX_SUBSTEPS, (int) Math.ceil(Math.hypot(vx, vz) / MAX_SUBSTEP)));
        for (int i = 0; i < steps; i++) {
            setDeltaMovement(vx, vy, vz);
            move(MoverType.SELF, new Vec3(vx / steps, vy / steps, vz / steps));
            Vec3 dm = getDeltaMovement();
            if (i == steps - 1) {
                // L'ultimo passo prende anche il rallentamento del blocco (sabbia delle anime, miele) una volta sola.
                vx = dm.x;
                vz = dm.z;
            } else {
                if (dm.x == 0) vx = 0;
                if (dm.z == 0) vz = 0;
            }
            vy = dm.y;
            if (resting) continue;
            // La scocca vera è lunga e ruota: contro i muri la ferma lei, non la scatola fissa dell'entità.
            double[] push = resolveBodyCollision();
            bodyClear = push == null;
            if (push != null) {
                double into = vx * push[0] + vz * push[1];
                if (into < 0) {
                    vx -= push[0] * into;
                    vz -= push[1] * into;
                }
            }
        }
        setDeltaMovement(vx, vy, vz);
        return new Vec3(vx, vy, vz);
    }

    /**
     * Separa la scocca da quella degli altri veicoli vicini e scambia la quantità di moto con un urto anelastico: chi
     * tampona rallenta, chi è tamponato parte (e il suo freno, se tirato, lo ferma nei tick successivi). Restituisce
     * la velocità {vx, vz} di questo veicolo dopo gli urti.
     */
    private double[] resolveVehicleCollisions(double vx, double vz) {
        var h = hitboxes();
        if (!h.hasSolid()) return new double[] {vx, vz};
        double m1 = def.powertrain().massKg();
        for (QrmVehicleEntity other : level().getEntitiesOfClass(QrmVehicleEntity.class,
                getBoundingBox().inflate(VEHICLE_SCAN), o -> o != this && !o.isRemoved())) {
            var oh = other.hitboxes();
            if (!oh.hasSolid()) continue;
            double[] p = h.bodyPush(getX(), getY(), getZ(), getYRot(), oh, other.getX(), other.getY(), other.getZ(),
                    other.getYRot());
            if (p == null) continue;
            double m2 = other.def.powertrain().massKg();
            double len = Math.hypot(p[0], p[1]);
            double k = Math.min(1, BODY_MAX_PUSH / Math.max(len, 1e-9));
            double share = dev.qrm.vehicles.logic.VehicleCollision.separationShare(m1, m2);
            setPos(getX() + p[0] * k * share, getY(), getZ() + p[1] * k * share);
            other.setPos(other.getX() - p[0] * k * (1 - share), other.getY(), other.getZ() - p[1] * k * (1 - share));
            double[] dv = dev.qrm.vehicles.logic.VehicleCollision.impulse(m1, vx, vz, m2, other.drive.vx(),
                    other.drive.vz(), p[2], p[3], dev.qrm.vehicles.logic.VehicleCollision.RESTITUTION);
            if (dv != null) {
                vx += dv[0];
                vz += dv[1];
                other.receiveImpulse(dv[2], dv[3]);
            } else {
                other.bodyClear = false;
            }
        }
        return new double[] {vx, vz};
    }

    /** Un altro veicolo ci ha urtati: la velocità cambia e al prossimo tick la scocca si ricontrolla contro i blocchi. */
    private void receiveImpulse(double dvx, double dvz) {
        drive = drive.withMotion(drive.heading(), drive.vx() + dvx, drive.vz() + dvz);
        bodyClear = false;
    }

    /**
     * Spinge fuori dai blocchi la scocca disegnata nel modello (le scatole {@code hitbox}, ruotate col muso).
     * Restituisce la direzione dell'ultima spinta, o {@code null} se non ha toccato niente.
     */
    private double @Nullable [] resolveBodyCollision() {
        var h = hitboxes();
        if (!h.hasSolid()) return null;
        AABB area = new AABB(getX() - 4.5, getY() - 0.5, getZ() - 4.5, getX() + 4.5, getY() + 3, getZ() + 4.5);
        List<double[]> obstacles = new java.util.ArrayList<>();
        for (net.minecraft.world.phys.shapes.VoxelShape shape : level().getBlockCollisions(this, area)) {
            for (AABB a : shape.toAabbs()) obstacles.add(new double[] {a.minX, a.minY, a.minZ, a.maxX, a.maxY, a.maxZ});
        }
        if (obstacles.isEmpty()) return null;
        double[] last = null;
        double total = 0;
        for (int i = 0; i < 4 && total < BODY_MAX_PUSH; i++) {
            double[] p = h.bodyPush(getX(), getY(), getZ(), getYRot(), obstacles, BODY_STEP_UP);
            if (p == null) break;
            double len = Math.hypot(p[0], p[1]);
            double k = Math.min(1, (BODY_MAX_PUSH - total) / Math.max(len, 1e-9));
            setPos(getX() + p[0] * k, getY(), getZ() + p[1] * k);
            total += len * k;
            last = new double[] {p[2], p[3]};
        }
        return last;
    }

    /** Copia lo stato del server nei dati sincronizzati, arrotondato perché non cambi a ogni tick. */
    private void syncData() {
        entityData.set(FUEL, (float) (Math.round(tank.level() * 10) / 10.0));
        entityData.set(ODOMETER_BLOCKS, (float) (Math.floor(odometer.blocks() / 10) * 10));
        entityData.set(SPEED, (float) (Math.round(drive.forwardSpeed() * 200) / 200.0));
        entityData.set(STEER, (float) (Math.round(drive.steerAngle() * 2) / 2.0));
        entityData.set(GEAR, selector);
        entityData.set(RPM, (float) (Math.round(drive.rpm() / 100) * 100));
    }

    /** Pendenza filtrata del terreno nel verso del muso (seno, positiva in salita). */
    private double slope;

    private static TagKey<Block> surfaceTag(String id) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(VehiclesMod.MOD_ID, "surface/" + id));
    }

    private static final TagKey<Block> ICE_TAG = surfaceTag("ice");
    private static final TagKey<Block> SNOW_TAG = surfaceTag("snow");
    private static final TagKey<Block> SAND_TAG = surfaceTag("sand");
    private static final TagKey<Block> DIRT_TAG = surfaceTag("dirt");

    /** Il fondo sotto le ruote, dai tag dei blocchi (dal più scivoloso al più aderente) e dai valori configurati. */
    /**
     * Pendenza lungo il muso, dalla salita effettivamente fatta nell'ultimo tick (con un filtro per non reagire ai
     * gradini). Fuori da terra o fermi si smorza verso zero.
     */
    private void updateSlope(double dy, double dist) {
        if (onGround() && dist > 0.02) {
            double raw = dy / dist * Math.signum(drive.forwardSpeed());
            double limit = DrivingModel.MAX_SLOPE;
            slope += (Math.max(-limit, Math.min(limit, raw)) - slope) * 0.25;
        } else if (!onGround()) {
            slope *= 0.8;
        }
    }

    private Surface surfaceUnder() {
        BlockState s = level().getBlockState(getOnPos());
        if (s.is(ICE_TAG)) return Surface.ICE.current();
        if (s.is(SNOW_TAG)) return Surface.SNOW.current();
        if (s.is(SAND_TAG)) return Surface.SAND.current();
        if (s.is(DIRT_TAG)) return Surface.DIRT.current();
        return Surface.PAVED.current();
    }

    // ---- sedili ----

    private EntityDataAccessor<Integer> seatSlot(int i) {
        return i == 0 ? SEAT_0 : SEAT_1;
    }

    /** Il posto occupato da un passeggero (0 = guidatore); se i dati non sono ancora arrivati, per ordine di salita. */
    private int seatOf(Entity passenger) {
        int id = passenger.getId();
        for (int i = 0; i < Math.min(SYNCED_SEATS, def.seats()); i++) {
            if (entityData.get(seatSlot(i)) == id) return i;
        }
        return Math.max(0, Math.min(getPassengers().indexOf(passenger), def.seats() - 1));
    }

    /** Il posto è libero? Un id rimasto nei dati di un passeggero sceso non conta. */
    private boolean seatFree(int seat) {
        if (seat < 0 || seat >= def.seats()) return false;
        if (seat >= SYNCED_SEATS) return true;
        int id = entityData.get(seatSlot(seat));
        for (Entity p : getPassengers()) if (p.getId() == id) return false;
        return true;
    }

    private int freeSeat() {
        for (int i = 0; i < def.seats(); i++) if (seatFree(i)) return i;
        return def.seats() - 1;
    }

    /** Posto richiesto da chi sta salendo (solo server), preso da {@link #addPassenger}. */
    private final Map<UUID, Integer> wantedSeat = new HashMap<>();

    private void assignSeat(Entity passenger, int seat) {
        for (int i = 0; i < Math.min(SYNCED_SEATS, def.seats()); i++) {
            if (entityData.get(seatSlot(i)) == passenger.getId()) entityData.set(seatSlot(i), -1);
        }
        if (seat < SYNCED_SEATS) entityData.set(seatSlot(seat), passenger.getId());
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if (level().isClientSide()) return;
        Integer wanted = wantedSeat.remove(passenger.getUUID());
        assignSeat(passenger, wanted != null && seatFree(wanted) ? wanted : freeSeat());
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (level().isClientSide()) return;
        for (int i = 0; i < Math.min(SYNCED_SEATS, def.seats()); i++) {
            if (entityData.get(seatSlot(i)) == passenger.getId()) entityData.set(seatSlot(i), -1);
        }
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof Player && getPassengers().size() < def.seats();
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        for (Entity e : getPassengers()) {
            if (seatOf(e) == 0 && e instanceof LivingEntity living) return living;
        }
        return null;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        List<float[]> layout = VehicleSeats.of(def);
        int seat = Math.min(seatOf(passenger), layout.size() - 1);
        float[] p = layout.get(seat);
        float x = p[0];
        float y = p[1];
        float z = p[2];
        // Solo sul client e solo per la visuale del giocatore locale: {destra, su, avanti} (il davanti è -Z).
        float[] view = level().isClientSide() ? VIEW_OFFSET.apply(passenger) : null;
        if (view != null) {
            x += view[0];
            y += view[1];
            z -= view[2];
        }
        double[] w = SeatMath.toWorld(x / 16.0, z / 16.0, getYRot());
        return new Vec3(w[0], y / 16.0, w[1]);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        double side = seatOf(passenger) == 0 ? -1 : 1;
        for (double lateral : new double[] {side * 2.0, -side * 2.0}) {
            double[] w = SeatMath.toWorld(lateral, 0, getYRot());
            BlockPos target = BlockPos.containing(getX() + w[0], getY(), getZ() + w[1]);
            Vec3 safe = DismountHelper.findSafeDismountLocation(passenger.getType(), level(), target, false);
            if (safe != null) return safe;
        }
        return super.getDismountLocationForPassenger(passenger);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (!boardsFromAnywhere()) return InteractionResult.PASS;
        if (player.isSecondaryUseActive() || getPassengers().size() >= def.seats()) return InteractionResult.PASS;
        if (level().isClientSide()) return InteractionResult.SUCCESS;
        return player.startRiding(this) ? InteractionResult.SUCCESS_SERVER : InteractionResult.PASS;
    }

    // ---- fisica dell'entità ----

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false; // invulnerabile: i danni da incidente sono fuori dalla fase 1
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    /** Le hitbox disegnate nel modello (vuote se il modello non ne ha). */
    public dev.qrm.vehicles.logic.VehicleHitboxes hitboxes() {
        return VehicleHitboxFiles.of(def.id());
    }

    /**
     * Con un ingombro disegnato nel modello la collisione col giocatore la fa quello (ruota col muso); la scatola
     * fissa dell'entità, che non ruota, non deve fare da muro in più.
     */
    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return !hitboxes().hasSolid();
    }

    /** Si sale cliccando ovunque sul veicolo solo se il modello non ha zone per i sedili. */
    private boolean boardsFromAnywhere() {
        return hitboxes().zones().keySet().stream().noneMatch(z -> z.startsWith("hit_seat"));
    }

    /**
     * Cosa succede usando una zona del modello (dal server). I sedili {@code hit_seat*} fanno salire; le altre zone
     * per ora si illuminano soltanto.
     */
    public void useZone(Player player, String zone, boolean hold) {
        if (zone.startsWith("hit_seat")) {
            if (hold) return;
            takeSeat(player, seatOfZone(zone));
            return;
        }
        if (zone.equals("hit_ignition_key")) {
            // Tenuto premuto il blocchetto gira (accende o spegne); un clic inserisce o toglie la chiave.
            if (!hold) toggleKey();
            else if (keyInserted()) toggleEngine();
            return;
        }
        if (hold) return;
        if (zone.equals("hit_gear_shifter")) {
            cycleShifter();
            return;
        }
        int bit = VehicleParts.bitOfZone(zone);
        if (bit == VehicleParts.PARKING_BRAKE) {
            toggleParkingBrake();
        } else if (bit != 0) {
            togglePart(bit);
        } else if (zone.equals("hit_light_switch")) {
            toggleLights();
        }
    }

    /**
     * Il posto più vicino al centro di una zona {@code hit_seat*}, tra quelli liberi (o quello già occupato dal
     * giocatore stesso); a parità, il passeggero (il sedile posteriore sta in mezzo). Senza liberi, -1.
     */
    private int seatOfZone(String zone) {
        double[] c = hitboxes().center(zone);
        List<float[]> layout = VehicleSeats.of(def);
        if (c == null) return freeSeat();
        int best = -1;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < Math.min(def.seats(), layout.size()); i++) {
            double dx = layout.get(i)[0] / 16.0 - c[0];
            double dz = layout.get(i)[2] / 16.0 - c[2];
            double d = dx * dx + dz * dz;
            if (d <= bestD + 1e-9) {
                best = i;
                bestD = d;
            }
        }
        return best;
    }

    /** Sale su un posto preciso o, se il giocatore è già a bordo, si sposta; un posto occupato non si prende. */
    private void takeSeat(Player player, int seat) {
        if (seat < 0) return;
        if (player.getVehicle() == this) {
            if (seatOf(player) != seat && seatFree(seat)) assignSeat(player, seat);
            return;
        }
        if (!seatFree(seat)) return;
        wantedSeat.put(player.getUUID(), seat);
        if (!board(player)) wantedSeat.remove(player.getUUID());
    }

    /** Sale a bordo (dal server). Restituisce se è riuscito. */
    public boolean board(Player player) {
        if (getPassengers().size() >= def.seats() || player.isSecondaryUseActive()) return false;
        return player.startRiding(this);
    }

    // ---- salvataggio ----

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        out.putString("def", def.id());
        out.putDouble("fuel", tank.level());
        out.putDouble("odometer_blocks", odometer.blocks());
        out.putBoolean("lights", lightsOn());
        out.putInt("parts", parts());
        out.putInt("gear", selector);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        VehicleSaveData data = VehicleSaveData.of(def, optional(in, "fuel"), optional(in, "odometer_blocks"));
        tank = new FuelTank(def.fuelCapacity(), data.fuel());
        odometer = new Odometer(data.odometerBlocks());
        entityData.set(LIGHTS, in.getBooleanOr("lights", false));
        entityData.set(PARTS, in.getIntOr("parts", 0));
        selector = Math.max(-1, Math.min(def.powertrain().gears(), in.getIntOr("gear", 0)));
        engineStatus = EngineStatus.OFF; // dopo un caricamento il motore è spento
        entityData.set(ENGINE_STATE, EngineState.OFF.id());
        syncData();
    }

    private static OptionalDouble optional(ValueInput in, String key) {
        return in.read(key, Codec.DOUBLE).map(OptionalDouble::of).orElseGet(OptionalDouble::empty);
    }
}
