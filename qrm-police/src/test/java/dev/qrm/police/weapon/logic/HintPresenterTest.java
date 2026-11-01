package dev.qrm.police.weapon.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.qrm.police.weapon.logic.HintPresenter.Binding;
import dev.qrm.police.weapon.logic.HintPresenter.Line;
import java.util.List;
import org.junit.jupiter.api.Test;

class HintPresenterTest {
    private static final float EPS = 1e-5f;

    private static void ticks(HintPresenter p, int n, Hint hint, boolean holding, boolean blocking) {
        for (int i = 0; i < n; i++) p.tick(hint, holding, blocking);
    }

    @Test
    void aRiposoNienteDaMostrare() {
        HintPresenter p = new HintPresenter();
        assertNull(p.line());
        assertEquals(0f, p.alpha(1f), EPS);
        p.tick(Hint.NONE, true, false);
        assertNull(p.line());
    }

    @Test
    void ilSuggerimentoEntraInSeiTick() {
        HintPresenter p = new HintPresenter();
        p.tick(Hint.RACK_SLIDE, true, false);
        assertEquals(new Line("qrm_police.hint.rack_slide", Binding.INTERACT, List.of()), p.line());
        assertEquals(1f / 6f, p.alpha(1f), EPS);
        assertEquals(0f, p.alpha(0f), EPS); // interpolato col tick precedente
        ticks(p, 4, Hint.RACK_SLIDE, true, false);
        assertEquals(5f / 6f, p.alpha(1f), EPS);
        p.tick(Hint.RACK_SLIDE, true, false);
        assertEquals(1f, p.alpha(1f), EPS);
        ticks(p, 100, Hint.RACK_SLIDE, true, false);
        assertEquals(1f, p.alpha(1f), EPS); // resta finché dura lo stato
    }

    @Test
    void chiaviETastiDeiQuattroStati() {
        assertEquals(new Line("qrm_police.hint.release_slide", Binding.INTERACT, List.of()),
                HintPresenter.lineFor(Hint.RELEASE_SLIDE));
        assertEquals(new Line("qrm_police.hint.rack_slide", Binding.INTERACT, List.of()),
                HintPresenter.lineFor(Hint.RACK_SLIDE));
        assertEquals(new Line("qrm_police.hint.reload", Binding.RELOAD, List.of()),
                HintPresenter.lineFor(Hint.RELOAD));
        assertEquals(new Line("qrm_police.hint.no_spare", Binding.NONE, List.of()),
                HintPresenter.lineFor(Hint.NO_SPARE));
        assertNull(HintPresenter.lineFor(Hint.NONE));
    }

    @Test
    void finitoLoStatoEsceInSeiTick() {
        HintPresenter p = new HintPresenter();
        ticks(p, 6, Hint.RELOAD, true, false);
        assertEquals(1f, p.alpha(1f), EPS);
        p.tick(Hint.NONE, true, false);
        assertEquals("qrm_police.hint.reload", p.line().key()); // ancora visibile mentre svanisce
        assertEquals(5f / 6f, p.alpha(1f), EPS);
        ticks(p, 4, Hint.NONE, true, false);
        assertEquals(1f / 6f, p.alpha(1f), EPS);
        p.tick(Hint.NONE, true, false);
        assertEquals(0f, p.alpha(1f), EPS);
        assertNull(p.line());
    }

    @Test
    void cambioDiStatoPrimaEsceEPoiEntra() {
        HintPresenter p = new HintPresenter();
        ticks(p, 6, Hint.RELOAD, true, false);
        ticks(p, 5, Hint.RACK_SLIDE, true, false);
        assertEquals("qrm_police.hint.reload", p.line().key());
        assertEquals(1f / 6f, p.alpha(1f), EPS);
        p.tick(Hint.RACK_SLIDE, true, false);
        assertEquals("qrm_police.hint.rack_slide", p.line().key());
        assertEquals(1f / 6f, p.alpha(1f), EPS);
    }

    @Test
    void nessunCaricatoreSoloPerQuarantaTickDopoUnClicAVuoto() {
        HintPresenter p = new HintPresenter();
        ticks(p, 20, Hint.NO_SPARE, true, false);
        assertNull(p.line()); // senza clic a vuoto non compare

        p.dryFire();
        p.tick(Hint.NO_SPARE, true, false);
        assertEquals(new Line("qrm_police.hint.no_spare", Binding.NONE, List.of()), p.line());
        ticks(p, 39, Hint.NO_SPARE, true, false); // 40 tick in tutto
        assertEquals(1f, p.alpha(1f), EPS);
        p.tick(Hint.NO_SPARE, true, false); // tick 41: comincia a svanire
        assertEquals(5f / 6f, p.alpha(1f), EPS);
        ticks(p, 5, Hint.NO_SPARE, true, false);
        assertNull(p.line());
    }

    @Test
    void unNuovoClicAVuotoRinnovaLaFinestra() {
        HintPresenter p = new HintPresenter();
        p.dryFire();
        ticks(p, 30, Hint.NO_SPARE, true, false);
        p.dryFire();
        ticks(p, 40, Hint.NO_SPARE, true, false);
        assertEquals(1f, p.alpha(1f), EPS);
        p.tick(Hint.NO_SPARE, true, false);
        assertEquals(5f / 6f, p.alpha(1f), EPS);
    }

