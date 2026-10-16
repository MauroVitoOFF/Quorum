package dev.qrm.boss.logic;

/** Azioni richieste dal client. */
public enum BossAction {
    REFRESH, ADD_MEMBER, SET_RANK, REMOVE_MEMBER, SET_NOTICE, SET_TITLE, GIVE_BONUS;

    public static BossAction fromOrdinal(int ordinal) {
        BossAction[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : REFRESH;
    }
}
