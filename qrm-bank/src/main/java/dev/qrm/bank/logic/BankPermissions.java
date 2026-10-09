package dev.qrm.bank.logic;

import dev.qrm.api.id.CharacterId;
import dev.qrm.api.permission.PermissionEffect;
import dev.qrm.api.permission.PermissionService;

/**
 * Nodi della banca. Sono consentiti a tutti salvo DENY esplicito sul personaggio: il {@code PermissionService} non ha
 * un concetto di "concesso per impostazione predefinita".
 */
public final class BankPermissions {
    public static final String ATM_USE = "bank.atm.use";
    public static final String TRANSFER = "bank.transfer";

    private BankPermissions() {}

    public static boolean allowed(PermissionService permissions, CharacterId character, String node) {
        return permissions.overridesOf(character).get(node) != PermissionEffect.DENY;
    }
}
