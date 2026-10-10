package dev.qrm.locker.mc;

import dev.qrm.api.QRM;
import dev.qrm.api.point.PointOwner;
import dev.qrm.api.point.PointType;
import dev.qrm.api.point.PointTypeRegistry;
import dev.qrm.locker.net.LockerNetwork;
import java.util.Set;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Armadietto personale per Character, sui punti di interazione di tipo {@code locker}. */
@Mod(LockerMod.MOD_ID)
public final class LockerMod {
    public static final String MOD_ID = "qrm_locker";
    public static final String POINT_TYPE = "locker";

    public LockerMod(IEventBus modBus) {
        modBus.addListener(LockerNetwork::register);
        QRM.get(PointTypeRegistry.class).register(new PointType(POINT_TYPE, "qrm_locker.point.locker",
                Set.of(PointOwner.Kind.JOB, PointOwner.Kind.ORGANIZATION), false, LockerMenus::open));
        NeoForge.EVENT_BUS.addListener(LockerMod::onTick);
        NeoForge.EVENT_BUS.addListener(LockerMod::onStopping);
        NeoForge.EVENT_BUS.addListener(LockerMod::onStopped);
    }

    private static void onTick(ServerTickEvent.Post event) {
        LockerMenus.flow().flushDirty();
    }

    private static void onStopping(ServerStoppingEvent event) {
        LockerMenus.flow().closeAll();
    }

    private static void onStopped(ServerStoppedEvent event) {
        LockerMenus.reset();
    }
}
