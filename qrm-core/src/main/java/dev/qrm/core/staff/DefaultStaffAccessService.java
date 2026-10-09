package dev.qrm.core.staff;

import dev.qrm.api.permission.PermissionNodes;
import dev.qrm.api.staff.StaffAccessService;
import dev.qrm.api.staff.StaffNodes;
import dev.qrm.core.db.Database;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

public final class DefaultStaffAccessService implements StaffAccessService {
    private final Database db;

    public DefaultStaffAccessService(Database db) {
        this.db = db;
    }

    @Override
    public boolean has(UUID account, String node) {
        if (!PermissionNodes.isExactNode(node)) {
            throw new IllegalArgumentException("not an exact permission node: " + node);
        }
        return StaffNodes.matchesAny(grantsOf(account), node);
    }

    @Override
    public Set<String> grantsOf(UUID account) {
        return db.read(c -> {
            Set<String> out = new TreeSet<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT node FROM qrm_staff_grant WHERE account=?")) {
                ps.setString(1, account.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.add(rs.getString(1));
                }
            }
            return out;
        });
    }

    @Override
    public boolean grant(UUID account, String pattern) {
        require(pattern);
        return db.tx(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT 1 FROM qrm_staff_grant WHERE account=? AND node=?")) {
                ps.setString(1, account.toString());
                ps.setString(2, pattern);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return false;
                }
            }
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO qrm_staff_grant (account, node) VALUES (?,?)")) {
                ps.setString(1, account.toString());
                ps.setString(2, pattern);
                ps.executeUpdate();
            }
            return true;
        });
    }

    @Override
    public boolean revoke(UUID account, String pattern) {
        require(pattern);
        return db.tx(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM qrm_staff_grant WHERE account=? AND node=?")) {
                ps.setString(1, account.toString());
                ps.setString(2, pattern);
                return ps.executeUpdate() > 0;
            }
        });
    }

    private static void require(String pattern) {
        if (!StaffNodes.isGrantable(pattern)) {
            throw new IllegalArgumentException("not a grantable staff pattern: " + pattern);
        }
    }
}
