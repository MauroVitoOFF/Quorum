package dev.qrm.core.org;

import dev.qrm.api.character.CharacterStatus;
import dev.qrm.api.economy.Account;
import dev.qrm.api.economy.AccountService;
import dev.qrm.api.economy.AccountStatus;
import dev.qrm.api.economy.OwnerRef;
import dev.qrm.api.event.EventBus;
import dev.qrm.api.id.AccountId;
import dev.qrm.api.id.CharacterId;
import dev.qrm.api.job.Assignment;
import dev.qrm.api.job.JobDefinition;
import dev.qrm.api.job.JobRegistry;
import dev.qrm.api.job.JobService;
import dev.qrm.api.org.*;
import dev.qrm.api.permission.PermissionNodes;
import dev.qrm.api.permission.PermissionService;
import dev.qrm.core.db.Database;
import dev.qrm.core.job.CharacterQueries;
import dev.qrm.core.job.PermissionMatcher;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

public final class DefaultOrganizationService implements OrganizationService {
    private static final String ORG_COLUMNS = "id, label, status, account_id, created_at";

    private final Database db;
    private final EventBus bus;
    private final AccountService accounts;
    private final JobRegistry jobRegistry;
    private final JobService jobs;
    private final PermissionService permissions;
    private final Clock clock;

    public DefaultOrganizationService(Database db, EventBus bus, AccountService accounts, JobRegistry jobRegistry,
            JobService jobs, PermissionService permissions, Clock clock) {
        this.db = db;
        this.bus = bus;
        this.accounts = accounts;
        this.jobRegistry = jobRegistry;
        this.jobs = jobs;
        this.permissions = permissions;
        this.clock = clock;
    }

    // ---- organizzazioni ------------------------------------------------------------------------------------

