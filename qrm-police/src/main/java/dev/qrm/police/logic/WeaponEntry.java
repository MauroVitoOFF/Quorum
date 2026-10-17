package dev.qrm.police.logic;

/** Un'arma registrata. {@code ownerId} è vuoto per le armi d'ordinanza o senza proprietario noto. */
public record WeaponEntry(String serial, String type, String ownerId, String ownerName, WeaponStatus status) {
    public WeaponEntry withStatus(WeaponStatus s) {
        return new WeaponEntry(serial, type, ownerId, ownerName, s);
    }
}
