package dev.qrm.bank.server;

import dev.qrm.api.Money;
import dev.qrm.api.QRM;
import dev.qrm.api.character.CharacterService;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.economy.CurrencyRegistry;
import dev.qrm.api.id.CharacterId;
import dev.qrm.api.id.PlayerId;
import dev.qrm.api.permission.PermissionService;
import dev.qrm.bank.logic.ActionLimiter;
import dev.qrm.bank.logic.BankOperations;
import dev.qrm.bank.logic.BankPermissions;
import dev.qrm.bank.logic.BankResult;
import dev.qrm.bank.logic.HistoryLine;
import dev.qrm.bank.logic.TransferNote;
import dev.qrm.bank.mc.BankConfig;
import dev.qrm.bank.mc.BankItems;
import dev.qrm.bank.mc.BankMod;
import dev.qrm.bank.mc.PlayerCash;
import dev.qrm.bank.net.AtmStatePayload;
import dev.qrm.bank.net.DepositPayload;
import dev.qrm.bank.net.HistoryRequestPayload;
import dev.qrm.bank.net.TransferPayload;
import dev.qrm.bank.net.WithdrawPayload;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tutto ciò che il server fa per l'ATM. Ogni richiesta del client passa dagli stessi controlli, nell'ordine: rate
 * limit, personaggio attivo, sessione ATM e distanza, permessi, formato dei campi; poi la logica di
 * {@link BankOperations}. Il client non indica mai il personaggio.
 */
public final class AtmActions {
    private static final Logger LOG = LoggerFactory.getLogger("qrm_bank");
    private static final Pattern CURRENCY = Pattern.compile("[A-Z0-9_]{2,16}");

    private record Session(ResourceKey<Level> dimension, BlockPos pos) {}

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();
    private static final ActionLimiter LIMITER = new ActionLimiter(Clock.systemUTC());

    private AtmActions() {}

    /** Click destro sul blocco. */
    public static void open(ServerPlayer player, BlockPos pos) {
        Optional<PlayerCharacter> character = activeCharacter(player);
        if (character.isEmpty()) {
            player.sendSystemMessage(Component.translatable("qrm_bank.msg.no_character"));
            return;
        }
        if (!BankPermissions.allowed(QRM.get(PermissionService.class), character.get().id(), BankPermissions.ATM_USE)) {
            player.sendSystemMessage(Component.translatable("qrm_bank.msg.denied"));
            return;
        }
        if (!LIMITER.tryAcquire(player.getUUID(), BankConfig.MIN_INTERVAL_MS.get())) return;
        SESSIONS.put(player.getUUID(), new Session(player.level().dimension(), pos.immutable()));
        sendState(player, character.get().id(), true, BankResult.OK);
    }

    public static void onWithdraw(WithdrawPayload payload, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        run(player, false, id -> {
            if (!validCurrency(payload.currency()) || payload.amount() <= 0) return BankResult.INVALID_AMOUNT;
            return BankMod.operations().withdraw(id, Money.of(payload.currency(), payload.amount()), new PlayerCash(player));
        });
    }

    public static void onDeposit(DepositPayload payload, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        run(player, false, id -> {
            if (!validCurrency(payload.currency())) return BankResult.INVALID_AMOUNT;
            return BankMod.operations().deposit(id, payload.currency(), new PlayerCash(player));
        });
    }

    public static void onTransfer(TransferPayload payload, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        run(player, true, id -> {
            if (!validCurrency(payload.currency()) || payload.amount() <= 0) return BankResult.INVALID_AMOUNT;
            return BankMod.operations().transfer(id, payload.recipient(),
                    Money.of(payload.currency(), payload.amount()), payload.note());
        });
    }

    public static void onHistory(HistoryRequestPayload payload, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        run(player, false, id -> BankResult.OK);
    }

    /**
     * Da chiamare a ogni tick del giocatore: se la sessione ATM non è più valida (blocco rotto, giocatore troppo
     * lontano o in un'altra dimensione) la chiude subito sul client, senza aspettare la prossima azione.
     */
    public static void tick(ServerPlayer player) {
        if (player.tickCount % 10 != 0) return;
        if (!SESSIONS.containsKey(player.getUUID()) || inRange(player)) return;
        SESSIONS.remove(player.getUUID());
        PacketDistributor.sendToPlayer(player, AtmStatePayload.empty(BankResult.TOO_FAR));
    }

    public static void forget(UUID player) {
        SESSIONS.remove(player);
        LIMITER.forget(player);
    }

