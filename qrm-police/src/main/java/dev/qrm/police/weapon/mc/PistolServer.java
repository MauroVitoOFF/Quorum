package dev.qrm.police.weapon.mc;

import dev.qrm.police.weapon.net.PistolPayloads;
import net.minecraft.server.level.ServerPlayer;

/** Lato server della pistola. */
public final class PistolServer {
    private PistolServer() {}

    /** Riceve un'intenzione del giocatore. Il corpo è vuoto: lo riempie il task server (C4). */
    public static void onInput(ServerPlayer player, PistolPayloads.Input.Kind kind) {}
}
