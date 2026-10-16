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
                of("boss.members.manage", "boss.notice", "boss.title", "boss.bonus"));
    }

    @Test
    void menuAloneShowsOnlyTheOverview() {
        assertEquals(new BossPermissions.Sections(true, false, false, false, false, false), of("boss.menu"));
    }

    @Test
    void manageGivesTheMembersSectionAndRankChanges() {
        assertEquals(new BossPermissions.Sections(true, true, true, false, false, false),
                of("boss.menu", "boss.members.manage"));
    }

    @Test
    void titleAndBonusAlsoOpenTheMembersSectionButNotRankChanges() {
        assertEquals(new BossPermissions.Sections(true, true, false, false, true, false), of("boss.menu", "boss.title"));
        assertEquals(new BossPermissions.Sections(true, true, false, false, false, true), of("boss.menu", "boss.bonus"));
    }

    @Test
    void noticeDoesNotOpenTheMembersSection() {
        assertEquals(new BossPermissions.Sections(true, false, false, true, false, false), of("boss.menu", "boss.notice"));
    }

    @Test
    void everythingTogether() {
        assertEquals(new BossPermissions.Sections(true, true, true, true, true, true),
                of("boss.menu", "boss.members.manage", "boss.notice", "boss.title", "boss.bonus"));
    }
}
