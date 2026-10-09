package dev.qrm.neoforge.net;

public enum CharacterAction {
    ROSTER, SELECT, CREATE, ARCHIVE;

    public static CharacterAction fromOrdinal(int ordinal) {
        CharacterAction[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : ROSTER;
    }
}
