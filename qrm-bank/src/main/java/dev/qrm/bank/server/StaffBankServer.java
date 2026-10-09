package dev.qrm.bank.server;

import dev.qrm.api.QRM;
import dev.qrm.api.character.CharacterService;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.id.CharacterId;
import dev.qrm.api.id.PlayerId;
import dev.qrm.api.staff.StaffAccessService;
import dev.qrm.api.staff.StaffNodes;
import dev.qrm.bank.logic.ActionLimiter;
import dev.qrm.bank.logic.StaffBankLogic;
import dev.qrm.bank.mc.BankItems;
import dev.qrm.bank.mc.BankMod;
import dev.qrm.bank.mc.PlayerCash;
import dev.qrm.bank.net.StaffBankAction;
import dev.qrm.bank.net.StaffBankActionPayload;
import dev.qrm.bank.net.StaffBankResult;
import dev.qrm.bank.net.StaffBankStatePayload;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lato server della schermata staff della banca (aperta dalla voce del pannello staff di QRM). Il permesso (operatore o
 * nodo {@code staff.bank}) è ricontrollato a ogni pacchetto; le azioni usano {@link StaffBankLogic}.
 */
public final class StaffBankServer {
    private static final Logger LOG = LoggerFactory.getLogger("qrm_bank");
    private static final long MIN_INTERVAL_MS = 250;
    private static final ActionLimiter LIMITER = new ActionLimiter(Clock.systemUTC());

    private StaffBankServer() {}

