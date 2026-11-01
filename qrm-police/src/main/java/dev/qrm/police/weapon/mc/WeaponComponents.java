package dev.qrm.police.weapon.mc;

import dev.qrm.police.mc.PoliceMod;
import dev.qrm.police.weapon.logic.MagazineState;
import dev.qrm.police.weapon.logic.WeaponDef;
import dev.qrm.police.weapon.logic.WeaponState;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Data component della pistola e del caricatore.
 *
 * <p>I valori predefiniti degli oggetti sono costanti con id {@link #NIL}: un id vero viene assegnato la prima volta
 * che il server vede l'oggetto, con {@link #ensurePistolId} / {@link #ensureMagazineId}.
 */
public final class WeaponComponents {
    private WeaponComponents() {}

    /** Id segnaposto dei valori predefiniti: nessun oggetto in gioco deve conservarlo a lungo. */
    public static final UUID NIL = new UUID(0L, 0L);

    /** Capienza del caricatore d'ordinanza. */
    public static final int MAGAZINE_CAPACITY = WeaponDef.pistol().magazineCapacity();

    public static final WeaponState DEFAULT_PISTOL = WeaponState.empty(NIL);
    public static final MagazineState DEFAULT_MAGAZINE = MagazineState.full(NIL, MAGAZINE_CAPACITY);

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, PoliceMod.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WeaponState>> PISTOL_STATE =
            COMPONENTS.registerComponentType("pistol_state",
                    b -> b.persistent(WeaponCodecs.WEAPON).networkSynchronized(WeaponCodecs.WEAPON_STREAM));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MagazineState>> MAGAZINE =
            COMPONENTS.registerComponentType("magazine",
                    b -> b.persistent(WeaponCodecs.MAGAZINE).networkSynchronized(WeaponCodecs.MAGAZINE_STREAM));

    /**
     * Stato della pistola con un id vero: se l'oggetto ha ancora {@link #NIL} (o nessuno stato) lo sostituisce con
     * {@code ids.get()} e lo scrive sull'oggetto. Da chiamare solo lato server; restituisce lo stato aggiornato.
     */
    public static WeaponState ensurePistolId(ItemStack stack, Supplier<UUID> ids) {
        WeaponState state = stack.getOrDefault(PISTOL_STATE.get(), DEFAULT_PISTOL);
        UUID id = freshIfNil(state.id(), ids);
        if (id.equals(state.id()) && stack.has(PISTOL_STATE.get())) return state;
        WeaponState next = new WeaponState(id, state.magazine(), state.chambered(), state.slideLocked());
        stack.set(PISTOL_STATE.get(), next);
        return next;
    }

    /** Come {@link #ensurePistolId}, per il caricatore. */
    public static MagazineState ensureMagazineId(ItemStack stack, Supplier<UUID> ids) {
        MagazineState state = stack.getOrDefault(MAGAZINE.get(), DEFAULT_MAGAZINE);
        UUID id = freshIfNil(state.id(), ids);
        if (id.equals(state.id()) && stack.has(MAGAZINE.get())) return state;
        MagazineState next = new MagazineState(id, state.rounds(), state.capacity(), state.knownBy());
        stack.set(MAGAZINE.get(), next);
        return next;
    }

    /** {@code id} se è già vero, altrimenti uno nuovo da {@code ids}. */
    static UUID freshIfNil(UUID id, Supplier<UUID> ids) {
        return NIL.equals(id) ? ids.get() : id;
    }
}
