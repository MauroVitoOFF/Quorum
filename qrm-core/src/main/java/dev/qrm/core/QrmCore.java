package dev.qrm.core;

import dev.qrm.api.QRM;
import dev.qrm.api.character.CharacterCreatedEvent;
import dev.qrm.api.character.CharacterSelectedEvent;
import dev.qrm.api.character.CharacterService;
import dev.qrm.api.data.ModDataService;
import dev.qrm.api.economy.AccountService;
import dev.qrm.api.economy.AuditService;
import dev.qrm.api.economy.CurrencyRegistry;
import dev.qrm.api.economy.OwnerRef;
import dev.qrm.api.economy.TransactionService;
import dev.qrm.api.event.EventBus;
import dev.qrm.api.event.QrmReadyEvent;
import dev.qrm.api.event.Registration;
import dev.qrm.api.job.DutyReason;
import dev.qrm.api.job.JobRegistry;
import dev.qrm.api.job.JobService;
import dev.qrm.api.moderation.ModerationService;
import dev.qrm.api.org.OrganizationBankService;
import dev.qrm.api.org.OrganizationService;
import dev.qrm.api.permission.PermissionService;
import dev.qrm.api.point.PointService;
import dev.qrm.api.point.PointTypeRegistry;
import dev.qrm.api.player.PlayerService;
import dev.qrm.api.staff.StaffAccessService;
import dev.qrm.api.staff.StaffLogService;
import dev.qrm.core.character.DefaultCharacterService;
import dev.qrm.core.data.DefaultModDataService;
import dev.qrm.core.db.Database;
import dev.qrm.core.db.Migrator;
import dev.qrm.core.economy.DefaultAccountService;
import dev.qrm.core.economy.DefaultAuditService;
import dev.qrm.core.economy.DefaultTransactionService;
import dev.qrm.core.job.DefaultJobService;
import dev.qrm.core.moderation.DefaultModerationService;
import dev.qrm.core.job.DefaultPermissionService;
import dev.qrm.core.org.DefaultOrganizationBankService;
import dev.qrm.core.org.DefaultOrganizationService;
import dev.qrm.core.org.JobOrgSync;
import dev.qrm.core.player.DefaultPlayerService;
import dev.qrm.core.point.DefaultPointService;
import dev.qrm.core.staff.DefaultStaffAccessService;
import dev.qrm.core.staff.DefaultStaffLogService;
import java.time.Clock;
import java.util.List;

/** Avvio del Core: apre il DB, migra, registra i servizi in {@link QRM} e pubblica {@link QrmReadyEvent}. */
public final class QrmCore implements AutoCloseable {
    private static final List<Class<?>> SERVICE_TYPES = List.of(
            PlayerService.class, CharacterService.class, AccountService.class,
            TransactionService.class, AuditService.class, ModDataService.class,
            JobService.class, PermissionService.class, OrganizationService.class, OrganizationBankService.class,
            StaffAccessService.class, StaffLogService.class, ModerationService.class, PointService.class);

    private final Database db;
    private final Registration characterHandler;
    private final Registration selectHandler;

    private QrmCore(Database db, Registration characterHandler, Registration selectHandler) {
        this.db = db;
        this.characterHandler = characterHandler;
        this.selectHandler = selectHandler;
    }

    /**
     * @param bus, {@code currencies}, {@code jobs} e {@code pointTypes} devono essere già registrati in {@link QRM} dall'adattatore, così i mod
     *             possono usarli prima di QrmReadyEvent.
     */
    public static QrmCore start(EventBus bus, CurrencyRegistry currencies, JobRegistry jobs,
            PointTypeRegistry pointTypes, String jdbcUrl,
            String user, String password, CoreConfig config, Clock clock) {
        Database db = Database.open(jdbcUrl, user, password);
        Registration characterHandler = null;
        Registration selectHandler = null;
        try {
            Migrator.migrate(db);
            // un crash non deve lasciare personaggi in servizio: nessun giocatore e' online all'avvio
            db.tx(c -> {
                try (java.sql.Statement st = c.createStatement()) {
                    st.executeUpdate("UPDATE qrm_character_job SET on_duty=0");
                }
                return null;
            });
            DefaultAccountService accounts = new DefaultAccountService(db, bus, clock);
            DefaultJobService jobService = new DefaultJobService(db, bus, jobs, clock);
            DefaultTransactionService transactions = new DefaultTransactionService(db, bus, clock, currencies);
            DefaultPermissionService permissionService = new DefaultPermissionService(db, bus, jobs, jobService);
            DefaultOrganizationService organizations = new DefaultOrganizationService(db, bus, accounts, jobs, jobService,
                    permissionService, clock);
            QRM.register(PointService.class, new DefaultPointService(db, bus, pointTypes, jobs, jobService, organizations, clock));
            QRM.register(PlayerService.class, new DefaultPlayerService(db, clock, config));
            QRM.register(CharacterService.class, new DefaultCharacterService(db, bus, clock, config));
            QRM.register(AccountService.class, accounts);
            QRM.register(TransactionService.class, transactions);
            QRM.register(AuditService.class, new DefaultAuditService(db));
            QRM.register(ModDataService.class, new DefaultModDataService(db));
            QRM.register(JobService.class, jobService);
            QRM.register(PermissionService.class, permissionService);
            QRM.register(OrganizationService.class, organizations);
            QRM.register(OrganizationBankService.class, new DefaultOrganizationBankService(organizations, transactions));
            QRM.register(StaffAccessService.class, new DefaultStaffAccessService(db));
            QRM.register(StaffLogService.class, new DefaultStaffLogService(db, clock));
            QRM.register(ModerationService.class, new DefaultModerationService(db, bus, clock));

            characterHandler = bus.register(CharacterCreatedEvent.class, dev.qrm.api.event.Priority.EARLIEST, false, "qrm-core", e -> {
                OwnerRef owner = OwnerRef.character(e.character().id());
                if (accounts.accountsOf(owner).isEmpty()) accounts.create(owner, false);
            });
            selectHandler = bus.register(CharacterSelectedEvent.class, dev.qrm.api.event.Priority.EARLIEST, false, "qrm-core",
                    e -> jobService.setOthersOffDuty(e.character().playerId(), e.character().id(), DutyReason.SWITCH));

            accounts.systemAccount();
            JobOrgSync.sync(db, organizations, jobs);
            bus.post(new QrmReadyEvent());
            return new QrmCore(db, characterHandler, selectHandler);
        } catch (Throwable t) {
            if (characterHandler != null) characterHandler.unregister();
            if (selectHandler != null) selectHandler.unregister();
            SERVICE_TYPES.forEach(QRM::unregister);
            try {
                db.close();
            } catch (RuntimeException closeFailure) {
                t.addSuppressed(closeFailure);
            }
            throw t;
        }
    }

    @Override
    public void close() {
        characterHandler.unregister();
        selectHandler.unregister();
        SERVICE_TYPES.forEach(QRM::unregister);
        db.close();
    }
}