    @Override
    public OrgResult<Organization> create(String id, String label) {
        Organization.requireValid(id, label);
        if (find(id).isPresent()) return OrgResult.fail(new OrgError.Exists(id));
        // AccountService.create apre una propria transazione e tx() non si annida: prima il conto, poi la riga.
        // Un conto orfano (crash tra conto e riga, o retry) viene riusato invece di crearne un secondo.
        Account account = accounts.accountsOf(Organization.ownerOf(id)).stream()
                .filter(a -> !isReferenced(a.id()))
                .findFirst()
                .orElseGet(() -> accounts.create(Organization.ownerOf(id), false));
        Organization org = new Organization(id, label, OrgStatus.ACTIVE, account.id(),
                Instant.ofEpochMilli(clock.millis()));
        boolean inserted = db.tx(c -> {
            if (loadOrg(c, id).isPresent()) return false;
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO qrm_organization (" + ORG_COLUMNS + ") VALUES (?,?,?,?,?)")) {
                ps.setString(1, org.id());
                ps.setString(2, org.label());
                ps.setString(3, org.status().name());
                ps.setString(4, org.account().value().toString());
                ps.setLong(5, org.createdAt().toEpochMilli());
                ps.executeUpdate();
            }
            return true;
        });
        if (!inserted) return OrgResult.fail(new OrgError.Exists(id));
        bus.post(new OrganizationCreatedEvent(org));
        return OrgResult.ok(org);
    }

    private boolean isReferenced(AccountId account) {
        return db.read(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM qrm_organization WHERE account_id=?")) {
                ps.setString(1, account.value().toString());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    @Override
    public Optional<Organization> find(String id) {
        return db.read(c -> loadOrg(c, id));
    }

    @Override
    public List<Organization> all() {
        return db.read(c -> {
            List<Organization> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT " + ORG_COLUMNS + " FROM qrm_organization ORDER BY id");
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapOrg(rs));
            }
            return out;
        });
    }

    @Override
    public OrgResult<Organization> archive(String id) {
        Optional<Organization> org = find(id);
        if (org.isEmpty()) return OrgResult.fail(new OrgError.UnknownOrg(id));
        boolean wasFrozen = accounts.find(org.get().account())
                .map(a -> a.status() == AccountStatus.FROZEN).orElse(false);
        if (org.get().status() == OrgStatus.ARCHIVED && wasFrozen) return OrgResult.ok(org.get());
        // Prima il conto, poi la riga: nessuna finestra con organizzazione ARCHIVED e conto ancora prelevabile.
        accounts.setStatus(org.get().account(), AccountStatus.FROZEN);
        boolean changed = db.tx(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE qrm_organization SET status='ARCHIVED' WHERE id=? AND status='ACTIVE'")) {
                ps.setString(1, id);
                return ps.executeUpdate() > 0;
            }
        });
        Organization archived = find(id).orElseThrow();
        if (changed || !wasFrozen) bus.post(new OrganizationArchivedEvent(archived));
        return OrgResult.ok(archived);
    }

    @Override
    public OrgResult<Organization> rename(String id, String newId, String newLabel) {
        Organization.requireValid(newId, newLabel == null ? "x" : newLabel);
        return db.<OrgResult<Organization>>tx(c -> {
            Optional<Organization> old = loadOrg(c, id);
            if (old.isEmpty()) return OrgResult.fail(new OrgError.UnknownOrg(id));
            if (id.equals(newId)) {
                if (newLabel == null) return OrgResult.ok(old.get());
                exec(c, "UPDATE qrm_organization SET label=? WHERE id=?", newLabel, id);
                return OrgResult.ok(loadOrg(c, id).orElseThrow());
            }
            if (loadOrg(c, newId).isPresent()) return OrgResult.fail(new OrgError.Exists(newId));
            Organization o = old.get();
            String label = newLabel == null ? o.label() : newLabel;
            // Le tabelle figlie puntano all'id con una FK: prima la nuova riga, poi i figli, infine la vecchia riga.
            exec(c, "INSERT INTO qrm_organization (" + ORG_COLUMNS + ") VALUES (?,?,?,?,?)",
                    newId, label, o.status().name(), o.account().value().toString(), o.createdAt().toEpochMilli());
            exec(c, "UPDATE qrm_org_rank SET org_id=? WHERE org_id=?", newId, id);
            exec(c, "UPDATE qrm_org_member SET org_id=? WHERE org_id=?", newId, id);
            exec(c, "DELETE FROM qrm_organization WHERE id=?", id);
            String oldOwner = Organization.ownerOf(id).id().toString();
            String newOwner = Organization.ownerOf(newId).id().toString();
            exec(c, "UPDATE qrm_account SET owner_id=? WHERE owner_type=? AND owner_id=?",
                    newOwner, OwnerRef.ORGANIZATION, oldOwner);
            exec(c, "UPDATE qrm_mod_data SET owner_id=? WHERE owner_type=? AND owner_id=?",
                    newOwner, OwnerRef.ORGANIZATION, oldOwner);
            exec(c, "UPDATE qrm_interaction_point SET owner_id=? WHERE owner_kind='ORGANIZATION' AND owner_id=?",
                    newId, id);
            return OrgResult.ok(loadOrg(c, newId).orElseThrow());
        });
    }

    @Override
    public OrgResult<Organization> delete(String id) {
        Optional<Organization> org = find(id);
        if (org.isEmpty()) return OrgResult.fail(new OrgError.UnknownOrg(id));
        if (org.get().status() != OrgStatus.ACTIVE
                && accounts.balances(org.get().account()).stream().anyMatch(m -> m.amount() != 0)) {
            return OrgResult.fail(new OrgError.NotEmpty(id));
        }
        return db.<OrgResult<Organization>>tx(c -> {
            Optional<Organization> o = loadOrg(c, id);
            if (o.isEmpty()) return OrgResult.fail(new OrgError.UnknownOrg(id));
            if (o.get().status() != OrgStatus.ARCHIVED) return OrgResult.fail(new OrgError.NotArchived(id));
            try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM qrm_org_member WHERE org_id=? LIMIT 1")) {
                ps.setString(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return OrgResult.fail(new OrgError.NotEmpty(id));
                }
            }
            String owner = Organization.ownerOf(id).id().toString();
            exec(c, "DELETE FROM qrm_org_rank WHERE org_id=?", id);
            exec(c, "DELETE FROM qrm_interaction_point WHERE owner_kind='ORGANIZATION' AND owner_id=?", id);
            exec(c, "DELETE FROM qrm_mod_data WHERE owner_type=? AND owner_id=?", OwnerRef.ORGANIZATION, owner);
            exec(c, "DELETE FROM qrm_organization WHERE id=?", id);
            // Il conto congelato resta per lo storico, ma sganciato dall'id così una nuova org non lo riusa.
            exec(c, "UPDATE qrm_account SET owner_id=? WHERE owner_type=? AND owner_id=?",
                    UUID.randomUUID().toString(), OwnerRef.ORGANIZATION, owner);
            return OrgResult.ok(o.get());
        });
    }

    private static void exec(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            ps.executeUpdate();
        }
    }

    // ---- rank ----------------------------------------------------------------------------------------------

    @Override
    public OrgResult<Rank> defineRank(String orgId, Rank rank) {
        return db.<OrgResult<Rank>>tx(c -> {
            OrgError gate = gateIn(c, orgId);
            if (gate != null) return OrgResult.fail(gate);
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT 1 FROM qrm_org_rank WHERE org_id=? AND level=? AND rank_id<>?")) {
                ps.setString(1, orgId);
                ps.setInt(2, rank.level());
                ps.setString(3, rank.id());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return OrgResult.fail(new OrgError.LevelTaken(rank.level()));
                }
            }
            String perms = String.join("\n", new TreeSet<>(rank.permissions()));
            boolean exists = loadRank(c, orgId, rank.id()).isPresent();
            String sql = exists
                    ? "UPDATE qrm_org_rank SET label=?, level=?, permissions=? WHERE org_id=? AND rank_id=?"
                    : "INSERT INTO qrm_org_rank (label, level, permissions, org_id, rank_id) VALUES (?,?,?,?,?)";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, rank.label());
                ps.setInt(2, rank.level());
                ps.setString(3, perms);
                ps.setString(4, orgId);
                ps.setString(5, rank.id());
                ps.executeUpdate();
            }
            return OrgResult.ok(rank);
        });
    }

    @Override
    public OrgResult<Rank> removeRank(String orgId, String rankId) {
        return db.<OrgResult<Rank>>tx(c -> {
            OrgError gate = gateIn(c, orgId);
            if (gate != null) return OrgResult.fail(gate);
            Optional<Rank> existing = loadRank(c, orgId, rankId);
            if (existing.isEmpty()) return OrgResult.fail(new OrgError.UnknownRank(rankId));
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT 1 FROM qrm_org_member WHERE org_id=? AND rank_id=? LIMIT 1")) {
                ps.setString(1, orgId);
                ps.setString(2, rankId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return OrgResult.fail(new OrgError.RankInUse(rankId));
                }
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM qrm_org_rank WHERE org_id=? AND rank_id=?")) {
                ps.setString(1, orgId);
                ps.setString(2, rankId);
                ps.executeUpdate();
            }
            return OrgResult.ok(existing.get());
        });
    }

    @Override
    public List<Rank> ranksOf(String orgId) {
        return db.read(c -> loadRanks(c, orgId));
    }

    // ---- membri --------------------------------------------------------------------------------------------

    @Override
    public OrgResult<Membership> addMember(String orgId, CharacterId character, String rankId) {
        OrgError gate = gate(orgId);
        if (gate != null) return OrgResult.fail(gate);
        Optional<CharacterStatus> status = db.read(c -> CharacterQueries.statusOf(c, character));
        if (status.isEmpty()) return OrgResult.fail(new OrgError.UnknownCharacter());
        if (status.get() != CharacterStatus.ACTIVE) return OrgResult.fail(new OrgError.NotUsable(status.get()));
        if (db.read(c -> loadRank(c, orgId, rankId)).isEmpty()) return OrgResult.fail(new OrgError.UnknownRank(rankId));
        if (db.read(c -> loadMember(c, orgId, character)).isPresent()) return OrgResult.fail(new OrgError.AlreadyMember());

        MembershipChangePreEvent pre = bus.post(new MembershipChangePreEvent(orgId, character, null, rankId));
        if (pre.isCancelled()) return OrgResult.fail(new OrgError.Cancelled(pre.cancelledBy(), pre.cancelReason()));

        Membership member = new Membership(orgId, character, rankId, Instant.ofEpochMilli(clock.millis()));
        // Controlli ripetuti dentro la transazione: organizzazione e rank possono cambiare dopo i pre-check.
        OrgError failure = db.<OrgError>tx(c -> {
            OrgError g = gateIn(c, orgId);
            if (g != null) return g;
            if (loadRank(c, orgId, rankId).isEmpty()) return new OrgError.UnknownRank(rankId);
            if (loadMember(c, orgId, character).isPresent()) return new OrgError.AlreadyMember();
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO qrm_org_member (org_id, character_id, rank_id, since) VALUES (?,?,?,?)")) {
                ps.setString(1, orgId);
                ps.setString(2, character.value().toString());
                ps.setString(3, rankId);
                ps.setLong(4, member.since().toEpochMilli());
                ps.executeUpdate();
            }
            return null;
        });
        if (failure != null) return OrgResult.fail(failure);
        bus.post(new MembershipChangedEvent(orgId, character, null, member));
        return OrgResult.ok(member);
    }

    @Override
    public OrgResult<Membership> setRank(String orgId, CharacterId character, String rankId) {
        OrgError gate = gate(orgId);
        if (gate != null) return OrgResult.fail(gate);
        Optional<Membership> before = db.read(c -> loadMember(c, orgId, character));
        if (before.isEmpty()) return OrgResult.fail(new OrgError.NotMember());
        if (db.read(c -> loadRank(c, orgId, rankId)).isEmpty()) return OrgResult.fail(new OrgError.UnknownRank(rankId));
        if (before.get().rankId().equals(rankId)) return OrgResult.ok(before.get());

        MembershipChangePreEvent pre = bus.post(new MembershipChangePreEvent(orgId, character, before.get().rankId(), rankId));
        if (pre.isCancelled()) return OrgResult.fail(new OrgError.Cancelled(pre.cancelledBy(), pre.cancelReason()));

        OrgError failure = db.<OrgError>tx(c -> {
            OrgError g = gateIn(c, orgId);
            if (g != null) return g;
            if (loadRank(c, orgId, rankId).isEmpty()) return new OrgError.UnknownRank(rankId);
            if (loadMember(c, orgId, character).isEmpty()) return new OrgError.NotMember();
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE qrm_org_member SET rank_id=? WHERE org_id=? AND character_id=?")) {
                ps.setString(1, rankId);
                ps.setString(2, orgId);
                ps.setString(3, character.value().toString());
                ps.executeUpdate();
            }
            return null;
        });
        if (failure != null) return OrgResult.fail(failure);
        Membership after = new Membership(orgId, character, rankId, before.get().since());
        bus.post(new MembershipChangedEvent(orgId, character, before.get(), after));
        return OrgResult.ok(after);
    }

    @Override
    public OrgResult<Membership> removeMember(String orgId, CharacterId character) {
        OrgError gate = gate(orgId);
        if (gate != null) return OrgResult.fail(gate);
        Optional<Membership> before = db.read(c -> loadMember(c, orgId, character));
        if (before.isEmpty()) return OrgResult.fail(new OrgError.NotMember());

        MembershipChangePreEvent pre = bus.post(new MembershipChangePreEvent(orgId, character, before.get().rankId(), null));
        if (pre.isCancelled()) return OrgResult.fail(new OrgError.Cancelled(pre.cancelledBy(), pre.cancelReason()));

        OrgError failure = db.<OrgError>tx(c -> {
            OrgError g = gateIn(c, orgId);
            if (g != null) return g;
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM qrm_org_member WHERE org_id=? AND character_id=?")) {
                ps.setString(1, orgId);
                ps.setString(2, character.value().toString());
                return ps.executeUpdate() > 0 ? null : new OrgError.NotMember();
            }
        });
        if (failure != null) return OrgResult.fail(failure);
        bus.post(new MembershipChangedEvent(orgId, character, before.get(), null));
        return OrgResult.ok(before.get());
    }

    @Override
    public List<Membership> membersOf(String orgId) {
        return db.read(c -> {
            List<Membership> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT m.character_id, m.rank_id, m.since FROM qrm_org_member m "
                            + "JOIN qrm_character ch ON ch.id=m.character_id "
                            + "WHERE m.org_id=? AND ch.status='ACTIVE' ORDER BY m.since, m.character_id")) {
                ps.setString(1, orgId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        out.add(new Membership(orgId, new CharacterId(UUID.fromString(rs.getString(1))), rs.getString(2),
                                Instant.ofEpochMilli(rs.getLong(3))));
                    }
                }
            }
            return out;
        });
    }

    // ---- appartenenza e permessi (Task 6) ------------------------------------------------------------------

    @Override
    public List<Organization> organizationsOf(CharacterId character) {
        if (!isActive(character)) return List.of();
        List<Organization> out = db.read(c -> {
            List<Organization> list = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT o.id, o.label, o.status, o.account_id, o.created_at FROM qrm_organization o "
                            + "JOIN qrm_org_member m ON m.org_id=o.id "
                            + "WHERE m.character_id=? AND o.status='ACTIVE'")) {
                ps.setString(1, character.value().toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(mapOrg(rs));
                }
            }
            return list;
        });
        implicitOrgId(character).flatMap(this::find)
                .filter(o -> o.status() == OrgStatus.ACTIVE)
                .filter(o -> out.stream().noneMatch(x -> x.id().equals(o.id())))
                .ifPresent(out::add);
        out.sort(Comparator.comparing(Organization::id));
        return List.copyOf(out);
    }

    @Override
    public boolean can(CharacterId character, String orgId, String node) {
        if (!PermissionNodes.isExactNode(node)) throw new IllegalArgumentException("not an exact permission node: " + node);
        Optional<Organization> org = find(orgId);
        if (org.isEmpty() || org.get().status() != OrgStatus.ACTIVE) return false;
        if (!isActive(character)) return false;
        if (explicitAllows(orgId, character, node)) return true;
        return implicitOrgId(character).filter(orgId::equals).isPresent() && permissions.has(character, node);
    }

    private boolean isActive(CharacterId character) {
        return db.read(c -> CharacterQueries.statusOf(c, character)).filter(s -> s == CharacterStatus.ACTIVE).isPresent();
    }

    /** Organizzazione collegata al job noto e in servizio del personaggio, se esiste. */
    private Optional<String> implicitOrgId(CharacterId character) {
        Optional<Assignment> a = jobs.assignmentOf(character);
        if (a.isEmpty() || !a.get().known() || !a.get().onDuty()) return Optional.empty();
        return jobRegistry.find(a.get().jobId()).map(JobDefinition::orgId);
    }

    /** Unione dei nodi di tutti i rank con level &lt;= a quello del membro esplicito. */
    private boolean explicitAllows(String orgId, CharacterId character, String node) {
        return db.read(c -> {
            Optional<Membership> member = loadMember(c, orgId, character);
            if (member.isEmpty()) return false;
            List<Rank> ranks = loadRanks(c, orgId);
            Optional<Rank> mine = ranks.stream().filter(r -> r.id().equals(member.get().rankId())).findFirst();
            if (mine.isEmpty()) return false;
            return ranks.stream().filter(r -> r.level() <= mine.get().level())
                    .flatMap(r -> r.permissions().stream())
                    .anyMatch(p -> PermissionMatcher.matches(p, node));
        });
    }

    // ---- helper --------------------------------------------------------------------------------------------

    /** @return null se l'organizzazione esiste ed è attiva, altrimenti l'errore corrispondente */
    private OrgError gate(String orgId) {
        Optional<Organization> org = find(orgId);
        if (org.isEmpty()) return new OrgError.UnknownOrg(orgId);
        if (org.get().status() != OrgStatus.ACTIVE) return new OrgError.Archived(orgId);
        return null;
    }

    /** Come {@link #gate} ma dentro una transazione già aperta (controllo atomico con la scrittura). */
    private static OrgError gateIn(Connection c, String orgId) throws SQLException {
        Optional<Organization> org = loadOrg(c, orgId);
        if (org.isEmpty()) return new OrgError.UnknownOrg(orgId);
        if (org.get().status() != OrgStatus.ACTIVE) return new OrgError.Archived(orgId);
        return null;
    }

    private static Optional<Organization> loadOrg(Connection c, String id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT " + ORG_COLUMNS + " FROM qrm_organization WHERE id=?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapOrg(rs)) : Optional.empty();
            }
        }
    }

    private static Organization mapOrg(ResultSet rs) throws SQLException {
        return new Organization(rs.getString(1), rs.getString(2), OrgStatus.valueOf(rs.getString(3)),
                new AccountId(UUID.fromString(rs.getString(4))), Instant.ofEpochMilli(rs.getLong(5)));
    }

    private static Optional<Rank> loadRank(Connection c, String orgId, String rankId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT rank_id, label, level, permissions FROM qrm_org_rank WHERE org_id=? AND rank_id=?")) {
            ps.setString(1, orgId);
            ps.setString(2, rankId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRank(rs)) : Optional.empty();
            }
        }
    }

    private static Optional<Membership> loadMember(Connection c, String orgId, CharacterId character) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT rank_id, since FROM qrm_org_member WHERE org_id=? AND character_id=?")) {
            ps.setString(1, orgId);
            ps.setString(2, character.value().toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                        ? Optional.of(new Membership(orgId, character, rs.getString(1), Instant.ofEpochMilli(rs.getLong(2))))
                        : Optional.empty();
            }
        }
    }

    private static List<Rank> loadRanks(Connection c, String orgId) throws SQLException {
        List<Rank> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT rank_id, label, level, permissions FROM qrm_org_rank WHERE org_id=? ORDER BY level")) {
            ps.setString(1, orgId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapRank(rs));
            }
        }
        return out;
    }

    private static Rank mapRank(ResultSet rs) throws SQLException {
        String stored = rs.getString(4);
        Set<String> perms = stored.isEmpty() ? Set.of() : new HashSet<>(Arrays.asList(stored.split("\n")));
        return new Rank(rs.getString(1), rs.getString(2), rs.getInt(3), perms);
    }
}
