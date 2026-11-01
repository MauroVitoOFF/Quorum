package dev.qrm.police.weapon.logic;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * Cosa scrivere vicino alla pistola e con quale opacità, tick per tick (proposta per Q4c). Puro: niente Minecraft,
 * così tempi e precedenze si provano con i test; il disegno e le chiavi dei tasti li mette il client.
 *
 * <p>Precedenze del testo voluto in un tick:
 * <ol>
 *   <li>pistola non in mano: niente, e si dimentica tutto (finestre e risultato);</li>
 *   <li>azione che blocca in corso (ricarica, carrello: {@link #blocks}): niente, e il risultato del controllo
 *       munizioni si scarta (dopo una ricarica non varrebbe più);</li>
 *   <li>risultato del controllo munizioni, per {@link #CHECK_TICKS} tick da {@link #ammoCheck};</li>
 *   <li>{@code RELEASE_SLIDE}, {@code RACK_SLIDE}, {@code RELOAD}: finché dura lo stato;</li>
 *   <li>{@code NO_SPARE}: solo per {@link #NO_SPARE_TICKS} tick dopo un clic a vuoto ({@link #dryFire}).</li>
 * </ol>
 *
 * <p>Dissolvenza: il livello va da 0 a {@link #FADE_TICKS}, un passo per tick. Se il testo voluto cambia, quello
 * mostrato prima esce del tutto e poi entra il nuovo (mai due testi insieme, mai un testo che cambia a metà opacità).
 */
public final class HintPresenter {
    /** Tick di ingresso e di uscita. */
    public static final int FADE_TICKS = 6;
    /** Durata di {@code NO_SPARE} dopo un clic a vuoto. */
    public static final int NO_SPARE_TICKS = 40;
    /** Durata del risultato del controllo munizioni. */
    public static final int CHECK_TICKS = 9;

    private static final Set<PistolAction> BLOCKING = EnumSet.of(
            PistolAction.RELOAD_TACTICAL, PistolAction.RELOAD_EMPTY,
            PistolAction.SLIDE_RELEASE, PistolAction.SLIDE_RACK);

    /** Tasto il cui nome va nel primo {@code %s} della riga. */
    public enum Binding { NONE, INTERACT, RELOAD }

    /**
     * Una riga da tradurre: {@code key} con, nell'ordine, il nome del tasto {@code binding} (se c'è) e le traduzioni
     * delle chiavi {@code args}.
     */
    public record Line(String key, Binding binding, List<String> args) {
        public Line {
            args = List.copyOf(args);
        }
    }

    private long now;
    private long noSpareUntil = Long.MIN_VALUE;
    private long checkUntil = Long.MIN_VALUE;
    private @Nullable Line check;

    private @Nullable Line current;
    private int level;
    private int prevLevel;

    // ------------------------------------------------------------------ ingressi

    /** Clic a vuoto: apre (o rinnova) la finestra di {@code NO_SPARE}. */
    public void dryFire() {
        noSpareUntil = now + NO_SPARE_TICKS;
    }

    /**
     * Commit del controllo munizioni: mostra il risultato per {@link #CHECK_TICKS} tick.
     *
     * @param magazine caricatore inserito in quell'istante, {@code null} se manca
     */
    public void ammoCheck(@Nullable MagazineState magazine) {
        check = checkLine(magazine);
        checkUntil = now + CHECK_TICKS;
    }

    /**
     * Avanza di un tick.
     *
     * @param hint suggerimento dello stato attuale ({@link PistolRules#hint})
     * @param holding vero se il giocatore impugna la pistola
     * @param blocking vero se è in corso un'azione che blocca ({@link #blocks}, {@link #blocksClip})
     */
    public void tick(Hint hint, boolean holding, boolean blocking) {
        now++;
        if (!holding) {
            noSpareUntil = Long.MIN_VALUE;
            checkUntil = Long.MIN_VALUE;
            check = null;
            current = null;
            level = 0;
            prevLevel = 0;
            return;
        }
        if (blocking) {
            check = null;
            checkUntil = Long.MIN_VALUE;
        }

        Line target = target(hint, blocking);
        prevLevel = level;
        if (!Objects.equals(target, current)) {
            if (level > 0) level--;
            if (level == 0) {
                current = target;
                prevLevel = 0;
            }
        }
        if (current != null && Objects.equals(target, current)) level = Math.min(FADE_TICKS, level + 1);
    }

    // ------------------------------------------------------------------ uscite

    /** Riga mostrata ora (anche mentre svanisce), o {@code null}. */
    public @Nullable Line line() {
        return current;
    }

    /** Opacità tra 0 e 1, interpolata tra il tick precedente e l'attuale. */
    public float alpha(float partialTick) {
        if (current == null) return 0f;
        float t = Math.max(0f, Math.min(1f, partialTick));
        return (prevLevel + (level - prevLevel) * t) / FADE_TICKS;
    }

    // ------------------------------------------------------------------ regole

    /** Vero se durante l'azione i suggerimenti si nascondono: ricariche e carrello. */
    public static boolean blocks(@Nullable PistolAction action) {
        return action != null && BLOCKING.contains(action);
    }

    /** Come {@link #blocks} per il nome di una clip in corso. */
    public static boolean blocksClip(@Nullable String clip) {
        if (clip == null) return false;
        for (PistolAction a : BLOCKING) if (a.clip().equals(clip)) return true;
        return false;
    }

    /** Riga di un suggerimento persistente o di {@code NO_SPARE}; {@code null} per {@code NONE}. */
    public static @Nullable Line lineFor(Hint hint) {
        return switch (hint) {
            case RELEASE_SLIDE -> new Line("qrm_police.hint.release_slide", Binding.INTERACT, List.of());
            case RACK_SLIDE -> new Line("qrm_police.hint.rack_slide", Binding.INTERACT, List.of());
            case RELOAD -> new Line("qrm_police.hint.reload", Binding.RELOAD, List.of());
            case NO_SPARE -> new Line("qrm_police.hint.no_spare", Binding.NONE, List.of());
            case NONE -> null;
        };
    }

    /** "Caricatore: &lt;fascia&gt;" (solo i colpi del caricatore); senza caricatore la fascia diventa "nessuno". */
    public static Line checkLine(@Nullable MagazineState magazine) {
        String band = magazine == null
                ? "qrm_police.ammo_check.no_magazine"
                : "qrm_police.band." + AmmoBand.of(magazine.rounds(), magazine.capacity()).name()
                        .toLowerCase(Locale.ROOT);
        return new Line("qrm_police.ammo_check", Binding.NONE, List.of(band));
    }

    private @Nullable Line target(Hint hint, boolean blocking) {
        if (blocking) return null;
        if (check != null && now <= checkUntil) return check;
        return switch (hint) {
            case RELEASE_SLIDE, RACK_SLIDE, RELOAD -> lineFor(hint);
            case NO_SPARE -> now <= noSpareUntil ? lineFor(hint) : null;
            case NONE -> null;
        };
    }
}