    @Test
    void ilClicAVuotoNonCambiaGliAltriSuggerimenti() {
        HintPresenter p = new HintPresenter();
        p.dryFire();
        ticks(p, 6, Hint.RELOAD, true, false);
        assertEquals("qrm_police.hint.reload", p.line().key());
        ticks(p, 100, Hint.RELOAD, true, false);
        assertEquals("qrm_police.hint.reload", p.line().key());
    }

    @Test
    void durateUnAzioneCheBloccaIlSuggerimentoSiNasconde() {
        HintPresenter p = new HintPresenter();
        ticks(p, 6, Hint.RELOAD, true, false);
        p.tick(Hint.RELOAD, true, true);
        assertEquals(5f / 6f, p.alpha(1f), EPS);
        ticks(p, 5, Hint.RELOAD, true, true);
        assertNull(p.line());
        ticks(p, 30, Hint.RELOAD, true, true);
        assertNull(p.line());
        p.tick(Hint.RACK_SLIDE, true, false); // finita la ricarica: il nuovo stato
        assertEquals("qrm_police.hint.rack_slide", p.line().key());
    }

    @Test
    void azioniCheBloccano() {
        assertTrue(HintPresenter.blocks(PistolAction.RELOAD_TACTICAL));
        assertTrue(HintPresenter.blocks(PistolAction.RELOAD_EMPTY));
        assertTrue(HintPresenter.blocks(PistolAction.SLIDE_RELEASE));
        assertTrue(HintPresenter.blocks(PistolAction.SLIDE_RACK));
        assertFalse(HintPresenter.blocks(PistolAction.AMMO_CHECK));
        assertFalse(HintPresenter.blocks(PistolAction.INSPECT));
        assertFalse(HintPresenter.blocks(PistolAction.DRAW));
        assertFalse(HintPresenter.blocks(null));
        assertTrue(HintPresenter.blocksClip("reload_empty"));
        assertTrue(HintPresenter.blocksClip("slide_rack"));
        assertFalse(HintPresenter.blocksClip("fire"));
        assertFalse(HintPresenter.blocksClip(null));
    }

    @Test
    void ilControlloMunizioniSostituisceIlSuggerimentoPerNoveTick() {
        HintPresenter p = new HintPresenter();
        ticks(p, 6, Hint.RACK_SLIDE, true, false);
        p.ammoCheck(new MagazineState(java.util.UUID.randomUUID(), 3, 17, java.util.Set.of()));
        Line check = new Line("qrm_police.ammo_check", Binding.NONE,
                List.of("qrm_police.band.almost_empty"));
        // il suggerimento esce in 6 tick, poi entra il risultato
        ticks(p, 6, Hint.RACK_SLIDE, true, false);
        assertEquals(check, p.line());
        assertEquals(1f / 6f, p.alpha(1f), EPS);
        ticks(p, 3, Hint.RACK_SLIDE, true, false); // tick 9: ultimo del risultato
        assertEquals(check, p.line());
        assertEquals(4f / 6f, p.alpha(1f), EPS);
        p.tick(Hint.RACK_SLIDE, true, false); // scaduto: esce
        assertEquals(check, p.line());
        assertEquals(3f / 6f, p.alpha(1f), EPS);
        ticks(p, 3, Hint.RACK_SLIDE, true, false);
        p.tick(Hint.RACK_SLIDE, true, false); // poi torna il suggerimento
        assertEquals("qrm_police.hint.rack_slide", p.line().key());
    }

    @Test
    void controlloSenzaCaricatore() {
        HintPresenter p = new HintPresenter();
        p.ammoCheck(null);
        p.tick(Hint.NONE, true, false);
        assertEquals(new Line("qrm_police.ammo_check", Binding.NONE,
                List.of("qrm_police.ammo_check.no_magazine")), p.line());
        assertEquals(1f / 6f, p.alpha(1f), EPS);
    }

    @Test
    void controlloConCaricatorePienoEVuoto() {
        assertEquals(List.of("qrm_police.band.full"),
                HintPresenter.checkLine(MagazineState.full(java.util.UUID.randomUUID(), 17)).args());
        assertEquals(List.of("qrm_police.band.empty"),
                HintPresenter.checkLine(new MagazineState(java.util.UUID.randomUUID(), 0, 17, java.util.Set.of()))
                        .args());
    }

    @Test
    void unAzioneCheBloccaCancellaIlRisultatoDelControllo() {
        HintPresenter p = new HintPresenter();
        p.ammoCheck(null);
        ticks(p, 10, Hint.NONE, true, false);
        ticks(p, 6, Hint.NONE, true, true);
        assertNull(p.line());
        ticks(p, 5, Hint.NONE, true, false); // il risultato non torna dopo la ricarica
        assertNull(p.line());
    }

    @Test
    void ripostaLaPistolaSiAzzeraTutto() {
        HintPresenter p = new HintPresenter();
        p.dryFire();
        p.ammoCheck(null);
        ticks(p, 6, Hint.NO_SPARE, true, false);
        p.tick(Hint.NONE, false, false);
        assertNull(p.line());
        assertEquals(0f, p.alpha(1f), EPS);
        p.tick(Hint.NO_SPARE, true, false); // ripresa in mano: niente finestra residua né risultato
        assertNull(p.line());
    }
}
