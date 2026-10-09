package dev.qrm.api.character;

import dev.qrm.api.id.CharacterId;
import dev.qrm.api.id.PlayerId;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CharacterService {
    /** Il primo personaggio creato da un giocatore viene selezionato automaticamente. */
    CreateCharacterResult create(PlayerId player, String firstName, String lastName, LocalDate birthDate);

    SelectCharacterResult select(PlayerId player, CharacterId character);

    /** Soft-delete: lo stato diventa ARCHIVED. @return false se non trovato, non del giocatore o già archiviato. */
    boolean archive(PlayerId player, CharacterId character);

    Optional<PlayerCharacter> activeOf(PlayerId player);

    /** Tutti i personaggi del giocatore (anche archiviati), dal più vecchio al più recente. */
    List<PlayerCharacter> listOf(PlayerId player);

    Optional<PlayerCharacter> find(CharacterId id);

    /**
     * Personaggi non archiviati il cui nome completo ("Nome Cognome") coincide, senza distinzione di maiuscole e
     * ignorando spazi ai bordi. I nomi non sono necessariamente unici ({@code uniqueNames} è configurabile):
     * può tornare più di un risultato. Dal più vecchio al più recente.
     */
    List<PlayerCharacter> findByName(String fullName);
}
