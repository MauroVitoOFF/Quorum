package dev.qrm.bank.mc;

import dev.qrm.api.QRM;
import dev.qrm.api.character.CharacterService;
import dev.qrm.api.economy.AccountService;
import dev.qrm.api.economy.AuditService;
import dev.qrm.api.economy.Currency;
import dev.qrm.api.economy.CurrencyRegistry;
import dev.qrm.api.economy.TransactionService;
import dev.qrm.api.org.OrganizationService;
import dev.qrm.bank.logic.BankOperations;
import dev.qrm.bank.logic.CashDenominations;
import dev.qrm.bank.logic.StaffBankLogic;
import dev.qrm.bank.net.BankNetwork;
import dev.qrm.bank.server.AtmActions;
import dev.qrm.bank.api.BankTransferEvent;
import dev.qrm.bank.server.BankCommands;
import dev.qrm.bank.server.TransferNotifier;
import java.time.Clock;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(BankMod.MOD_ID)
public final class BankMod {
    public static final String MOD_ID = "qrm_bank";
    private static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    private static CashDenominations denominations;
    private static BankOperations operations;

    public BankMod(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, BankConfig.SPEC);
        // I tagli si leggono ora (non da un datapack): gli item si registrano prima del caricamento dei dati.
        denominations = CashConfigFile.load(FMLPaths.CONFIGDIR.get().resolve(MOD_ID));
        BankItems.registerCash(denominations);
        BankItems.ITEMS.register(modBus);
        BankItems.BLOCKS.register(modBus);
        modBus.addListener(BankNetwork::register);
        modBus.addListener(BankItems::onModifyDefaultComponents);
        modBus.addListener(BankMod::onBuildCreativeTab);
        NeoForge.EVENT_BUS.addListener(BankCommands::onRegisterCommands);
        QRM.events().register(BankTransferEvent.class, MOD_ID, TransferNotifier::onTransfer);
        NeoForge.EVENT_BUS.addListener(BankMod::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(BankMod::onServerStopped);
        NeoForge.EVENT_BUS.addListener(BankMod::onPlayerLoggedOut);
    }

    private static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) event.accept(BankItems.ATM_ITEM.get());
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) AtmActions.tick(player);
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        reset();
        AtmActions.reset();
    }

    private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AtmActions.forget(player.getUUID());
            dev.qrm.bank.server.StaffBankServer.forget(player.getUUID());
        }
    }

    public static CashDenominations denominations() {
        return denominations;
    }

    /** La logica della banca del mondo corrente (creata al primo uso, scartata a fine mondo con {@link #reset()}). */
    public static synchronized BankOperations operations() {
        if (operations == null) {
            warnAboutCurrencies();
            operations = new BankOperations(
                    QRM.get(AccountService.class), QRM.get(TransactionService.class),
                    QRM.get(CharacterService.class), QRM.get(OrganizationService.class), QRM.events(),
                    Clock.systemUTC(), denominations, BankConfig.limits());
        }
        return operations;
    }

    /** Logica amministrativa condivisa fra i comandi {@code /bank} e il menu staff. */
    public static StaffBankLogic staffLogic() {
        return new StaffBankLogic(operations(), QRM.get(AccountService.class), QRM.get(TransactionService.class),
                QRM.get(AuditService.class), QRM.get(CurrencyRegistry.class));
    }

    public static synchronized void reset() {
        operations = null;
    }

    private static void warnAboutCurrencies() {
        CurrencyRegistry registry = QRM.get(CurrencyRegistry.class);
        for (String code : denominations.currencies()) {
            Currency c = registry.find(code).orElse(null);
            if (c == null) {
                LOG.warn("cash.json: la valuta {} non e' registrata in QRM (il contante non e' utilizzabile)", code);
            } else if (c.decimals() != denominations.decimals(code)) {
                LOG.warn("cash.json: {} ha {} decimali ma la valuta QRM ne ha {}", code, denominations.decimals(code),
                        c.decimals());
            }
        }
    }
}
