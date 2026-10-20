package dev.qrm.police.weapon.client;

import dev.qrm.police.weapon.net.PistolPayloads;

/**
 * Ponte dal canale di rete al client della pistola: caricata solo quando arriva davvero un pacchetto (mai sul
 * server dedicato). I metodi sono vuoti: li riempiono i task del client.
 */
public final class PistolClientBridge {
    private PistolClientBridge() {}

    public static void onActionStarted(PistolPayloads.ActionStarted p) {}

    public static void onActionEnded(PistolPayloads.ActionEnded p) {}

    public static void onShot(PistolPayloads.Shot p) {}

    public static void onAim(PistolPayloads.Aim p) {}

    public static void onRejected(PistolPayloads.Rejected p) {}

    public static void onIdentity(PistolPayloads.Identity p) {}
}
