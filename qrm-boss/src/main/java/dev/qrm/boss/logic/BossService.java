package dev.qrm.boss.logic;

import dev.qrm.api.Money;
import dev.qrm.api.character.CharacterService;
import dev.qrm.api.character.CharacterStatus;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.economy.Account;
import dev.qrm.api.economy.AccountService;
import dev.qrm.api.economy.Currency;
import dev.qrm.api.economy.CurrencyRegistry;
import dev.qrm.api.economy.OwnerRef;
import dev.qrm.api.economy.TransactionService;
import dev.qrm.api.economy.TransferRequest;
import dev.qrm.api.economy.TransferResult;
import dev.qrm.api.id.CharacterId;
import dev.qrm.api.job.AssignResult;
import dev.qrm.api.job.Assignment;
import dev.qrm.api.job.GradeDefinition;
import dev.qrm.api.job.JobDefinition;
import dev.qrm.api.job.JobRegistry;
import dev.qrm.api.job.JobService;
import dev.qrm.api.org.BankResult;
import dev.qrm.api.org.Membership;
import dev.qrm.api.org.OrgError;
import dev.qrm.api.org.OrgResult;
import dev.qrm.api.org.Organization;
import dev.qrm.api.org.OrganizationBankService;
import dev.qrm.api.org.OrganizationService;
import dev.qrm.api.org.OrgStatus;
import dev.qrm.api.org.Rank;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Regole del menu boss, senza Minecraft: snapshot e azioni su membri e conto. Non ha stato: ogni chiamata rilegge i
 * servizi del Core, che restano la fonte di verità. Chi chiama passa il Character già verificato come attivo.
 *
 * <p>I membri si gestiscono in due modi. Se l'azienda ha un Job collegato (ogni Job dei file ha la sua azienda, con lo
 * stesso id), i membri sono i Character con quel Job e i "rank" sono i gradi: aggiungere assegna il grado più basso,
 * cambiare il rank cambia il grado, licenziare toglie il Job. Altrimenti (gang e gruppi) sono i rank dell'azienda.
 */
public final class BossService {
    static final String DEPOSIT_REASON = "Deposito aziendale";

    private final OrganizationService orgs;
    private final OrganizationBankService bank;
    private final AccountService accounts;
    private final TransactionService txs;
    private final CharacterService characters;
    private final CurrencyRegistry currencies;
    private final JobService jobs;
    private final JobRegistry jobRegistry;

    public BossService(OrganizationService orgs, OrganizationBankService bank, AccountService accounts,
            TransactionService txs, CharacterService characters, CurrencyRegistry currencies, JobService jobs,
            JobRegistry jobRegistry) {
        this.orgs = orgs;
        this.bank = bank;
        this.accounts = accounts;
        this.txs = txs;
        this.characters = characters;
        this.currencies = currencies;
        this.jobs = jobs;
        this.jobRegistry = jobRegistry;
    }

    // ---- aperture e snapshot -----------------------------------------------------------------------------

    /** Le aziende per cui il Character ha {@code boss.menu}, per id. */
    public List<Organization> menuOrganizations(CharacterId viewer) {
        return orgs.organizationsOf(viewer).stream()
                .filter(o -> orgs.can(viewer, o.id(), BossPermissions.MENU))
                .sorted(Comparator.comparing(Organization::id))
                .toList();
    }

    /** Vero se il Character può aprire il menu di questa azienda. */
    public boolean canOpen(CharacterId viewer, String orgId) {
        return orgs.can(viewer, orgId, BossPermissions.MENU);
    }

