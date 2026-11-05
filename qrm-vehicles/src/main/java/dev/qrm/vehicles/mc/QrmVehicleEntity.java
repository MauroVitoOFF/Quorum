package dev.qrm.vehicles.mc;

import com.mojang.serialization.Codec;
import dev.qrm.vehicles.logic.DriveInput;
import dev.qrm.vehicles.logic.DriveState;
import dev.qrm.vehicles.logic.DrivingModel;
import dev.qrm.vehicles.logic.FuelTank;
import dev.qrm.vehicles.logic.Odometer;
import dev.qrm.vehicles.logic.SeatMath;
import dev.qrm.vehicles.logic.Surface;
import dev.qrm.vehicles.logic.VehicleDef;
import dev.qrm.vehicles.logic.VehicleMotionFx;
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
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Un veicolo guidabile. Il server è autorevole: simula la guida con {@link DrivingModel} a partire dall'ultimo input
 * del guidatore e la posizione arriva ai client con la normale sincronizzazione delle entità. Carburante e
 * chilometri vivono nei campi del server e sono copiati, arrotondati, nei dati sincronizzati per HUD e rendering.
 */
public final class QrmVehicleEntity extends Entity {

    private static final EntityDataAccessor<Boolean> ENGINE =
            SynchedEntityData.defineId(QrmVehicleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> LIGHTS =
            SynchedEntityData.defineId(QrmVehicleEntity.class, EntityDataSerializers.BOOLEAN);
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

    /** Oltre questi tick senza input il guidatore è come se avesse lasciato i comandi. */
    private static final int INPUT_TIMEOUT_TICKS = 5;
    private static final int HONK_COOLDOWN_TICKS = 6;

    private final VehicleDef def = VehicleEntities.defOf(getType());
    private final InterpolationHandler interpolation = new InterpolationHandler(this, 3);
    private FuelTank tank = new FuelTank(def.fuelCapacity(), def.fuelCapacity());
    private Odometer odometer = new Odometer(0);
    private final Map<UUID, Integer> seats = new HashMap<>();
    private final VehicleMotionFx fx = new VehicleMotionFx();
    /**
     * Impostato dal client: scostamento {destra, su, avanti} della visuale del giocatore locale in prima persona, in
     * unità del modello; null per tutti gli altri. Sul server non viene mai interrogato.
     */
    public static volatile java.util.function.Function<Entity, float[]> VIEW_OFFSET = e -> null;

    private DriveState drive = DriveState.rest(0);
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
        builder.define(ENGINE, false);
        builder.define(LIGHTS, false);
        // Chiamato dal costruttore di Entity, prima dei campi di questa classe: il tipo però c'è già.
        builder.define(FUEL, (float) VehicleEntities.defOf(getType()).fuelCapacity());
        builder.define(ODOMETER_BLOCKS, 0f);
        builder.define(SPEED, 0f);
        builder.define(STEER, 0f);
        builder.define(GEAR, 1);
        builder.define(RPM, 0f);
    }

    public VehicleDef def() {
        return def;
    }

    /** Effetti di movimento (ruote, assetto): vivono solo sul client. */
    public VehicleMotionFx motionFx() {
        return fx;
    }

    public boolean engineOn() {
        return entityData.get(ENGINE);
    }

    public boolean lightsOn() {
        return entityData.get(LIGHTS);
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
        if (engineOn()) entityData.set(ENGINE, false);
        else if (!tank.isEmpty()) entityData.set(ENGINE, true);
    }

