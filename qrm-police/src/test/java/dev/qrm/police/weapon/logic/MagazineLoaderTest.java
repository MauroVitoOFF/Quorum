package dev.qrm.police.weapon.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MagazineLoaderTest {
    private static final int CAP = 17;

    @Test
    void clicSinistroSpostaTuttoQuelloCheCi() {
        assertEquals(10, MagazineLoader.loadable(0, CAP, CAP, 10, false));
        assertEquals(17, MagazineLoader.loadable(0, CAP, CAP, 64, false));
        assertEquals(5, MagazineLoader.loadable(12, CAP, CAP, 64, false));
    }

    @Test
    void clicDestroSpostaUnSoloColpo() {
        assertEquals(1, MagazineLoader.loadable(0, CAP, CAP, 64, true));
        assertEquals(1, MagazineLoader.loadable(16, CAP, CAP, 64, true));
    }

    @Test
    void pienoONessunProiettileNonSposta() {
        assertEquals(0, MagazineLoader.loadable(CAP, CAP, CAP, 64, false));
        assertEquals(0, MagazineLoader.loadable(CAP, CAP, CAP, 64, true));
        assertEquals(0, MagazineLoader.loadable(3, CAP, CAP, 0, false));
        assertEquals(0, MagazineLoader.loadable(3, CAP, CAP, 0, true));
    }

    @Test
    void laCapienzaDellaDefinizioneVinceSuQuellaSalvata() {
        // un caricatore con una capienza maggiore nei dati non supera quella dell'arma
        assertEquals(2, MagazineLoader.loadable(15, 30, CAP, 64, false));
        assertEquals(0, MagazineLoader.loadable(17, 30, CAP, 64, false));
        // e se il caricatore ne ha già più della capienza (dati vecchi), non ne accetta altri
        assertEquals(0, MagazineLoader.loadable(20, 30, CAP, 64, false));
    }

    @Test
    void lAttesaNonPuoMaiEssereNegativaOSuperareLeDisponibili() {
        for (int rounds = 0; rounds <= CAP; rounds++) {
            for (int have : new int[] {0, 1, 5, 17, 64}) {
                for (boolean single : new boolean[] {false, true}) {
                    int n = MagazineLoader.loadable(rounds, CAP, CAP, have, single);
                    assertTrue(n >= 0 && n <= have && rounds + n <= CAP, rounds + "/" + have + "/" + single);
                }
            }
        }
    }

    @Test
    void laDefinizioneDellaPistolaDaLaCapienzaEIlProiettile() {
        WeaponDef d = WeaponDef.pistol();
        assertEquals(17, d.magazineCapacity());
        assertEquals("qrm_police:police_bullet", d.bulletItem());
        assertTrue(d.clips().containsKey("refill_loop"));
    }

    @Test
    void iTempiDelRiempimentoVengonoDalleClip() {
        // preparazione 0,7 s = 14 tick + il "clack" a 0,2 s = 4 tick; un giro dura 0,6 s = 12 tick
        assertEquals(18, MagazineLoader.firstInsertTicks());
        assertEquals(12, MagazineLoader.insertIntervalTicks());
        // riempire 17 colpi dura 18 + 16 · 12 tick, cioe' circa 10 secondi
        int total = MagazineLoader.firstInsertTicks() + 16 * MagazineLoader.insertIntervalTicks();
        assertTrue(total >= 200 && total <= 240, "totale " + total);
    }
}
