package dev.qrm.neoforge.server;

import dev.qrm.api.Money;
import dev.qrm.api.character.CharacterService;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.economy.Account;
import dev.qrm.api.economy.AccountService;
import dev.qrm.api.economy.Currency;
import dev.qrm.api.economy.CurrencyRegistry;
import dev.qrm.api.economy.OwnerRef;
import dev.qrm.api.id.CharacterId;
import dev.qrm.api.id.PlayerId;
import dev.qrm.api.job.Assignment;
import dev.qrm.api.job.GradeDefinition;
import dev.qrm.api.job.JobDefinition;
import dev.qrm.api.job.JobRegistry;
import dev.qrm.api.job.JobService;
import dev.qrm.api.org.Membership;
import dev.qrm.api.org.Organization;
import dev.qrm.api.org.OrganizationService;
import dev.qrm.api.org.Rank;
import dev.qrm.neoforge.net.PlayerStatus;
import dev.qrm.neoforge.net.UiConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Costruisce lo snapshot {@link PlayerStatus} dai servizi del Core. Nessuna dipendenza da Minecraft. */
public final class PlayerStatusBuilder {
    private static final Logger LOG = LoggerFactory.getLogger("qrm");
    private static final AtomicBoolean WARNED_PRIMARY = new AtomicBoolean();

    private final CharacterService characters;
    private final AccountService accounts;
    private final CurrencyRegistry currencies;
    private final JobService jobs;
    private final JobRegistry jobRegistry;
    private final OrganizationService orgs;
    private final Supplier<String> configuredPrimary;
    private final Supplier<UiConfig> ui;

    public PlayerStatusBuilder(CharacterService characters, AccountService accounts, CurrencyRegistry currencies,
            JobService jobs, JobRegistry jobRegistry, OrganizationService orgs, Supplier<String> configuredPrimary,
            Supplier<UiConfig> ui) {
        this.ui = ui;
        this.characters = characters;
        this.accounts = accounts;
        this.currencies = currencies;
        this.jobs = jobs;
        this.jobRegistry = jobRegistry;
        this.orgs = orgs;
        this.configuredPrimary = configuredPrimary;
    }

    public Optional<CharacterId> activeCharacter(PlayerId player) {
        return characters.activeOf(player).map(PlayerCharacter::id);
    }

    public PlayerStatus build(PlayerId player) {
        Optional<PlayerCharacter> active = characters.activeOf(player);
        if (active.isEmpty()) return PlayerStatus.EMPTY;
        PlayerCharacter pc = active.get();
        Optional<Currency> primary = resolvePrimary();
        String primaryCode = primary.map(Currency::code).orElse("");

        List<PlayerStatus.BalanceEntry> balances = balances(pc.id(), primaryCode);
        List<PlayerStatus.CurrencyInfo> infos = new ArrayList<>();
        for (PlayerStatus.BalanceEntry b : balances) {
            currencies.find(b.currency()).ifPresent(c -> infos.add(new PlayerStatus.CurrencyInfo(c.code(),
                    cut(c.symbol(), PlayerStatus.MAX_SYMBOL), c.decimals())));
        }
        String name = (pc.firstName().strip() + " " + pc.lastName().strip()).strip();
        return new PlayerStatus(cut(name, PlayerStatus.MAX_FULL_NAME), primaryCode, infos, balances,
                jobs.assignmentOf(pc.id()).map(this::jobInfo), orgEntries(pc.id()), ui.get());
    }

    private Optional<Currency> resolvePrimary() {
        String code = configuredPrimary.get();
        Optional<Currency> found = code == null ? Optional.empty() : currencies.find(code.toUpperCase(Locale.ROOT));
        if (found.isPresent()) return found;
        Optional<Currency> first = currencies.all().stream().min(Comparator.comparing(Currency::code));
        if (first.isPresent() && WARNED_PRIMARY.compareAndSet(false, true)) {
            LOG.warn("QRM: economy.defaultCurrency '{}' is not a registered currency; showing {} as the primary one",
                    code, first.get().code());
        }
        return first;
    }

    /** Valuta principale per prima (anche a 0), poi gli altri saldi positivi per codice; al massimo 16. */
    private List<PlayerStatus.BalanceEntry> balances(CharacterId id, String primaryCode) {
        Optional<Account> account = accounts.accountsOf(OwnerRef.character(id)).stream().findFirst();
        List<Money> held = account.map(a -> accounts.balances(a.id())).orElse(List.of());
        List<PlayerStatus.BalanceEntry> out = new ArrayList<>();
        if (!primaryCode.isEmpty()) {
            long amount = held.stream().filter(m -> m.currency().equals(primaryCode)).mapToLong(Money::amount)
                    .findFirst().orElse(0L);
            out.add(new PlayerStatus.BalanceEntry(primaryCode, amount));
        }
        held.stream().filter(m -> !m.currency().equals(primaryCode)).sorted(Comparator.comparing(Money::currency))
                .forEach(m -> out.add(new PlayerStatus.BalanceEntry(cut(m.currency(), PlayerStatus.MAX_TEXT), m.amount())));
        return out.size() > PlayerStatus.MAX_BALANCES ? out.subList(0, PlayerStatus.MAX_BALANCES) : out;
    }

    private PlayerStatus.JobInfo jobInfo(Assignment a) {
        Optional<JobDefinition> def = jobRegistry.find(a.jobId());
        String jobLabel = def.map(JobDefinition::label).orElse(a.jobId());
        String gradeLabel = def.flatMap(d -> d.grades().stream().filter(g -> g.id().equals(a.gradeId())).findFirst())
                .map(GradeDefinition::label).orElse(a.gradeId());
        long since = a.since() == null ? 0L : a.since().getEpochSecond();
        return new PlayerStatus.JobInfo(cut(jobLabel, PlayerStatus.MAX_TEXT), cut(gradeLabel, PlayerStatus.MAX_TEXT),
                a.onDuty(), since, a.known());
    }

    private List<PlayerStatus.OrgEntry> orgEntries(CharacterId id) {
        List<PlayerStatus.OrgEntry> out = new ArrayList<>();
        for (Organization org : orgs.organizationsOf(id)) {
            if (out.size() >= PlayerStatus.MAX_ORGS) break;
            Optional<Membership> membership = orgs.membersOf(org.id()).stream()
                    .filter(m -> m.character().equals(id)).findFirst();
            String rank = membership.map(m -> rankLabel(org.id(), m.rankId())).orElse("");
            out.add(new PlayerStatus.OrgEntry(cut(org.label(), PlayerStatus.MAX_TEXT),
                    cut(rank, PlayerStatus.MAX_TEXT), membership.isEmpty()));
        }
        return out;
    }

    private String rankLabel(String orgId, String rankId) {
        return orgs.ranksOf(orgId).stream().filter(r -> r.id().equals(rankId)).findFirst().map(Rank::label)
                .orElse(rankId);
    }

    private static String cut(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
