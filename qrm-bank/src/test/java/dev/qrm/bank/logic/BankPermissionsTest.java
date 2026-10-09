package dev.qrm.bank.logic;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.character.PlayerCharacter;
import org.junit.jupiter.api.Test;

class BankPermissionsTest {
    @Test
    void allowedByDefaultButNotAfterExplicitDeny() {
        try (BankEnv env = new BankEnv()) {
            PlayerCharacter anna = env.newCharacter();
            assertTrue(BankPermissions.allowed(env.permissions, anna.id(), BankPermissions.ATM_USE));
            env.permissions.deny(anna.id(), BankPermissions.ATM_USE);
            assertFalse(BankPermissions.allowed(env.permissions, anna.id(), BankPermissions.ATM_USE));
            assertTrue(BankPermissions.allowed(env.permissions, anna.id(), BankPermissions.TRANSFER));
            env.permissions.grant(anna.id(), BankPermissions.ATM_USE);
            assertTrue(BankPermissions.allowed(env.permissions, anna.id(), BankPermissions.ATM_USE));
        }
    }
}
