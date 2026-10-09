package dev.qrm.core.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

/** Applica in ordine gli script SQL non ancora eseguiti. La posizione nella lista è la versione (1-based). */
public final class Migrator {
    private static final List<String> FILES = List.of("V1__init.sql", "V2__jobs_permissions.sql", "V3__organizations.sql",
            "V4__staff_access.sql",
            "V5__staff_log.sql",
            "V6__moderation.sql");

    private Migrator() {}

    public static void migrate(Database db) {
        db.tx(c -> {
            try (Statement s = c.createStatement()) {
                s.execute("CREATE TABLE IF NOT EXISTS qrm_schema_version (version INT PRIMARY KEY, applied_at BIGINT NOT NULL)");
            }
            int current;
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT MAX(version) FROM qrm_schema_version")) {
                rs.next();
                current = rs.getInt(1);
            }
            for (int i = 0; i < FILES.size(); i++) {
                int version = i + 1;
                if (version <= current) continue;
                for (String statement : statementsOf(FILES.get(i))) {
                    try (Statement s = c.createStatement()) {
                        s.execute(statement);
                    }
                }
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO qrm_schema_version (version, applied_at) VALUES (?, ?)")) {
                    ps.setInt(1, version);
                    ps.setLong(2, System.currentTimeMillis());
                    ps.executeUpdate();
                }
            }
            return null;
        });
    }

    private static List<String> statementsOf(String file) {
        try (InputStream in = Migrator.class.getResourceAsStream("/db/migration/" + file)) {
            if (in == null) throw new IllegalStateException("Missing migration: " + file);
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            StringBuilder cleaned = new StringBuilder();
            for (String line : sql.split("\n")) {
                if (!line.strip().startsWith("--")) cleaned.append(line).append('\n');
            }
            return java.util.Arrays.stream(cleaned.toString().split(";"))
                    .map(String::strip)
                    .filter(s -> !s.isEmpty())
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read migration " + file, e);
        }
    }
}
