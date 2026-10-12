package dev.qrm.boss.logic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;

class BossPermissionsTest {
    private static BossPermissions.Sections of(String... nodes) {
        Set<String> set = Set.of(nodes);
        return BossPermissions.sections(set::contains);
    }

    @Test
    void nothingWithoutTheMenuNode() {
        assertEquals(BossPermissions.Sections.NONE, of());
        assertEquals(BossPermissions.Sections.NONE,
                of("boss.members.manage", "boss.bank.view", "boss.bank.deposit", "org.account.withdraw"));
    }

    @Test
    void menuAloneShowsOnlyTheOverview() {
        assertEquals(new BossPermissions.Sections(true, false, false), of("boss.menu"));
    }

    @Test
    void membersSectionNeedsItsNode() {
        assertEquals(new BossPermissions.Sections(true, true, false), of("boss.menu", "boss.members.manage"));
    }

    @Test
    void accountSectionAppearsWithAnyOfTheThreeBankNodes() {
        for (String node : new String[] {"boss.bank.view", "boss.bank.deposit", "org.account.withdraw"}) {
            assertEquals(new BossPermissions.Sections(true, false, true), of("boss.menu", node), node);
        }
    }

    @Test
    void everythingTogether() {
        assertEquals(new BossPermissions.Sections(true, true, true),
                of("boss.menu", "boss.members.manage", "boss.bank.view", "boss.bank.deposit", "org.account.withdraw"));
    }
}
