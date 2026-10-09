package dev.qrm.core.data;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.economy.OwnerRef;
import dev.qrm.api.id.CharacterId;
import dev.qrm.core.db.Database;
import dev.qrm.core.db.Migrator;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ModDataServiceTest {
    private Database db;
    private DefaultModDataService data;
    private final OwnerRef owner = OwnerRef.character(CharacterId.random());

    @BeforeEach
    void setUp() {
        db = Database.open("jdbc:sqlite::memory:");
        Migrator.migrate(db);
        data = new DefaultModDataService(db);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void putGetOverwriteDelete() {
        assertEquals(Optional.empty(), data.get("police", owner, "badge"));
        data.put("police", owner, "badge", "{\"n\":1}");
        assertEquals(Optional.of("{\"n\":1}"), data.get("police", owner, "badge"));
        data.put("police", owner, "badge", "{\"n\":2}");
        assertEquals(Optional.of("{\"n\":2}"), data.get("police", owner, "badge"));
        data.delete("police", owner, "badge");
        assertEquals(Optional.empty(), data.get("police", owner, "badge"));
    }

    @Test
    void namespacesAreIsolatedAndPurgeable() {
        data.put("police", owner, "k", "1");
        data.put("ems", owner, "k", "2");
        assertEquals(1, data.purgeNamespace("police"));
        assertEquals(Optional.empty(), data.get("police", owner, "k"));
        assertEquals(Optional.of("2"), data.get("ems", owner, "k"));
    }
}