    /**
     * Snapshot per il Character, o vuoto se non ha {@code boss.menu} (azienda archiviata, inesistente o non sua).
     *
     * @param online Character attivi dei giocatori online: da qui vengono i candidati da aggiungere
     */
    public Optional<BossSnapshot> snapshot(CharacterId viewer, String orgId, Collection<PlayerCharacter> online) {
        BossPermissions.Sections sections = BossPermissions.sections(node -> orgs.can(viewer, orgId, node));
        if (!sections.overview()) return Optional.empty();
        Optional<Organization> found = orgs.find(orgId);
        if (found.isEmpty()) return Optional.empty();
        Organization org = found.get();

        Optional<JobDefinition> job = jobFor(orgId);
        Roster roster = job.isPresent() ? jobRoster(viewer, job.get()) : rankRoster(viewer, orgId);
        int level = roster.level();

        Optional<Currency> currency = currency();
        boolean canView = orgs.can(viewer, orgId, BossPermissions.BANK_VIEW);
        boolean hasBalance = sections.account() && canView && currency.isPresent();
        String balance = hasBalance ? currency.get().format(
                accounts.balance(org.account(), currency.get().code()).amount()) : "";

        List<BossSnapshot.Member> memberViews = new ArrayList<>();
        List<BossSnapshot.RankOption> options = new ArrayList<>();
        List<BossSnapshot.Candidate> candidates = new ArrayList<>();
        if (sections.members()) {
            for (Entry e : roster.entries()) {
                boolean editable = level >= 0 && !e.character().equals(viewer) && e.known() && e.level() <= level;
                memberViews.add(new BossSnapshot.Member(e.character().value(), nameOf(e.character()), e.rankId(),
                        e.rankLabel(), e.known() ? e.level() : -1, editable));
            }
            memberViews.sort(Comparator.comparingInt(BossSnapshot.Member::level).reversed()
                    .thenComparing(BossSnapshot.Member::name));
            if (level >= 0) {
                for (Option o : roster.options()) {
                    if (o.level() <= level) options.add(new BossSnapshot.RankOption(o.id(), o.label(), o.level()));
                }
                Set<CharacterId> memberIds = new HashSet<>();
                roster.entries().forEach(e -> memberIds.add(e.character()));
                for (PlayerCharacter pc : online) {
                    if (pc.status() == CharacterStatus.ACTIVE && !memberIds.contains(pc.id())
                            && (job.isEmpty() || unemployed(pc.id()))) {
                        candidates.add(new BossSnapshot.Candidate(pc.id().value(), fullName(pc)));
                    }
                }
                candidates.sort(Comparator.comparing(BossSnapshot.Candidate::name));
            }
        }
        return Optional.of(new BossSnapshot(org.id(), org.label(), org.status().name(), viewer.value(),
                roster.viewerLabel(), level, sections.members(), sections.account(), roster.entries().size(),
                hasBalance, balance, currency.map(Currency::code).orElse(""),
                sections.account() && orgs.can(viewer, orgId, BossPermissions.BANK_DEPOSIT) && currency.isPresent(),
                sections.account() && orgs.can(viewer, orgId, BossPermissions.WITHDRAW) && currency.isPresent(),
                cap(memberViews, BossSnapshot.MAX_MEMBERS), cap(options, BossSnapshot.MAX_RANKS),
                cap(candidates, BossSnapshot.MAX_CANDIDATES)));
    }

    private static <T> List<T> cap(List<T> list, int max) {
        return List.copyOf(list.size() > max ? list.subList(0, max) : list);
    }

    // ---- membri ------------------------------------------------------------------------------------------

    /** Aggiunge un Character con il rank (o il grado) assegnabile di livello più basso. */
    public BossResult addMember(CharacterId viewer, String orgId, CharacterId target) {
        BossResult gate = gate(viewer, orgId, BossPermissions.MEMBERS_MANAGE);
        if (gate != BossResult.OK) return gate;
        Optional<JobDefinition> job = jobFor(orgId);
        Roster roster = job.isPresent() ? jobRoster(viewer, job.get()) : rankRoster(viewer, orgId);
        if (roster.level() < 0) return BossResult.READ_ONLY;
        if (target.equals(viewer)) return BossResult.ALREADY_MEMBER;
        Optional<Option> lowest = roster.options().stream().filter(o -> o.level() <= roster.level())
                .min(Comparator.comparingInt(Option::level));
        if (lowest.isEmpty()) return BossResult.UNKNOWN_RANK;
        if (job.isEmpty()) return map(orgs.addMember(orgId, target, lowest.get().id()));
        if (roster.entries().stream().anyMatch(e -> e.character().equals(target))) return BossResult.ALREADY_MEMBER;
        if (!unemployed(target)) return BossResult.HAS_JOB;
        return map(jobs.assign(target, job.get().id(), lowest.get().id()));
    }

