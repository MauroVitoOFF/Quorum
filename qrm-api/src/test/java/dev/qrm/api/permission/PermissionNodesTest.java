package dev.qrm.api.permission;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PermissionNodesTest {
    @Test
    void exactNodes() {
        assertTrue(PermissionNodes.isExactNode("police.arrest"));
        assertTrue(PermissionNodes.isExactNode("a"));
        assertTrue(PermissionNodes.isExactNode("a_b.c1.d_2"));
        assertFalse(PermissionNodes.isExactNode(""));
        assertFalse(PermissionNodes.isExactNode(null));
        assertFalse(PermissionNodes.isExactNode("Police.arrest"));
        assertFalse(PermissionNodes.isExactNode("police..arrest"));
        assertFalse(PermissionNodes.isExactNode(".police"));
        assertFalse(PermissionNodes.isExactNode("police."));
        assertFalse(PermissionNodes.isExactNode("police.*"));
        assertFalse(PermissionNodes.isExactNode("*"));
        assertFalse(PermissionNodes.isExactNode("a".repeat(129)));
    }

    @Test
    void patterns() {
        assertTrue(PermissionNodes.isPattern("*"));
        assertTrue(PermissionNodes.isPattern("police.*"));
        assertTrue(PermissionNodes.isPattern("police.cuff.*"));
        assertTrue(PermissionNodes.isPattern("police.arrest"));
        assertFalse(PermissionNodes.isPattern("*.x"));
        assertFalse(PermissionNodes.isPattern("police.*.x"));
        assertFalse(PermissionNodes.isPattern("police*"));
        assertFalse(PermissionNodes.isPattern("**"));
        assertFalse(PermissionNodes.isPattern(""));
        assertFalse(PermissionNodes.isPattern(null));
        assertFalse(PermissionNodes.isPattern("a".repeat(127) + ".*"));
    }

    @Test
    void requirePatternReturnsValueOrThrows() {
        assertEquals("police.*", PermissionNodes.requirePattern("police.*"));
        assertThrows(IllegalArgumentException.class, () -> PermissionNodes.requirePattern("Bad Node"));
    }
}
