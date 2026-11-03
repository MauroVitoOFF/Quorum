package dev.qrm.arrival.mc;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Primo arrivo: un Character nuovo si risveglia a terra in un vicolo della città. */
@Mod(ArrivalMod.MOD_ID)
public final class ArrivalMod {
    public static final String MOD_ID = "qrm_arrival";
    static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    public ArrivalMod(IEventBus modBus) {}
}
