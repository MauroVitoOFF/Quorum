package dev.qrm.core.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Statement;

/**
 * Copia di sicurezza del database SQLite prima di una migrazione. Il file si chiama {@code qrm.db.bak-v<N>}, dove N è la
 * versione dello schema prima dell'aggiornamento: è l'unica copia che permette di tornare a un QRM più vecchio.
 */
public final class DatabaseBackup {
    private static final String PREFIX = "jdbc:sqlite:";

    private DatabaseBackup() {}

    /**
     * Se il database è un file SQLite già esistente e ha migrazioni da applicare, ne salva una copia accanto al file.
     * Non sovrascrive una copia già presente. Niente da fare per database nuovi, in memoria o non SQLite.
     *
     * @return il file creato, o {@code null} se non serviva
     */
    public static Path beforeMigration(Database db, String jdbcUrl) {
        if (!jdbcUrl.startsWith(PREFIX)) return null;
        String file = jdbcUrl.substring(PREFIX.length());
        if (file.isBlank() || file.startsWith(":memory:") || file.contains("mode=memory")) return null;
        Path source = Path.of(file);
        if (!Files.isRegularFile(source)) return null;

        int current = Migrator.currentVersion(db);
        if (current == 0 || current >= Migrator.latestVersion()) return null;

        Path backup = source.resolveSibling(source.getFileName() + ".bak-v" + current);
        if (Files.exists(backup)) return null;
        // VACUUM INTO produce una copia coerente anche col database aperto e in modalità WAL
        db.read(c -> {
            try (Statement s = c.createStatement()) {
                s.execute("VACUUM INTO '" + backup.toAbsolutePath().toString().replace("'", "''") + "'");
            }
            return null;
        });
        return backup;
    }
}
