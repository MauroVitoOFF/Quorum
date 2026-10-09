package dev.qrm.api.org;

import dev.qrm.api.id.CharacterId;
import java.util.List;
import java.util.Optional;

/**
 * Organizzazioni, rank e membri. Non controlla chi chiama: per imporre regole usare {@link MembershipChangePreEvent}.
 * Le operazioni su organizzazioni archiviate sono rifiutate con {@link OrgError.Archived}.
 */
public interface OrganizationService {
    /** Crea l'organizzazione e il suo conto. @throws IllegalArgumentException se id o label sono malformati */
    OrgResult<Organization> create(String id, String label);

    Optional<Organization> find(String id);

    /** Ordinate per id. */
    List<Organization> all();

    /** Archivia e congela il conto. Idempotente: un'organizzazione già archiviata restituisce {@code Ok}. */
    OrgResult<Organization> archive(String id);

    /** Crea o sostituisce (stesso id) un rank. */
    OrgResult<Rank> defineRank(String orgId, Rank rank);

    OrgResult<Rank> removeRank(String orgId, String rankId);

    /** Ordinati per level crescente; vuota se l'organizzazione non esiste. */
    List<Rank> ranksOf(String orgId);

    OrgResult<Membership> addMember(String orgId, CharacterId character, String rankId);

    OrgResult<Membership> setRank(String orgId, CharacterId character, String rankId);

    OrgResult<Membership> removeMember(String orgId, CharacterId character);

    /** Solo membri espliciti con personaggio ACTIVE; i membri impliciti da job non compaiono. */
    List<Membership> membersOf(String orgId);

    /** Organizzazioni ACTIVE di un personaggio ACTIVE: membership esplicite più quella implicita del job in servizio. */
    List<Organization> organizationsOf(CharacterId character);

    /** @param node nodo esatto (mai jolly) @throws IllegalArgumentException se {@code node} non è un nodo esatto */
    boolean can(CharacterId character, String orgId, String node);
}