    public void toggleLights() {
        entityData.set(LIGHTS, !lightsOn());
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
            fx.tick(speed(), steerAngle(), def.maxSpeed(), def.maxSteerDeg());
            return;
        }
        serverTick();
    }

    private void serverTick() {
        if (!headingInitialised) {
            // Prima volta dopo la creazione o il caricamento: il muso resta dov'era, non torna a zero.
            drive = DriveState.rest(getYRot());
            headingInitialised = true;
        }
        ticksSinceInput++;
        LivingEntity driver = getControllingPassenger();
        DriveInput in = driver != null && ticksSinceInput <= INPUT_TIMEOUT_TICKS ? input : DriveInput.NONE;

        boolean engine = engineOn();
        if (engine && tank.isEmpty()) engine = false;
        boolean swimming = isInWater() || isInLava();
        if (swimming) {
            in = DriveInput.NONE;
            engine = false;
        }
        if (engine != engineOn()) entityData.set(ENGINE, engine);

        drive = DrivingModel.step(def, drive, in, surfaceUnder(), engine, slope);
        if (swimming) drive = drive.withMotion(drive.heading(), 0, 0);

        double vy = isNoGravity() ? 0 : (getDeltaMovement().y - getGravity()) * 0.98;
        Vec3 before = position();
        setDeltaMovement(drive.vx(), vy, drive.vz());
        move(MoverType.SELF, getDeltaMovement());
        // Se i blocchi hanno fermato una componente, la velocità del modello la perde: niente spinta contro i muri.
        Vec3 after = getDeltaMovement();
        double heading = ((drive.heading() % 360) + 360) % 360;
        drive = drive.withMotion(heading, after.x, after.z);
        setYRot((float) heading);

        double dist = Math.hypot(getX() - before.x, getZ() - before.z);
        updateSlope(getY() - before.y, dist);
        if (engine) tank.consume(dist, def.fuelPer100Blocks());
        odometer.add(dist);
        syncData();
    }

    /** Copia lo stato del server nei dati sincronizzati, arrotondato perché non cambi a ogni tick. */
    private void syncData() {
        entityData.set(FUEL, (float) (Math.round(tank.level() * 10) / 10.0));
        entityData.set(ODOMETER_BLOCKS, (float) (Math.floor(odometer.blocks() / 10) * 10));
        entityData.set(SPEED, (float) (Math.round(drive.forwardSpeed() * 200) / 200.0));
        entityData.set(STEER, (float) (Math.round(drive.steerAngle() * 2) / 2.0));
        entityData.set(GEAR, drive.gear());
        entityData.set(RPM, (float) (Math.round(drive.rpm() / 100) * 100));
    }

    /** Pendenza filtrata del terreno nel verso del muso (seno, positiva in salita). */
    private double slope;

    private static TagKey<Block> surfaceTag(String id) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(VehiclesMod.MOD_ID, "surface/" + id));
    }

    private static final TagKey<Block> SLIPPERY_TAG = surfaceTag("slippery");
    private static final TagKey<Block> ROUGH_TAG = surfaceTag("rough");
    private static final TagKey<Block> DIRT_TAG = surfaceTag("dirt");

    /** Il fondo sotto le ruote, dai tag dei blocchi (il piu' scivoloso vince) e dai valori configurati. */
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
        if (s.is(SLIPPERY_TAG)) return Surface.SLIPPERY.current();
        if (s.is(ROUGH_TAG)) return Surface.ROUGH.current();
        if (s.is(DIRT_TAG)) return Surface.DIRT.current();
        return Surface.PAVED.current();
    }

    // ---- sedili ----

    private int freeSeat() {
        for (int i = 0; i < def.seats(); i++) if (!seats.containsValue(i)) return i;
        return def.seats() - 1;
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        seats.put(passenger.getUUID(), freeSeat());
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        seats.remove(passenger.getUUID());
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof Player && getPassengers().size() < def.seats();
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        for (Entity e : getPassengers()) {
            if (seats.getOrDefault(e.getUUID(), -1) == 0 && e instanceof LivingEntity living) return living;
        }
        return null;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        List<float[]> layout = VehicleSeats.of(def);
        int seat = Math.min(seats.getOrDefault(passenger.getUUID(), 0), layout.size() - 1);
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
        double side = seats.getOrDefault(passenger.getUUID(), 0) == 0 ? -1 : 1;
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

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return true;
    }

    // ---- salvataggio ----

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        out.putString("def", def.id());
        out.putDouble("fuel", tank.level());
        out.putDouble("odometer_blocks", odometer.blocks());
        out.putBoolean("lights", lightsOn());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        VehicleSaveData data = VehicleSaveData.of(def, optional(in, "fuel"), optional(in, "odometer_blocks"));
        tank = new FuelTank(def.fuelCapacity(), data.fuel());
        odometer = new Odometer(data.odometerBlocks());
        entityData.set(LIGHTS, in.getBooleanOr("lights", false));
        entityData.set(ENGINE, false); // dopo un caricamento il motore è spento
        syncData();
    }

    private static OptionalDouble optional(ValueInput in, String key) {
        return in.read(key, Codec.DOUBLE).map(OptionalDouble::of).orElseGet(OptionalDouble::empty);
    }
}
