package dev.qrm.police.weapon.mc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.qrm.police.weapon.logic.MagazineState;
import dev.qrm.police.weapon.logic.WeaponState;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Codec persistenti e di rete degli stati di pistola e caricatore. */
public final class WeaponCodecs {
    private WeaponCodecs() {}

    /** Campi grezzi di un caricatore, prima della validazione dell'intervallo dei colpi. */
    private record RawMagazine(UUID id, int rounds, int capacity, List<String> knownBy) {}

    private static final Codec<RawMagazine> RAW_MAGAZINE = RecordCodecBuilder.create(i -> i.group(
                    UUIDUtil.CODEC.fieldOf("id").forGetter(RawMagazine::id),
                    Codec.INT.fieldOf("rounds").forGetter(RawMagazine::rounds),
                    Codec.INT.fieldOf("capacity").forGetter(RawMagazine::capacity),
                    Codec.STRING.listOf().optionalFieldOf("known_by", List.of()).forGetter(RawMagazine::knownBy))
            .apply(i, RawMagazine::new));

    /**
     * Caricatore su disco. Un salvataggio con colpi o capienza fuori intervallo diventa un errore del codec
     * invece di un'eccezione; {@code known_by} è scritto in ordine alfabetico per un risultato stabile.
     */
    public static final Codec<MagazineState> MAGAZINE = RAW_MAGAZINE.comapFlatMap(
            WeaponCodecs::toMagazine,
            m -> new RawMagazine(m.id(), m.rounds(), m.capacity(), m.knownBy().stream().sorted().toList()));

    private static final StreamCodec<ByteBuf, Set<String>> KNOWN_BY_STREAM = ByteBufCodecs.STRING_UTF8
            .apply(ByteBufCodecs.list())
            .map(HashSet::new, ArrayList::new);

    /** Caricatore in rete. */
    public static final StreamCodec<ByteBuf, MagazineState> MAGAZINE_STREAM = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, MagazineState::id,
            ByteBufCodecs.VAR_INT, MagazineState::rounds,
            ByteBufCodecs.VAR_INT, MagazineState::capacity,
            KNOWN_BY_STREAM, MagazineState::knownBy,
            MagazineState::new);

    /** Pistola su disco: il caricatore inserito è un campo facoltativo. */
    public static final Codec<WeaponState> WEAPON = RecordCodecBuilder.create(i -> i.group(
                    UUIDUtil.CODEC.fieldOf("id").forGetter(WeaponState::id),
                    MAGAZINE.optionalFieldOf("magazine").forGetter(WeaponState::magazine),
                    Codec.BOOL.optionalFieldOf("chambered", false).forGetter(WeaponState::chambered),
                    Codec.BOOL.optionalFieldOf("slide_locked", false).forGetter(WeaponState::slideLocked))
            .apply(i, WeaponState::new));

    /** Pistola in rete. */
    public static final StreamCodec<ByteBuf, WeaponState> WEAPON_STREAM = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, WeaponState::id,
            ByteBufCodecs.optional(MAGAZINE_STREAM), WeaponState::magazine,
            ByteBufCodecs.BOOL, WeaponState::chambered,
            ByteBufCodecs.BOOL, WeaponState::slideLocked,
            WeaponState::new);

    private static DataResult<MagazineState> toMagazine(RawMagazine r) {
        if (r.capacity() <= 0) return DataResult.error(() -> "capienza del caricatore non valida: " + r.capacity());
        if (r.rounds() < 0 || r.rounds() > r.capacity()) {
            return DataResult.error(() -> "colpi fuori intervallo 0.." + r.capacity() + ": " + r.rounds());
        }
        return DataResult.success(new MagazineState(r.id(), r.rounds(), r.capacity(), Set.copyOf(r.knownBy())));
    }
}
