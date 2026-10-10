package dev.qrm.neoforge;

import dev.qrm.api.QRM;
import dev.qrm.api.economy.CurrencyRegistry;
import dev.qrm.api.event.EventBus;
import dev.qrm.api.job.JobDefinition;
import dev.qrm.api.job.JobRegistry;
import dev.qrm.api.job.JobService;
import dev.qrm.api.point.PointTypeRegistry;
import dev.qrm.core.QrmCore;
import dev.qrm.core.economy.InMemoryCurrencyRegistry;
import dev.qrm.core.event.SimpleEventBus;
import dev.qrm.core.job.InMemoryJobRegistry;
import dev.qrm.core.point.InMemoryPointTypeRegistry;
import dev.qrm.neoforge.net.QrmNetwork;
import dev.qrm.neoforge.server.DutyPointHandler;
import dev.qrm.neoforge.server.PointEvents;
import dev.qrm.neoforge.server.PointSync;
import dev.qrm.neoforge.server.StatusSync;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(QrmMod.MOD_ID)
public final class QrmMod {
    public static final String MOD_ID = "qrm";
    private static final Logger LOG = LoggerFactory.getLogger("qrm");

    private static final SimpleEventBus BUS = new SimpleEventBus();
    private static final InMemoryCurrencyRegistry CURRENCIES = new InMemoryCurrencyRegistry();
    private static final InMemoryJobRegistry JOBS = new InMemoryJobRegistry();
    private static final InMemoryPointTypeRegistry POINT_TYPES = new InMemoryPointTypeRegistry();
    private static QrmCore core;
    /** Valute caricate da file per il mondo corrente: rimosse a fine mondo per non passare al successivo. */
    private static Set<String> loadedCurrencyCodes = Set.of();
    /** Lavori caricati da file per il mondo corrente: rimossi a fine mondo. */
    private static Set<String> loadedJobIds = Set.of();

    public QrmMod(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, QrmConfig.SPEC);
        // Registrati subito: i mod dipendenti possono agganciare handler, valute e lavori nel loro costruttore.
        QRM.register(EventBus.class, BUS);
        QRM.register(CurrencyRegistry.class, CURRENCIES);
        QRM.register(JobRegistry.class, JOBS);
        QRM.register(PointTypeRegistry.class, POINT_TYPES);
        // il tipo di punto di QRM usa la stessa API pubblica dei mod di terzi
        POINT_TYPES.register(DutyPointHandler.type(() -> QRM.get(JobService.class), QrmMod::tell));
        PlayerEvents.register(BUS);
        StatusSync.register(BUS);
        PointSync.register(BUS);
        PointEvents.register();
        NeoForge.EVENT_BUS.addListener(QrmMod::onServerStarting);
        NeoForge.EVENT_BUS.addListener(QrmMod::onServerStopped);
        NeoForge.EVENT_BUS.addListener(QrmCommands::onRegisterCommands);
        modBus.addListener(QrmNetwork::register);
    }

    private static void onServerStarting(ServerStartingEvent event) {
        Path dir = event.getServer().getWorldPath(LevelResource.ROOT).resolve("qrm");
        try {
            Files.createDirectories(dir);
            Set<String> currenciesBefore = codes();
            int currencies = CurrencyLoader.load(CURRENCIES, dir.resolve("data").resolve("currencies"));
            Set<String> addedCurrencies = codes();
            addedCurrencies.removeAll(currenciesBefore);
            loadedCurrencyCodes = addedCurrencies;

            Set<String> jobsBefore = jobIds();
            int jobs = JobLoader.load(JOBS, CURRENCIES, dir.resolve("data").resolve("jobs"));
            Set<String> addedJobs = jobIds();
            addedJobs.removeAll(jobsBefore);
            loadedJobIds = addedJobs;

            String configured = QrmConfig.DB_URL.get();
            String url = configured.isBlank() ? "jdbc:sqlite:" + dir.resolve("qrm.db").toAbsolutePath() : configured;
            core = QrmCore.start(BUS, CURRENCIES, JOBS, POINT_TYPES, url, QrmConfig.DB_USER.get(), QrmConfig.DB_PASSWORD.get(),
                    QrmConfig.toCoreConfig(), Clock.systemUTC());
            LOG.info("QRM started ({} currencies, {} jobs)", currencies, jobs);
        } catch (IOException | RuntimeException e) {
            // QRM non parte con dati parziali: fa fallire l'avvio del server con un messaggio chiaro.
            throw new IllegalStateException("QRM could not start: " + e.getMessage(), e);
        }
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        if (core != null) {
            core.close();
            core = null;
        }
        loadedCurrencyCodes.forEach(CURRENCIES::remove);
        loadedCurrencyCodes = Set.of();
        loadedJobIds.forEach(JOBS::remove);
        loadedJobIds = Set.of();
    }

    private static void tell(dev.qrm.api.id.PlayerId id, String text) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        ServerPlayer player = server.getPlayerList().getPlayer(id.value());
        if (player != null) player.sendSystemMessage(Component.literal(text));
    }

    private static Set<String> codes() {
        Set<String> out = new HashSet<>();
        CURRENCIES.all().forEach(c -> out.add(c.code()));
        return out;
    }

    private static Set<String> jobIds() {
        Set<String> out = new HashSet<>();
        JOBS.all().stream().map(JobDefinition::id).forEach(out::add);
        return out;
    }
}
