package dev.qrm.arrival.logic;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.character.CharacterStatus;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.id.CharacterId;
import dev.qrm.api.id.PlayerId;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ArrivalPolicyTest {
    static final Instant INSTALLED = Instant.parse("2026-11-03T10:00:00Z");

    static final class FakeStorage implements ArrivalStorage {
        final Set<CharacterId> arrived = new HashSet<>();

        @Override
        public boolean hasArrived(CharacterId c) {
            return arrived.contains(c);
        }

        @Override
        public void markArrived(CharacterId c) {
            arrived.add(c);
        }

        @Override
        public Instant installedAt() {
            return INSTALLED;
        }
    }

    final FakeStorage storage = new FakeStorage();
    final ArrivalPolicy policy = new ArrivalPolicy(storage);

    private static PlayerCharacter character(Instant createdAt, CharacterStatus status) {
        return new PlayerCharacter(new CharacterId(UUID.randomUUID()), new PlayerId(UUID.randomUUID()), "Anna", "Rossi",
                LocalDate.of(1990, 1, 1), status, createdAt);
    }

    @Test
    void newCharacterCreatedAfterInstall_shouldArrive() {
        assertTrue(policy.shouldArrive(character(INSTALLED.plusSeconds(60), CharacterStatus.ACTIVE)));
    }

    @Test
    void characterCreatedBeforeInstall_shouldNot() {
        assertFalse(policy.shouldArrive(character(INSTALLED.minusSeconds(60), CharacterStatus.ACTIVE)));
    }

    @Test
    void alreadyArrived_shouldNot() {
        PlayerCharacter c = character(INSTALLED.plusSeconds(60), CharacterStatus.ACTIVE);
        storage.markArrived(c.id());
        assertFalse(policy.shouldArrive(c));
    }

    @Test
    void archivedOrDeceasedCharacter_shouldNot() {
        assertFalse(policy.shouldArrive(character(INSTALLED.plusSeconds(60), CharacterStatus.ARCHIVED)));
        assertFalse(policy.shouldArrive(character(INSTALLED.plusSeconds(60), CharacterStatus.DECEASED)));
    }
}
