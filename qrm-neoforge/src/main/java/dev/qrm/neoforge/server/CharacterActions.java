package dev.qrm.neoforge.server;

import dev.qrm.api.character.CharacterService;
import dev.qrm.api.character.CharacterStatus;
import dev.qrm.api.character.CreateCharacterResult;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.character.SelectCharacterResult;
import dev.qrm.api.id.CharacterId;
import dev.qrm.api.id.PlayerId;
import dev.qrm.api.job.Assignment;
import dev.qrm.api.job.JobDefinition;
import dev.qrm.api.job.JobRegistry;
import dev.qrm.api.job.JobService;
import dev.qrm.api.player.PlayerService;
import dev.qrm.api.player.QrmPlayer;
import dev.qrm.neoforge.net.CharacterCard;
import dev.qrm.neoforge.net.ResultCode;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Traduce le richieste della GUI in chiamate ai servizi del Core. Nessuna dipendenza da Minecraft. */
public final class CharacterActions {
    public static final LocalDate MIN_BIRTH = LocalDate.of(1900, 1, 1);

    public record Snapshot(List<CharacterCard> cards, int slots, Optional<UUID> activeId) {}

    public record Outcome(ResultCode code, Snapshot snapshot) {}

    private final CharacterService characters;
    private final PlayerService players;
    private final JobService jobs;
    private final JobRegistry jobRegistry;
    private final Clock clock;

    public CharacterActions(CharacterService characters, PlayerService players, JobService jobs,
            JobRegistry jobRegistry, Clock clock) {
        this.characters = characters;
        this.players = players;
        this.jobs = jobs;
        this.jobRegistry = jobRegistry;
        this.clock = clock;
    }

    /**
     * Solo i personaggi ACTIVE: archiviati e deceduti non sono selezionabili. Le carte sono limitate a
     * {@link CharacterCard#MAX_ROSTER_CARDS}: quelle oltre il limite non vengono mostrate.
     */
    public Snapshot snapshot(PlayerId player) {
        List<CharacterCard> cards = characters.listOf(player).stream()
                .filter(c -> c.status() == CharacterStatus.ACTIVE)
                .limit(CharacterCard.MAX_ROSTER_CARDS)
                .map(this::toCard)
                .toList();
        int slots = players.find(player).map(QrmPlayer::characterSlots).orElse(0);
        Optional<UUID> active = characters.activeOf(player).map(c -> c.id().value());
        return new Snapshot(cards, slots, active);
    }

    public Outcome select(PlayerId player, UUID characterId) {
        ResultCode code = switch (characters.select(player, new CharacterId(characterId))) {
            case SelectCharacterResult.Selected selected -> ResultCode.OK;
            case SelectCharacterResult.NotFound notFound -> ResultCode.NOT_FOUND;
            case SelectCharacterResult.NotOwner notOwner -> ResultCode.NOT_OWNER;
            case SelectCharacterResult.NotUsable notUsable -> ResultCode.NOT_USABLE;
        };
        return new Outcome(code, snapshot(player));
    }

    public Outcome create(PlayerId player, String firstName, String lastName, int year, int month, int day) {
        LocalDate birth;
        try {
            birth = LocalDate.of(year, month, day);
        } catch (DateTimeException e) {
            return new Outcome(ResultCode.INVALID_DATE, snapshot(player));
        }
        if (birth.isBefore(MIN_BIRTH) || birth.isAfter(LocalDate.now(clock))) {
            return new Outcome(ResultCode.INVALID_DATE, snapshot(player));
        }
        ResultCode code = switch (characters.create(player, firstName, lastName, birth)) {
            case CreateCharacterResult.Created created -> ResultCode.OK;
            case CreateCharacterResult.UnknownPlayer unknown -> ResultCode.ERROR;
            case CreateCharacterResult.SlotsFull full -> ResultCode.SLOTS_FULL;
            case CreateCharacterResult.InvalidName invalid -> ResultCode.INVALID_NAME;
            case CreateCharacterResult.NameTaken taken -> ResultCode.NAME_TAKEN;
        };
        return new Outcome(code, snapshot(player));
    }

    public Outcome archive(PlayerId player, UUID characterId) {
        boolean archived = characters.archive(player, new CharacterId(characterId));
        return new Outcome(archived ? ResultCode.OK : ResultCode.NOT_FOUND, snapshot(player));
    }

    private CharacterCard toCard(PlayerCharacter c) {
        UUID id = c.id().value();
        return new CharacterCard(id, c.firstName(), c.lastName(), c.birthDate(),
                CharacterCard.documentNumberOf(id), jobLabelOf(c));
    }

    private Optional<String> jobLabelOf(PlayerCharacter c) {
        return jobs.assignmentOf(c.id())
                .filter(Assignment::known)
                .flatMap(a -> jobRegistry.find(a.jobId()))
                .map(JobDefinition::label);
    }
}
