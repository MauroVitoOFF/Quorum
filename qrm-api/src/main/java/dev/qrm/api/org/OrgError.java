package dev.qrm.api.org;

import dev.qrm.api.character.CharacterStatus;

/** Motivi attesi per cui un'operazione sulle organizzazioni non viene eseguita. */
public sealed interface OrgError {
    record UnknownOrg(String orgId) implements OrgError {}

    record Archived(String orgId) implements OrgError {}

    /** Operazione ammessa solo su organizzazioni archiviate. */
    record NotArchived(String orgId) implements OrgError {}

    /** L'organizzazione ha ancora membri o fondi sul conto. */
    record NotEmpty(String orgId) implements OrgError {}

    record Exists(String orgId) implements OrgError {}

    record UnknownRank(String rankId) implements OrgError {}

    record RankInUse(String rankId) implements OrgError {}

    /** Un altro rank dell'organizzazione ha già questo {@code level}. */
    record LevelTaken(int level) implements OrgError {}

    record UnknownCharacter() implements OrgError {}

    /** Il personaggio non è {@code ACTIVE}. */
    record NotUsable(CharacterStatus status) implements OrgError {}

    record AlreadyMember() implements OrgError {}

    record NotMember() implements OrgError {}

    record Cancelled(String by, String reason) implements OrgError {}
}