    public BossResult setRank(CharacterId viewer, String orgId, CharacterId target, String rankId) {
        BossResult gate = gate(viewer, orgId, BossPermissions.MEMBERS_MANAGE);
        if (gate != BossResult.OK) return gate;
        Optional<JobDefinition> job = jobFor(orgId);
        Roster roster = job.isPresent() ? jobRoster(viewer, job.get()) : rankRoster(viewer, orgId);
        BossResult target0 = checkTarget(viewer, target, roster);
        if (target0 != BossResult.OK) return target0;
        Optional<Option> wanted = roster.options().stream().filter(o -> o.id().equals(rankId)).findFirst();
        if (wanted.isEmpty()) return BossResult.UNKNOWN_RANK;
        if (wanted.get().level() > roster.level()) return BossResult.LEVEL_TOO_HIGH;
        if (job.isEmpty()) return map(orgs.setRank(orgId, target, rankId));
        return map(jobs.setGrade(target, rankId));
    }

    public BossResult removeMember(CharacterId viewer, String orgId, CharacterId target) {
        BossResult gate = gate(viewer, orgId, BossPermissions.MEMBERS_MANAGE);
        if (gate != BossResult.OK) return gate;
        Optional<JobDefinition> job = jobFor(orgId);
        Roster roster = job.isPresent() ? jobRoster(viewer, job.get()) : rankRoster(viewer, orgId);
        BossResult target0 = checkTarget(viewer, target, roster);
        if (target0 != BossResult.OK) return target0;
        if (job.isEmpty()) return map(orgs.removeMember(orgId, target));
        return jobs.dismiss(target) ? BossResult.OK : BossResult.ERROR;
    }

    /** Vale per ogni azione su un membro: chi agisce ha un livello, il bersaglio è un altro membro di livello <=. */
    private static BossResult checkTarget(CharacterId viewer, CharacterId target, Roster roster) {
        if (roster.level() < 0) return BossResult.READ_ONLY;
        if (target.equals(viewer)) return BossResult.SELF;
        Optional<Entry> entry = roster.entries().stream().filter(e -> e.character().equals(target)).findFirst();
        if (entry.isEmpty()) return BossResult.NOT_MEMBER;
        if (!entry.get().known() || entry.get().level() > roster.level()) return BossResult.LEVEL_TOO_HIGH;
        return BossResult.OK;
    }

    // ---- elenco dei membri: rank dell'azienda o gradi del Job ---------------------------------------------

    /** Un'opzione assegnabile (rank o grado). */
    private record Option(String id, String label, int level) {}

    /** Un membro con il suo rank o grado; {@code known} è falso se il rank o il grado non esistono più. */
    private record Entry(CharacterId character, String rankId, String rankLabel, int level, boolean known) {}

    /** Chi guarda ({@code level} -1 se non ha un rank o un grado esplicito) e i membri con le opzioni assegnabili. */
    private record Roster(int level, String viewerLabel, List<Entry> entries, List<Option> options) {}

    private Roster rankRoster(CharacterId viewer, String orgId) {
        List<Rank> ranks = orgs.ranksOf(orgId);
        List<Membership> members = orgs.membersOf(orgId);
        Optional<Rank> mine = rankOf(viewer, members, ranks);
        List<Entry> entries = new ArrayList<>();
        for (Membership m : members) {
            Optional<Rank> rank = ranks.stream().filter(r -> r.id().equals(m.rankId())).findFirst();
            entries.add(new Entry(m.character(), m.rankId(), rank.map(Rank::label).orElse(m.rankId()),
                    rank.map(Rank::level).orElse(Integer.MAX_VALUE), rank.isPresent()));
        }
        List<Option> options = ranks.stream().map(r -> new Option(r.id(), r.label(), r.level())).toList();
        return new Roster(mine.map(Rank::level).orElse(-1), mine.map(Rank::label).orElse(""), entries, options);
    }

    private Roster jobRoster(CharacterId viewer, JobDefinition job) {
        Optional<GradeDefinition> mine = gradeOf(viewer, job);
        List<Entry> entries = new ArrayList<>();
        for (CharacterId id : jobs.membersOf(job.id())) {
            Optional<GradeDefinition> grade = gradeOf(id, job);
            String gradeId = jobs.assignmentOf(id).map(Assignment::gradeId).orElse("");
            entries.add(new Entry(id, gradeId, grade.map(GradeDefinition::label).orElse(gradeId),
                    grade.map(GradeDefinition::level).orElse(Integer.MAX_VALUE), grade.isPresent()));
        }
        List<Option> options = job.grades().stream().map(g -> new Option(g.id(), g.label(), g.level())).toList();
        return new Roster(mine.map(GradeDefinition::level).orElse(-1), mine.map(GradeDefinition::label).orElse(""),
                entries, options);
    }