    public static void reset() {
        SESSIONS.clear();
    }

    private static void run(ServerPlayer player, boolean transfer, Function<CharacterId, BankResult> action) {
        Optional<PlayerCharacter> character = activeCharacter(player);
        BankResult result = gate(player, character, transfer);
        if (result == BankResult.OK) {
            try {
                result = action.apply(character.get().id());
            } catch (RuntimeException e) {
                LOG.error("Bank operation failed for {}", player.getUUID(), e);
                result = BankResult.INTERNAL_ERROR;
            }
        } else if (result == BankResult.TOO_FAR) {
            SESSIONS.remove(player.getUUID());
        }
        if (character.isPresent()) {
            sendState(player, character.get().id(), false, result);
        } else {
            PacketDistributor.sendToPlayer(player, AtmStatePayload.empty(result));
        }
    }

    private static BankResult gate(ServerPlayer player, Optional<PlayerCharacter> character, boolean transfer) {
        if (!LIMITER.tryAcquire(player.getUUID(), BankConfig.MIN_INTERVAL_MS.get())) return BankResult.RATE_LIMITED;
        if (character.isEmpty()) return BankResult.NO_PERMISSION;
        if (!inRange(player)) return BankResult.TOO_FAR;
        PermissionService permissions = QRM.get(PermissionService.class);
        CharacterId id = character.get().id();
        if (!BankPermissions.allowed(permissions, id, BankPermissions.ATM_USE)) return BankResult.NO_PERMISSION;
        if (transfer && (!BankConfig.TRANSFERS_ENABLED.get()
                || !BankPermissions.allowed(permissions, id, BankPermissions.TRANSFER))) {
            return BankResult.NO_PERMISSION;
        }
        return BankResult.OK;
    }

    private static boolean inRange(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || !player.level().dimension().equals(session.dimension())) return false;
        if (!player.level().getBlockState(session.pos()).is(BankItems.ATM.get())) return false;
        double max = BankConfig.MAX_DISTANCE.get();
        return player.distanceToSqr(Vec3.atCenterOf(session.pos())) <= max * max;
    }

    private static boolean validCurrency(String code) {
        return code != null && CURRENCY.matcher(code).matches();
    }

    private static Optional<PlayerCharacter> activeCharacter(ServerPlayer player) {
        return QRM.get(CharacterService.class).activeOf(new PlayerId(player.getUUID()));
    }

    private static String cut(String text, int max) {
        return text.length() > max ? text.substring(0, max) : text;
    }

    private static void sendState(ServerPlayer player, CharacterId id, boolean open, BankResult result) {
        BankOperations ops = BankMod.operations();
        PlayerCash held = new PlayerCash(player);

        List<AtmStatePayload.CurrencyInfo> currencies = QRM.get(CurrencyRegistry.class).all().stream()
                .sorted(Comparator.comparing(c -> c.code()))
                .limit(AtmStatePayload.MAX_LIST)
                .map(c -> new AtmStatePayload.CurrencyInfo(c.code(), c.symbol(), c.decimals()))
                .toList();
        List<AtmStatePayload.BalanceEntry> balances = ops.balances(id).stream()
                .limit(AtmStatePayload.MAX_LIST)
                .map(m -> new AtmStatePayload.BalanceEntry(m.currency(), m.amount()))
                .toList();
        List<String> cashCurrencies = new ArrayList<>();
        List<AtmStatePayload.BalanceEntry> heldEntries = new ArrayList<>();
        for (String code : BankMod.denominations().currencies()) {
            if (cashCurrencies.size() >= AtmStatePayload.MAX_LIST) break;
            if (currencies.stream().noneMatch(c -> c.code().equals(code))) continue;
            cashCurrencies.add(code);
            heldEntries.add(new AtmStatePayload.BalanceEntry(code, held.count(code)));
        }
        List<AtmStatePayload.HistoryEntry> history = new ArrayList<>();
        for (HistoryLine line : ops.historyLines(id, Math.min(BankConfig.HISTORY_ROWS.get(), AtmStatePayload.MAX_HISTORY))) {
            history.add(new AtmStatePayload.HistoryEntry(line.epochSecond(), cut(line.reason(), AtmStatePayload.MAX_TEXT),
                    line.currency(), line.signedAmount(), cut(line.counterparty(), 129),
                    cut(line.note(), TransferNote.MAX_LENGTH)));
        }
        PacketDistributor.sendToPlayer(player,
                new AtmStatePayload(open, result, currencies, balances, heldEntries, history, cashCurrencies));
    }
}
