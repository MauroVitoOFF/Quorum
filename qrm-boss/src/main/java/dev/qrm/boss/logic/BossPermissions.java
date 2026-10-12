package dev.qrm.boss.logic;

import dev.qrm.api.org.OrganizationBankService;
import java.util.function.Predicate;

/** Nodi del menu boss e sezioni che ciascuna combinazione di nodi rende visibili. */
public final class BossPermissions {
    public static final String MENU = "boss.menu";
    public static final String MEMBERS_MANAGE = "boss.members.manage";
    public static final String BANK_VIEW = "boss.bank.view";
    public static final String BANK_DEPOSIT = "boss.bank.deposit";
    public static final String WITHDRAW = OrganizationBankService.WITHDRAW_NODE;

    private BossPermissions() {}

    /** Sezioni del menu: la Panoramica c'è sempre (serve {@code boss.menu}), le altre solo con un nodo proprio. */
    public record Sections(boolean overview, boolean members, boolean account) {
        public static final Sections NONE = new Sections(false, false, false);
    }

    /** @param has vero se il Character ha il nodo esatto nell'azienda */
    public static Sections sections(Predicate<String> has) {
        if (!has.test(MENU)) return Sections.NONE;
        return new Sections(true, has.test(MEMBERS_MANAGE),
                has.test(BANK_VIEW) || has.test(BANK_DEPOSIT) || has.test(WITHDRAW));
    }
}
