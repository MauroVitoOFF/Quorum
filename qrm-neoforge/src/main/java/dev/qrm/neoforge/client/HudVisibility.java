package dev.qrm.neoforge.client;

/** Quando disegnare l'HUD del personaggio. Pura: nessuna dipendenza da Minecraft. */
public final class HudVisibility {
    private HudVisibility() {}

    public static boolean visible(boolean noScreenOpen, boolean uiHidden, boolean enabled, boolean hasCharacter) {
        return noScreenOpen && !uiHidden && enabled && hasCharacter;
    }
}
