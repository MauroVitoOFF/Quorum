package dev.qrm.locker.logic;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.point.PointOwner;
import org.junit.jupiter.api.Test;

class LockerKeysTest {
    @Test
    void jobAndOrganizationGetDifferentKeys() {
        assertEquals("locker.job.police", LockerKeys.of(PointOwner.job("police")));
        assertEquals("locker.org.police", LockerKeys.of(PointOwner.organization("police")));
        assertNotEquals(LockerKeys.of(PointOwner.job("a")), LockerKeys.of(PointOwner.job("b")));
    }
}
