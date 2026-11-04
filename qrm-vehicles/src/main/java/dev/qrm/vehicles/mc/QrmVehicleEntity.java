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
import dev.qrm.vehicles.logic.VehicleSaveData;
import dev.qrm.vehicles.logic.VehicleSeats;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
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
import net.minecraft.world.level.block.Blocks;
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

    /** Oltre questi tick senza input il guidatore è come se avesse lasciato i comandi. */
    private static final int INPUT_TIMEOUT_TICKS = 5;

    private final VehicleDef def = VehicleDef.JEEP;
    private final InterpolationHandler interpolation = new InterpolationHandler(this, 3);
    private FuelTank tank = new FuelTank(def.fuelCapacity(), def.fuelCapacity());
    private Odometer odometer = new Odometer(0);
    private final Map<UUID, Integer> seats = new HashMap<>();
    private DriveState drive = DriveState.rest(0);
    private DriveInput input = DriveInput.NONE;
    private int ticksSinceInput = INPUT_TIMEOUT_TICKS;

    public QrmVehicleEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    // ---- dati sincronizzati (leggibili anche dal client) ----

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ENGINE, false);
        builder.define(LIGHTS, false);
        builder.define(FUEL, (float) VehicleDef.JEEP.fuelCapacity());
        builder.define(ODOMETER_BLOCKS, 0f);
        builder.define(SPEED, 0f);
        builder.define(STEER, 0f);
    }

    public VehicleDef def() {
        return def;
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

    @Override
    public InterpolationHandler getInterpolation() {
        return interpolation;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            interpolation.interpolate();
            return;
        }
        serverTick();
    }

    private void serverTick() {
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

        drive = DrivingModel.step(def, drive, in, surfaceUnder(), engine);
        if (swimming) drive = new DriveState(drive.heading(), drive.steerAngle(), 0, 0);

        double vy = isNoGravity() ? 0 : (getDeltaMovement().y - getGravity()) * 0.98;
        Vec3 before = position();
        setDeltaMovement(drive.vx(), vy, drive.vz());
        move(MoverType.SELF, getDeltaMovement());
        // Se i blocchi hanno fermato una componente, la velocità del modello la perde: niente spinta contro i muri.
        Vec3 after = getDeltaMovement();
        double heading = ((drive.heading() % 360) + 360) % 360;
        drive = new DriveState(heading, drive.steerAngle(), after.x, after.z);
        setYRot((float) heading);

        double dist = Math.hypot(getX() - before.x, getZ() - before.z);
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
    }

    private Surface surfaceUnder() {
        BlockState s = level().getBlockState(getOnPos());
        if (s.is(BlockTags.SAND) || s.is(BlockTags.SNOW) || s.is(BlockTags.MUD) || s.is(Blocks.SNOW_BLOCK)
                || s.is(Blocks.SOUL_SAND)) {
            return Surface.ROUGH;
        }
        if (s.is(BlockTags.DIRT) || s.is(Blocks.GRAVEL) || s.is(Blocks.FARMLAND) || s.is(Blocks.DIRT_PATH)) {
            return Surface.DIRT;
        }
        return Surface.PAVED;
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
        int seat = Math.min(seats.getOrDefault(passenger.getUUID(), 0), VehicleSeats.JEEP.size() - 1);
        float[] p = VehicleSeats.JEEP.get(seat);
        double[] w = SeatMath.toWorld(p[0] / 16.0, p[2] / 16.0, getYRot());
        return new Vec3(w[0], p[1] / 16.0, w[1]);
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
