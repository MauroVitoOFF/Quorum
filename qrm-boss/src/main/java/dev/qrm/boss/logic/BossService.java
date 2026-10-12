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
 */
public final class BossService {
    static final String DEPOSIT_REASON = "Deposito aziendale";

    private final OrganizationService orgs;
    private final OrganizationBankService bank;
    private final AccountService accounts;
    private final TransactionService txs;
    private final CharacterService characters;
    private final CurrencyRegistry currencies;

    public BossService(OrganizationService orgs, OrganizationBankService bank, AccountService accounts,
            TransactionService txs, CharacterService characters, CurrencyRegistry currencies) {
        this.orgs = orgs;
        this.bank = bank;
        this.accounts = accounts;
        this.txs = txs;
        this.characters = characters;
        this.currencies = currencies;
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

        List<Rank> ranks = orgs.ranksOf(orgId);
        List<Membership> members = orgs.membersOf(orgId);
        Optional<Rank> mine = rankOf(viewer, members, ranks);
        int level = mine.map(Rank::level).orElse(-1);

        Optional<Currency> currency = currency();
        boolean canView = orgs.can(viewer, orgId, BossPermissions.BANK_VIEW);
        boolean hasBalance = sections.account() && canView && currency.isPresent();
        String balance = hasBalance ? currency.get().format(
                accounts.balance(org.account(), currency.get().code()).amount()) : "";

        List<BossSnapshot.Member> memberViews = new ArrayList<>();
        List<BossSnapshot.RankOption> options = new ArrayList<>();
        List<BossSnapshot.Candidate> candidates = new ArrayList<>();
        if (sections.members()) {
            for (Membership m : members) {
                Optional<Rank> rank = ranks.stream().filter(r -> r.id().equals(m.rankId())).findFirst();
                int memberLevel = rank.map(Rank::level).orElse(Integer.MAX_VALUE);
                boolean editable = level >= 0 && !m.character().equals(viewer) && memberLevel <= level;
                memberViews.add(new BossSnapshot.Member(m.character().value(), nameOf(m.character()), m.rankId(),
                        rank.map(Rank::label).orElse(m.rankId()), rank.isPresent() ? memberLevel : -1, editable));
            }
            memberViews.sort(Comparator.comparingInt(BossSnapshot.Member::level).reversed()
                    .thenComparing(BossSnapshot.Member::name));
            if (level >= 0) {
                for (Rank r : ranks) {
                    if (r.level() <= level) options.add(new BossSnapshot.RankOption(r.id(), r.label(), r.level()));
                }
                Set<CharacterId> memberIds = new HashSet<>();
                members.forEach(m -> memberIds.add(m.character()));
                for (PlayerCharacter pc : online) {
                    if (pc.status() == CharacterStatus.ACTIVE && !memberIds.contains(pc.id())) {
                        candidates.add(new BossSnapshot.Candidate(pc.id().value(), fullName(pc)));
                    }
                }
                candidates.sort(Comparator.comparing(BossSnapshot.Candidate::name));
            }
        }
        return Optional.of(new BossSnapshot(org.id(), org.label(), org.status().name(), viewer.value(),
                mine.map(Rank::label).orElse(""), level, sections.members(), sections.account(), members.size(),
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

    /** Aggiunge un Character con il rank assegnabile di livello più basso. */
    public BossResult addMember(CharacterId viewer, String orgId, CharacterId target) {
        BossResult gate = gate(viewer, orgId, BossPermissions.MEMBERS_MANAGE);
        if (gate != BossResult.OK) return gate;
        List<Rank> ranks = orgs.ranksOf(orgId);
        Optional<Rank> mine = rankOf(viewer, orgs.membersOf(orgId), ranks);
        if (mine.isEmpty()) return BossResult.READ_ONLY;
        if (target.equals(viewer)) return BossResult.ALREADY_MEMBER;
        Optional<Rank> lowest = ranks.stream().filter(r -> r.level() <= mine.get().level())
                .min(Comparator.comparingInt(Rank::level));
        if (lowest.isEmpty()) return BossResult.UNKNOWN_RANK;
        return map(orgs.addMember(orgId, target, lowest.get().id()));
    }

    public BossResult setRank(CharacterId viewer, String orgId, CharacterId target, String rankId) {
        BossResult gate = gate(viewer, orgId, BossPermissions.MEMBERS_MANAGE);
        if (gate != BossResult.OK) return gate;
        List<Rank> ranks = orgs.ranksOf(orgId);
        List<Membership> members = orgs.membersOf(orgId);
        BossResult target0 = checkTarget(viewer, target, ranks, members);
        if (target0 != BossResult.OK) return target0;
        Optional<Rank> mine = rankOf(viewer, members, ranks);
        Optional<Rank> wanted = ranks.stream().filter(r -> r.id().equals(rankId)).findFirst();
        if (wanted.isEmpty()) return BossResult.UNKNOWN_RANK;
        if (wanted.get().level() > mine.get().level()) return BossResult.LEVEL_TOO_HIGH;
        return map(orgs.setRank(orgId, target, rankId));
    }

    public BossResult removeMember(CharacterId viewer, String orgId, CharacterId target) {
        BossResult gate = gate(viewer, orgId, BossPermissions.MEMBERS_MANAGE);
        if (gate != BossResult.OK) return gate;
        BossResult target0 = checkTarget(viewer, target, orgs.ranksOf(orgId), orgs.membersOf(orgId));
        if (target0 != BossResult.OK) return target0;
        return map(orgs.removeMember(orgId, target));
    }

    /** Vale per ogni azione su un membro: chi agisce ha un rank esplicito, il bersaglio è un altro membro di livello <=. */
    private BossResult checkTarget(CharacterId viewer, CharacterId target, List<Rank> ranks, List<Membership> members) {
        Optional<Rank> mine = rankOf(viewer, members, ranks);
        if (mine.isEmpty()) return BossResult.READ_ONLY;
        if (target.equals(viewer)) return BossResult.SELF;
        Optional<Membership> membership = members.stream().filter(m -> m.character().equals(target)).findFirst();
        if (membership.isEmpty()) return BossResult.NOT_MEMBER;
        Optional<Rank> theirs = ranks.stream().filter(r -> r.id().equals(membership.get().rankId())).findFirst();
        if (theirs.isEmpty() || theirs.get().level() > mine.get().level()) return BossResult.LEVEL_TOO_HIGH;
        return BossResult.OK;
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