    private Optional<GradeDefinition> gradeOf(CharacterId id, JobDefinition job) {
        return jobs.assignmentOf(id).filter(a -> a.jobId().equals(job.id())).flatMap(a -> job.grade(a.gradeId()));
    }

    /** Senza lavoro, o con un lavoro che non esiste più: può essere assunto. */
    private boolean unemployed(CharacterId id) {
        return jobs.assignmentOf(id).map(a -> !a.known()).orElse(true);
    }

    /** Il Job che gestisce l'azienda: quello con lo stesso id, altrimenti l'unico collegato ad essa. */
    private Optional<JobDefinition> jobFor(String orgId) {
        Optional<JobDefinition> same = jobRegistry.find(orgId).filter(j -> orgId.equals(j.orgId()));
        if (same.isPresent()) return same;
        List<JobDefinition> linked = jobRegistry.all().stream().filter(j -> orgId.equals(j.orgId())).toList();
        return linked.size() == 1 ? Optional.of(linked.get(0)) : Optional.empty();
    }

    // ---- conto -------------------------------------------------------------------------------------------

    /** Trasferisce dal conto personale del Character al conto dell'azienda. */
    public BossResult deposit(CharacterId viewer, String orgId, String amountText, UUID requestId) {
        BossResult gate = gate(viewer, orgId, BossPermissions.BANK_DEPOSIT);
        if (gate != BossResult.OK) return gate;
        Optional<Currency> currency = currency();
        if (currency.isEmpty()) return BossResult.NO_CURRENCY;
        Optional<Account> personal = personalAccount(viewer);
        if (personal.isEmpty()) return BossResult.NO_ACCOUNT;
        Optional<Organization> org = orgs.find(orgId);
        if (org.isEmpty()) return BossResult.NO_ORG;
        long base = accounts.balance(personal.get().id(), currency.get().code()).amount();
        BossResult amount = checkAmount(amountText, base, currency.get());
        if (amount != BossResult.OK) return amount;
        long value = AmountSpec.resolve(amountText, base, currency.get());
        TransferResult result = txs.transfer(TransferRequest.builder()
                .from(personal.get().id())
                .to(org.get().account())
                .amount(Money.of(currency.get().code(), value))
                .reason(DEPOSIT_REASON)
                .idempotencyKey(key(orgId, "deposit", requestId))
                .build());
        return map(result);
    }

    /** Preleva dal conto dell'azienda verso il conto personale; il permesso è ricontrollato dal Core. */
    public BossResult withdraw(CharacterId viewer, String orgId, String amountText, UUID requestId) {
        BossResult gate = gate(viewer, orgId, BossPermissions.WITHDRAW);
        if (gate != BossResult.OK) return gate;
        Optional<Currency> currency = currency();
        if (currency.isEmpty()) return BossResult.NO_CURRENCY;
        Optional<Account> personal = personalAccount(viewer);
        if (personal.isEmpty()) return BossResult.NO_ACCOUNT;
        Optional<Organization> org = orgs.find(orgId);
        if (org.isEmpty()) return BossResult.NO_ORG;
        long base = accounts.balance(org.get().account(), currency.get().code()).amount();
        BossResult amount = checkAmount(amountText, base, currency.get());
        if (amount != BossResult.OK) return amount;
        long value = AmountSpec.resolve(amountText, base, currency.get());
        BankResult result = bank.transfer(viewer, orgId, personal.get().id(),
                Money.of(currency.get().code(), value), key(orgId, "withdraw", requestId));
        return switch (result) {
            case BankResult.Transferred t -> map(t.result());
            case BankResult.Denied d -> BossResult.NO_PERMISSION;
            case BankResult.Failed f -> map(f.error());
        };
    }

