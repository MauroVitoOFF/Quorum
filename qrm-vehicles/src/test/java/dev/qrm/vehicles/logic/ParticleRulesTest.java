package dev.qrm.vehicles.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ParticleRulesTest {

    @Test
    void asfaltoEGhiaccioNonSollevanoNulla() {
        assertEquals(0, ParticleRules.dust("paved", 25));
        assertEquals(0, ParticleRules.dust("ice", 25));
    }

    @Test
    void ilFondoMorbidoSollevaPiuPolvereAllaVelocitaMaggiore() {
        assertEquals(0, ParticleRules.dust("sand", 1));
        assertTrue(ParticleRules.dust("sand", 20) > ParticleRules.dust("sand", 5));
        assertTrue(ParticleRules.dust("sand", 20) > ParticleRules.dust("dirt", 20));
        assertTrue(ParticleRules.dust("dirt", 100) <= ParticleRules.dust("dirt", 25), "oltre 25 m/s non cresce");
    }

    @Test
    void ilMotoSollevaLaPolvereAncheAndandoIndietro() {
        assertTrue(ParticleRules.dust("snow", -10) > 0);
    }

    @Test
    void lePneumaticheStrisciano() {
        assertTrue(ParticleRules.skidding(4, 0, 20));
        assertTrue(ParticleRules.skidding(-4, 0, 20));
        assertTrue(ParticleRules.skidding(0, 12, 15));
        assertFalse(ParticleRules.skidding(1, 2, 20));
        assertFalse(ParticleRules.skidding(0, 12, 2), "frenata forte ma quasi fermi: niente fumo");
    }

    @Test
    void loSbuffoSoloAlCambioDiMarciaAvanti() {
        assertTrue(ParticleRules.shifted(1, 2));
        assertTrue(ParticleRules.shifted(3, 2));
        assertFalse(ParticleRules.shifted(2, 2));
        assertFalse(ParticleRules.shifted(-1, 1), "uscire dalla retromarcia non e' un cambio");
    }
}
