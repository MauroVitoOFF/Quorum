package dev.qrm.core.job;

import dev.qrm.api.character.CharacterStatus;
import dev.qrm.api.id.CharacterId;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/** Query condivise dai servizi job/permessi (usano la connessione della transazione corrente). */
public final class CharacterQueries {
    private CharacterQueries() {}

    public static Optional<CharacterStatus> statusOf(Connection c, CharacterId id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT status FROM qrm_character WHERE id=?")) {
            ps.setString(1, id.value().toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(CharacterStatus.valueOf(rs.getString(1))) : Optional.empty();
            }
        }
    }
}
