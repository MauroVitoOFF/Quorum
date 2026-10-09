package dev.qrm.neoforge.server;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.neoforge.net.PlayerInfoLine;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InfoLinesTest {
    private static List<String> keys(List<PlayerInfoLine> lines) {
        return lines.stream().map(PlayerInfoLine::labelKey).toList();
    }

    @Test
    void characterWithJobAndOrganizations() {
        List<PlayerInfoLine> lines = InfoLines.character("Marco", "Rossi", LocalDate.of(1990, 3, 12), "ACTIVE",
                Optional.of(new InfoLines.JobView("Mechanic", "Chief", true)),
                List.of(new InfoLines.OrgView("Benny's", Optional.of("Boss")),
                        new InfoLines.OrgView("Garage", Optional.empty())));
        assertEquals(List.of("qrm.staff.info.name", "qrm.staff.info.birth", "qrm.staff.info.status", "qrm.staff.info.job",
                "qrm.staff.info.on_duty", "qrm.staff.info.org", "qrm.staff.info.org"), keys(lines));
        assertEquals("Marco Rossi", lines.get(0).value());
        assertEquals("1990-03-12", lines.get(1).value());
        assertEquals("active", lines.get(2).value());
        assertEquals("Mechanic · Chief", lines.get(3).value());
        assertEquals("Benny's · Boss", lines.get(5).value());
        assertEquals("Garage", lines.get(6).value());
    }

    @Test
    void characterWithoutJobOrOrganizationsShowsDashes() {
        List<PlayerInfoLine> lines = InfoLines.character("Anna", "Bianchi", LocalDate.of(2000, 1, 1), "ACTIVE",
                Optional.empty(), List.of());
        assertEquals("—", lines.get(3).value());
        assertEquals("qrm.staff.info.org", lines.get(4).labelKey());
        assertEquals("—", lines.get(4).value());
    }

    @Test
    void offDutyUsesItsOwnLabel() {
        List<PlayerInfoLine> lines = InfoLines.character("A", "B", LocalDate.of(2000, 1, 1), "ACTIVE",
                Optional.of(new InfoLines.JobView("Polizia", "Cadetto", false)), List.of());
        assertTrue(keys(lines).contains("qrm.staff.info.off_duty"));
        assertFalse(keys(lines).contains("qrm.staff.info.on_duty"));
    }

    @Test
    void permissionsListEffectiveThenOverrides() {
        List<PlayerInfoLine> lines = InfoLines.permissions(Set.of("police.cuff", "police.arrest"),
                Map.of("police.fine", false, "police.arrest", true));
        assertEquals(List.of("qrm.staff.info.permission", "qrm.staff.info.permission", "qrm.staff.info.override_grant",
                "qrm.staff.info.override_deny"), keys(lines));
        assertEquals("police.arrest", lines.get(0).value());          // ordinati
        assertEquals("police.cuff", lines.get(1).value());
        assertEquals("police.arrest", lines.get(2).value());
        assertEquals("police.fine", lines.get(3).value());
    }

    @Test
    void noPermissionsShowsAPlaceholder() {
        List<PlayerInfoLine> lines = InfoLines.permissions(Set.of(), Map.of());
        assertEquals(1, lines.size());
        assertEquals("qrm.staff.info.no_permissions", lines.get(0).labelKey());
    }

    @Test
    void permissionPagesAreCappedToTheLimit() {
        Set<String> many = new HashSet<>();
        for (int i = 0; i < 200; i++) many.add("staff.module.m" + i);
        assertEquals(InfoLines.MAX_LINES, InfoLines.permissions(many, Map.of()).size());
    }
}