    /** Operatore (GAMEMASTER) oppure account con il nodo {@code staff.bank}. */
    private static boolean isStaff(ServerPlayer player) {
        return player.createCommandSourceStack().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)
                || QRM.get(StaffAccessService.class).has(player.getUUID(), StaffNodes.BANK);
    }

    /** Azioni che cambiano qualcosa (le altre sono letture e non vanno nel registro staff). */
    private static boolean changesState(StaffBankAction action) {
        return action == StaffBankAction.GRANT || action == StaffBankAction.TAKE || action == StaffBankAction.FREEZE
                || action == StaffBankAction.UNFREEZE || action == StaffBankAction.GIVE_ATM;
    }

    public static void forget(UUID player) {
        LIMITER.forget(player);
    }

    /** Valute selezionabili: tutte quelle registrate; la prima proposta è quella del contante, se c'è. */
    private static String primaryCurrency(List<String> codes) {
        for (String c : BankMod.denominations().currencies()) if (codes.contains(c)) return c;
        return codes.isEmpty() ? "" : codes.get(0);
    }

    private static void reply(ServerPlayer player, StaffBankAction action, StaffBankResult result,
            Optional<StaffBankLogic.State> state, int auditIssues, List<StaffBankLogic.Excess> excess) {
        List<String> codes = BankMod.staffLogic().currencyCodes();
        PacketDistributor.sendToPlayer(player, new StaffBankStatePayload(action, result, state.isPresent(),
                state.map(StaffBankLogic.State::frozen).orElse(false),
                state.map(StaffBankLogic.State::balances).orElse(List.of()),
                state.map(StaffBankLogic.State::history).orElse(List.of()), codes, primaryCurrency(codes), auditIssues,
                excess));
    }

    private static void reply(ServerPlayer player, StaffBankAction action, StaffBankResult result) {
        reply(player, action, result, Optional.empty(), -1, List.of());
    }

    private static StaffBankResult map(StaffBankLogic.AdjustResult r) {
        return switch (r) {
            case OK -> StaffBankResult.OK;
            case INSUFFICIENT_FUNDS -> StaffBankResult.INSUFFICIENT_FUNDS;
            case FROZEN -> StaffBankResult.FROZEN;
            case UNKNOWN_CURRENCY -> StaffBankResult.UNKNOWN_CURRENCY;
            case INVALID -> StaffBankResult.INVALID;
            case REFUSED -> StaffBankResult.ERROR;
        };
    }

    public static void onAction(StaffBankActionPayload payload, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        StaffBankAction action = payload.action();
        if (!isStaff(player)) {
            if (changesState(action)) {
                BankStaffLog.screen(player, action.name().toLowerCase(java.util.Locale.ROOT), payload.target(),
                        Optional.empty(), "", dev.qrm.api.staff.StaffLogEntry.Outcome.DENIED);
            }
            reply(player, action, StaffBankResult.NO_PERMISSION);
            return;
        }
        if (!LIMITER.tryAcquire(player.getUUID(), MIN_INTERVAL_MS)) {
            reply(player, action, StaffBankResult.RATE_LIMITED);
            return;
        }
        try {
            run(player, action, payload.target(), payload.amount(), payload.currency());
        } catch (RuntimeException e) {
            LOG.error("Staff bank action {} failed for {}", action, player.getUUID(), e);
            reply(player, action, StaffBankResult.ERROR);
        }
    }

    private static void run(ServerPlayer staff, StaffBankAction action, Optional<UUID> targetId, String amount,
            String currency) {
        ServerPlayer target = targetId.map(id -> staff.level().getServer().getPlayerList().getPlayer(id)).orElse(null);
        if (targetId.isPresent() && target == null) {
            reply(staff, action, StaffBankResult.NO_TARGET);
            return;
        }
        Optional<CharacterId> character = target == null ? Optional.empty()
                : QRM.get(CharacterService.class).activeOf(new PlayerId(target.getUUID())).map(PlayerCharacter::id);
        if (action.needsTarget() && character.isEmpty()) {
            reply(staff, action, target == null ? StaffBankResult.NO_TARGET : StaffBankResult.NO_CHARACTER);
            return;
        }
        StaffBankLogic logic = BankMod.staffLogic();
        StaffBankResult result = StaffBankResult.OK;
        int auditIssues = -1;
        List<StaffBankLogic.Excess> excess = List.of();
        switch (action) {
            case FREEZE -> logic.setFrozen(character.get(), true);
            case UNFREEZE -> logic.setFrozen(character.get(), false);
            case GIVE_ATM -> {
                ItemStack stack = new ItemStack(BankItems.ATM_ITEM.get());
                if (!target.getInventory().add(stack)) target.drop(stack, false);
                result = StaffBankResult.ATM_GIVEN;
            }
            case AUDIT -> {
                StaffBankLogic.AuditResult audit = logic.audit(BankMod.denominations().currencies(), code -> {
                    long held = 0;
                    for (ServerPlayer p : staff.level().getServer().getPlayerList().getPlayers()) {
                        held += new PlayerCash(p).count(code);
                    }
                    return held;
                });
                auditIssues = audit.issues();
                excess = audit.excess();
                result = audit.issues() > 0 ? StaffBankResult.AUDIT_ISSUES
                        : audit.excess().isEmpty() ? StaffBankResult.AUDIT_CLEAN : StaffBankResult.CASH_EXCESS;
            }
            case GRANT, TAKE -> result = map(logic.adjust(character.get(), action == StaffBankAction.GRANT, amount,
                    currency));
            case REFRESH -> {}
        }
        if (action != StaffBankAction.REFRESH && action != StaffBankAction.AUDIT && result != null
                && (result == StaffBankResult.OK || result == StaffBankResult.ATM_GIVEN)) {
            boolean money = action == StaffBankAction.GRANT || action == StaffBankAction.TAKE;
            LOG.info("Staff {} ran bank {} on {}{}", staff.getGameProfile().name(), action, target.getGameProfile().name(),
                    money ? " (" + amount + " " + currency + ")" : "");
            BankStaffLog.screen(staff, action.name().toLowerCase(java.util.Locale.ROOT), Optional.of(target.getUUID()),
                    Optional.of(target.getGameProfile().name()), money ? amount + " " + currency : "",
                    dev.qrm.api.staff.StaffLogEntry.Outcome.OK);
        }
        reply(staff, action, result, character.map(logic::state), auditIssues, excess);
    }
}
