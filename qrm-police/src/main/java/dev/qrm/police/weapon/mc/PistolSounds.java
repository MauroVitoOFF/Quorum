package dev.qrm.police.weapon.mc;

import dev.qrm.police.mc.PoliceMod;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Eventi sonori della pistola di servizio. Gli id coincidono con quelli degli eventi {@code sound} delle animazioni
 * e con le chiavi di {@code sounds.json}; i suoni veri sono segnaposto vanilla finche' non arrivano i file .ogg
 * (vedi AUDIO_SOURCES.md).
 */
public final class PistolSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, PoliceMod.MOD_ID);

    private static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> BY_ID = new LinkedHashMap<>();

    public static final DeferredHolder<SoundEvent, SoundEvent> PISTOL_FIRE = register("pistol.fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> PISTOL_DRY_FIRE = register("pistol.dry_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> PISTOL_MISFIRE_CLICK = register("pistol.misfire_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> PISTOL_JAM_CLUNK = register("pistol.jam_clunk");
    public static final DeferredHolder<SoundEvent, SoundEvent> PISTOL_DRAW = register("pistol.draw");
    public static final DeferredHolder<SoundEvent, SoundEvent> PISTOL_DRAW_SETTLE = register("pistol.draw_settle");
    public static final DeferredHolder<SoundEvent, SoundEvent> PISTOL_HOLSTER = register("pistol.holster");
    public static final DeferredHolder<SoundEvent, SoundEvent> PISTOL_MAG_RELEASE = register("pistol.mag_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> PISTOL_MAG_OUT = register("pistol.mag_out");
    public static final DeferredHolder<SoundEvent, SoundEvent> PISTOL_MAG_IN = register("pistol.mag_in");
    public static final DeferredHolder<SoundEvent, SoundEvent> PISTOL_MAG_PEEK = register("pistol.mag_peek");
    public static final DeferredHolder<SoundEvent, SoundEvent> PISTOL_MAG_TAP = register("pistol.mag_tap");
    public static final DeferredHolder<SoundEvent, SoundEvent> PISTOL_MAG_INSPECT = register("pistol.mag_inspect");
    public static final DeferredHolder<SoundEvent, SoundEvent> PISTOL_PRESS_CHECK = register("pistol.press_check");
    public static final DeferredHolder<SoundEvent, SoundEvent> PISTOL_RACK_BACK = register("pistol.rack_back");
    public static final DeferredHolder<SoundEvent, SoundEvent> PISTOL_SLIDE_FORWARD = register("pistol.slide_forward");
    public static final DeferredHolder<SoundEvent, SoundEvent> PISTOL_SLIDE_LOCK = register("pistol.slide_lock");
    public static final DeferredHolder<SoundEvent, SoundEvent> PISTOL_SLIDE_RELEASE = register("pistol.slide_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> PISTOL_SLIDE_RACK = register("pistol.slide_rack");
    public static final DeferredHolder<SoundEvent, SoundEvent> PISTOL_SHELL_DROP = register("pistol.shell_drop");
    public static final DeferredHolder<SoundEvent, SoundEvent> PISTOL_BULLET_LOAD = register("pistol.bullet_load");
    public static final DeferredHolder<SoundEvent, SoundEvent> IMPACT_METAL = register("impact.metal");
    public static final DeferredHolder<SoundEvent, SoundEvent> IMPACT_WOOD = register("impact.wood");
    public static final DeferredHolder<SoundEvent, SoundEvent> IMPACT_STONE = register("impact.stone");
    public static final DeferredHolder<SoundEvent, SoundEvent> IMPACT_SOFT = register("impact.soft");

    private PistolSounds() {}

    private static DeferredHolder<SoundEvent, SoundEvent> register(String id) {
        DeferredHolder<SoundEvent, SoundEvent> holder = SOUNDS.register(id,
                () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(PoliceMod.MOD_ID, id)));
        BY_ID.put(id, holder);
        return holder;
    }

    /** Il suono con l'id usato da un evento di animazione (es. {@code pistol.fire}), se esiste. */
    public static Optional<Holder<SoundEvent>> byId(String id) {
        return Optional.ofNullable(BY_ID.get(id)).map(h -> (Holder<SoundEvent>) h);
    }
}
