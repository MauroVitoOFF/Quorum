package dev.qrm.immersion.mc;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Il "sedile": un'entita' invisibile, senza gravita' e senza ingombro, su cui un giocatore si siede.
 * Sparisce appena nessuno la cavalca, oppure se il blocco sotto non e' piu' una scala o una lastra.
 */
public final class SeatEntity extends Entity {
    public SeatEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;
        if (getPassengers().isEmpty() || !isSeatBlock(level().getBlockState(blockPosition().below()))
                && !isSeatBlock(level().getBlockState(blockPosition()))) {
            discard();
        }
    }

    /** Dove si trovava chi si e' seduto: solo sul server, non serve salvarla (il sedile non sopravvive al riavvio). */
    private net.minecraft.world.phys.Vec3 returnPos;

    void setReturnPos(net.minecraft.world.phys.Vec3 pos) {
        this.returnPos = pos;
    }

    /** Ci si alza dove ci si era seduti, se il posto e' ancora libero; altrimenti vale la discesa vanilla. */
    @Override
    public net.minecraft.world.phys.Vec3 getDismountLocationForPassenger(net.minecraft.world.entity.LivingEntity passenger) {
        if (returnPos != null && level().noCollision(passenger, passenger.getBoundingBox()
                .move(returnPos.subtract(passenger.position())))) {
            return returnPos;
        }
        return super.getDismountLocationForPassenger(passenger);
    }

    static boolean isSeatBlock(BlockState state) {
        return state.getBlock() instanceof StairBlock || state.getBlock() instanceof SlabBlock;
    }

    /** Chi scende (con Shift) fa sparire il sedile subito. */
    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!level().isClientSide() && getPassengers().isEmpty()) discard();
    }

    /**
     * Sulle scale il passeggero non gira oltre lo schienale; su una lastra non c'e' schienale, quindi puo'
     * voltarsi liberamente (altrimenti, seduto di fronte a un muro, non vedrebbe mai il lato libero).
     * Il tipo di blocco si legge dal mondo: cosi' la regola vale uguale su server e client.
     */
    @Override
    public void onPassengerTurned(Entity passenger) {
        if (level().getBlockState(blockPosition()).getBlock() instanceof StairBlock
                || level().getBlockState(blockPosition().below()).getBlock() instanceof StairBlock) {
            clampRotation(passenger);
        }
    }

    private void clampRotation(Entity passenger) {
        passenger.setYBodyRot(getYRot());
        float diff = Mth.wrapDegrees(passenger.getYRot() - getYRot());
        float clamped = Mth.clamp(diff, -105.0F, 105.0F);
        passenger.yRotO += clamped - diff;
        passenger.setYRot(passenger.getYRot() + clamped - diff);
        passenger.setYHeadRot(passenger.getYRot());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {}

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}
}
