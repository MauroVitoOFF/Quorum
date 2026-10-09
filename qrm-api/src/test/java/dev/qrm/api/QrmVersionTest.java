package dev.qrm.api;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class QrmVersionTest {
    @Test
    void apiVersionIsDeclared() {
        assertFalse(QrmVersion.API_VERSION.isBlank());
    }
}