    private static BossResult checkAmount(String text, long base, Currency currency) {
        long value = AmountSpec.resolve(text, base, currency);
        if (value <= 0) return BossResult.INVALID_AMOUNT;
        return value > base ? BossResult.INSUFFICIENT_FUNDS : BossResult.OK;
    }

    static String key(String orgId, String kind, UUID requestId) {
        return "org:" + orgId + ":" + kind + ":" + requestId;
    }

    /** Il primo conto del Character: se non esiste non lo crea (lo apre la banca al primo uso). */
    private Optional<Account> personalAccount(CharacterId character) {
        return accounts.accountsOf(OwnerRef.character(character)).stream().findFirst();
    }

    private Optional<Currency> currency() {
        return currencies.all().stream().findFirst();
    }

    // ---- comuni ------------------------------------------------------------------------------------------

    /** Il Character deve avere {@code boss.menu} e il nodo dell'azione. */
    private BossResult gate(CharacterId viewer, String orgId, String node) {
        Optional<Organization> org = orgs.find(orgId);
        if (org.isEmpty() || org.get().status() != OrgStatus.ACTIVE) {
            // Non rivela l'esistenza di aziende altrui: l'errore specifico solo a chi ne è membro.
            boolean member = org.isPresent() && orgs.membersOf(orgId).stream().anyMatch(m -> m.character().equals(viewer));
            return member ? BossResult.NO_ORG : BossResult.NO_PERMISSION;
        }
        if (!orgs.can(viewer, orgId, BossPermissions.MENU) || !orgs.can(viewer, orgId, node)) {
            return BossResult.NO_PERMISSION;
        }
        return BossResult.OK;
    }

    private static Optional<Rank> rankOf(CharacterId character, List<Membership> members, List<Rank> ranks) {
        return members.stream().filter(m -> m.character().equals(character)).findFirst()
                .flatMap(m -> ranks.stream().filter(r -> r.id().equals(m.rankId())).findFirst());
    }

    private String nameOf(CharacterId id) {
        return characters.find(id).map(BossService::fullName).orElse("?");
    }

    private static String fullName(PlayerCharacter pc) {
        return pc.firstName() + " " + pc.lastName();
    }

    private static BossResult map(OrgResult<?> result) {
        return result instanceof OrgResult.Failed<?> f ? map(f.error()) : BossResult.OK;
    }

    private static BossResult map(OrgError error) {
        return switch (error) {
            case OrgError.UnknownOrg e -> BossResult.NO_ORG;
            case OrgError.Archived e -> BossResult.NO_ORG;
            case OrgError.UnknownRank e -> BossResult.UNKNOWN_RANK;
            case OrgError.AlreadyMember e -> BossResult.ALREADY_MEMBER;
            case OrgError.NotMember e -> BossResult.NOT_MEMBER;
            case OrgError.UnknownCharacter e -> BossResult.NO_TARGET;
            case OrgError.NotUsable e -> BossResult.NO_TARGET;
            default -> BossResult.ERROR;
        };
    }

    private static BossResult map(AssignResult result) {
        return switch (result) {
            case AssignResult.Assigned a -> BossResult.OK;
            case AssignResult.UnknownCharacter u -> BossResult.NO_TARGET;
            case AssignResult.NotUsable n -> BossResult.NO_TARGET;
            case AssignResult.UnknownGrade g -> BossResult.UNKNOWN_RANK;
            case AssignResult.NoAssignment n -> BossResult.NOT_MEMBER;
            case AssignResult.UnknownJob j -> BossResult.NO_ORG;
            case AssignResult.Cancelled c -> BossResult.ERROR;
        };
    }

    private static BossResult map(TransferResult result) {
        return switch (result) {
            case TransferResult.Success s -> BossResult.OK;
            case TransferResult.Duplicate d -> BossResult.OK;
            case TransferResult.InsufficientFunds f -> BossResult.INSUFFICIENT_FUNDS;
            case TransferResult.AccountFrozen f -> BossResult.FROZEN;
            case TransferResult.AccountNotFound f -> BossResult.NO_ACCOUNT;
            case TransferResult.UnknownCurrency c -> BossResult.NO_CURRENCY;
            case TransferResult.InvalidRequest r -> BossResult.INVALID_AMOUNT;
            case TransferResult.Cancelled c -> BossResult.ERROR;
        };
    }
}
