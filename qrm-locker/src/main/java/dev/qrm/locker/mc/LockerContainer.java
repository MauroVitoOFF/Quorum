package dev.qrm.locker.mc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.qrm.api.point.InteractionPoint;
import dev.qrm.locker.logic.LockerFlow;
import dev.qrm.locker.logic.LockerSlots;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;

/** I 27 slot di un armadietto aperto: ogni modifica lo segna da scrivere, e smette di valere se l'accesso si perde. */
final class LockerContainer extends SimpleContainer {
    private static final double MAX_DISTANCE_SQR = 8.0 * 8.0;

    private final LockerFlow.Session session;
    private final List<JsonObject> unreadable = new ArrayList<>();

    LockerContainer(LockerFlow.Session session) {
        super(LockerSlots.SIZE);
        this.session = session;
    }

    void keepUnreadable(int slot, JsonElement item) {
        JsonObject entry = new JsonObject();
        entry.addProperty("slot", slot);
        entry.add("item", item);
        unreadable.add(entry);
    }

    List<JsonObject> unreadable() {
        return unreadable;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        session.markDirty();
    }

    @Override
    public boolean stillValid(Player player) {
        return session.stillValid(isNear(player, session.point()));
    }

    static boolean isNear(Player player, InteractionPoint point) {
        var pos = point.position();
        if (!player.level().dimension().identifier().toString().equals(pos.dimension())) return false;
        return player.distanceToSqr(pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5) < MAX_DISTANCE_SQR;
    }
}
