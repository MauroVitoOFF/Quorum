package dev.qrm.boss.logic;

import java.util.function.Predicate;

/** Nodi del menu boss e sezioni che ciascuna combinazione di nodi rende visibili. */
public final class BossPermissions {
    public static final String MENU = "boss.menu";
    public static final String MEMBERS_MANAGE = "boss.members.manage";
    public static final String NOTICE = "boss.notice";
    public static final String TITLE = "boss.title";
    public static final String BONUS = "boss.bonus";

    private BossPermissions() {}

    /**
     * Cosa vede e può fare chi guarda: la Panoramica c'è sempre (serve {@code boss.menu}); la sezione Membri compare
     * con uno qualunque dei nodi che agiscono sui membri; {@code manage} abilita rank e licenziamenti, {@code notice} abilita a scrivere l'avviso.
     */
    public record Sections(boolean overview, boolean members, boolean manage, boolean notice, boolean title,
            boolean bonus) {
        public static final Sections NONE = new Sections(false, false, false, false, false, false);
    }

    /** @param has vero se il Character ha il nodo esatto nell'azienda */
    public static Sections sections(Predicate<String> has) {
        if (!has.test(MENU)) return Sections.NONE;
        boolean title = has.test(TITLE);
        boolean bonus = has.test(BONUS);
        boolean manage = has.test(MEMBERS_MANAGE);
        return new Sections(true, manage || title || bonus, manage, has.test(NOTICE), title, bonus);
    }
}
