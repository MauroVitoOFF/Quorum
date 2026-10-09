package dev.qrm.core.data;

import dev.qrm.api.data.ModDataService;
import dev.qrm.api.economy.OwnerRef;
import dev.qrm.core.db.Database;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

public final class DefaultModDataService implements ModDataService {
    private final Database db;

    public DefaultModDataService(Database db) {
        this.db = db;
    }

    @Override
    public void put(String namespace, OwnerRef owner, String key, String json) {
        db.tx(c -> {
            int updated;
            try (PreparedStatement ps = c.prepareStatement("UPDATE qrm_mod_data SET data_value=? "
                    + "WHERE mod_ns=? AND owner_type=? AND owner_id=? AND data_key=?")) {
                ps.setString(1, json);
                ps.setString(2, namespace);
                ps.setString(3, owner.type());
                ps.setString(4, owner.id().toString());
                ps.setString(5, key);
                updated = ps.executeUpdate();
            }
            if (updated == 0) {
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO qrm_mod_data "
                        + "(mod_ns, owner_type, owner_id, data_key, data_value) VALUES (?,?,?,?,?)")) {
                    ps.setString(1, namespace);
                    ps.setString(2, owner.type());
                    ps.setString(3, owner.id().toString());
                    ps.setString(4, key);
                    ps.setString(5, json);
                    ps.executeUpdate();
                }
            }
            return null;
        });
    }

    @Override
    public Optional<String> get(String namespace, OwnerRef owner, String key) {
        return db.read(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT data_value FROM qrm_mod_data "
                    + "WHERE mod_ns=? AND owner_type=? AND owner_id=? AND data_key=?")) {
                ps.setString(1, namespace);
                ps.setString(2, owner.type());
                ps.setString(3, owner.id().toString());
                ps.setString(4, key);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(rs.getString(1)) : Optional.<String>empty();
                }
            }
        });
    }

    @Override
    public void delete(String namespace, OwnerRef owner, String key) {
        db.tx(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM qrm_mod_data "
                    + "WHERE mod_ns=? AND owner_type=? AND owner_id=? AND data_key=?")) {
                ps.setString(1, namespace);
                ps.setString(2, owner.type());
                ps.setString(3, owner.id().toString());
                ps.setString(4, key);
                ps.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public int purgeNamespace(String namespace) {
        return db.tx(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM qrm_mod_data WHERE mod_ns=?")) {
                ps.setString(1, namespace);
                return ps.executeUpdate();
            }
        });
    }
}
