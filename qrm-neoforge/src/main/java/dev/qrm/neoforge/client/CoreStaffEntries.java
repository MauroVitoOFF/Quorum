package dev.qrm.neoforge.client;

import dev.qrm.api.staff.StaffNodes;
import dev.qrm.client.api.staff.StaffEntries;
import dev.qrm.client.api.staff.StaffPlayerActions;
import dev.qrm.neoforge.net.PageKind;
import dev.qrm.neoforge.net.PlayerAction;
import java.util.List;

/** Azioni della scheda giocatore e voci di sezione fornite da QRM, registrate come quelle di un modulo qualsiasi. */
public final class CoreStaffEntries {
    private CoreStaffEntries() {}

    public static void register() {
        // Azioni della scheda, nell'ordine in cui compaiono. Un'azione senza esecuzione sarebbe visibile ma disattivata ("presto").
        simple(PlayerAction.TELEPORT_TO, false);
        simple(PlayerAction.BRING, false);
        simple(PlayerAction.FREEZE, false);
        simple(PlayerAction.HEAL, false);
        input(PlayerAction.GIVE_ITEM, false, new StaffPlayerActions.Field("qrm.staff.input.item", "qrm.staff.input.item.hint", 64, true));
        StaffPlayerActions.register(StaffPlayerActions.Action.withChoices("set_gamemode", label("set_gamemode"),
                PlayerAction.SET_GAMEMODE.node(), false,
                List.of(choice("survival"), choice("creative"), choice("adventure"), choice("spectator")),
                ctx -> PlayerActionClient.send(PlayerAction.SET_GAMEMODE, ctx.player(), ctx.arg())));
        input(PlayerAction.KICK, true, new StaffPlayerActions.Field("qrm.staff.input.reason", "qrm.staff.input.reason.hint", 128, false));
        input(PlayerAction.BAN, true,
                new StaffPlayerActions.Field("qrm.staff.input.duration", "qrm.staff.input.duration.hint", 16, false),
                new StaffPlayerActions.Field("qrm.staff.input.reason", "qrm.staff.input.reason.hint", 128, false));
        input(PlayerAction.MUTE, true,
                new StaffPlayerActions.Field("qrm.staff.input.duration", "qrm.staff.input.duration.hint", 16, false),
                new StaffPlayerActions.Field("qrm.staff.input.reason", "qrm.staff.input.reason.hint", 128, false));
        simple(PlayerAction.UNMUTE, false);
        input(PlayerAction.WARN, false, new StaffPlayerActions.Field("qrm.staff.input.reason", "qrm.staff.input.warn.hint", 128, true));
        input(PlayerAction.NOTE, false, new StaffPlayerActions.Field("qrm.staff.input.note", "qrm.staff.input.note.hint", 200, true));
        simple(PlayerAction.VIEW_CHARACTER, false);
        simple(PlayerAction.VIEW_PERMISSIONS, false);
        simple(PlayerAction.WARNINGS, false);
        simple(PlayerAction.NOTES, false);
        simple(PlayerAction.HISTORY, false);

        // Voci di sezione: pagine di righe chieste al server.
        page("recent_log", StaffEntries.Section.LOGS, "qrm.staff.log.recent", StaffNodes.LOGS, PageKind.LOG);
        page("active_mutes", StaffEntries.Section.MODERATION, "qrm.staff.moderation.mutes", StaffNodes.MODERATION, PageKind.MUTES);
        page("recent_warnings", StaffEntries.Section.MODERATION, "qrm.staff.moderation.warnings", StaffNodes.MODERATION,
                PageKind.WARNINGS);
    }

    private static StaffPlayerActions.Choice choice(String mode) {
        return new StaffPlayerActions.Choice(mode, "qrm.staff.gamemode." + mode);
    }

    /** Azione semplice: una pressione invia l'azione al server. */
    private static void simple(PlayerAction action, boolean danger) {
        StaffPlayerActions.register(StaffPlayerActions.Action.of(action.id(), label(action.id()), action.node(), danger,
                ctx -> PlayerActionClient.send(action, ctx.player(), "")));
    }

    /** Azione con finestrella di input: i valori dei campi arrivano al server come argomento (uno per riga). */
    private static void input(PlayerAction action, boolean danger, StaffPlayerActions.Field... fields) {
        StaffPlayerActions.register(StaffPlayerActions.Action.withInput(action.id(), label(action.id()), action.node(), danger,
                List.of(fields), ctx -> PlayerActionClient.send(action, ctx.player(), ctx.arg())));
    }

    private static void page(String id, StaffEntries.Section section, String labelKey, String node, PageKind kind) {
        StaffEntries.register(new StaffEntries.Entry(id, section, labelKey, node, false, () -> true,
                ctx -> PageClient.request(kind)));
    }

    private static String label(String id) {
        return "qrm.staff.player.action." + id;
    }
}
