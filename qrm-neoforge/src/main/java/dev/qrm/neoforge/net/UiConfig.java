package dev.qrm.neoforge.net;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Configurazione dell'interfaccia decisa dal server: schede dell'hub (nell'ordine indicato), righe dell'HUD e se i
 * giocatori possono cambiare da soli il proprio servizio. Viaggia dentro {@link PlayerStatus}.
 */
public record UiConfig(List<String> hubTabs, List<String> hudElements, boolean selfDuty) {
    public static final String TAB_ACCOUNTS = "accounts";
    public static final String TAB_JOB = "job";
    public static final String TAB_ORGS = "orgs";
    public static final String HUD_NAME = "name";
    public static final String HUD_BALANCE = "balance";
    public static final String HUD_JOB = "job";

    public static final List<String> ALL_TABS = List.of(TAB_ACCOUNTS, TAB_JOB, TAB_ORGS);
    public static final List<String> ALL_HUD = List.of(HUD_NAME, HUD_BALANCE, HUD_JOB);
    public static final UiConfig DEFAULT = new UiConfig(ALL_TABS, ALL_HUD, true);

    static final int MAX_NAMES = 8;
    static final int MAX_NAME_LENGTH = 16;
    static final int MAX_HUD_ELEMENTS = 16;
    static final int MAX_HUD_ELEMENT_LENGTH = 48;
    /** Stessa forma di {@code HudRows} (qrm-client-api); si duplica qui perché il server non carica il codice client. */
    private static final Pattern MODULE_ROW_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    private static final StreamCodec<ByteBuf, List<String>> NAMES =
            ByteBufCodecs.stringUtf8(MAX_NAME_LENGTH).apply(ByteBufCodecs.list(MAX_NAMES));
    private static final StreamCodec<ByteBuf, List<String>> HUD_NAMES =
            ByteBufCodecs.stringUtf8(MAX_HUD_ELEMENT_LENGTH).apply(ByteBufCodecs.list(MAX_HUD_ELEMENTS));

    public static final StreamCodec<ByteBuf, UiConfig> CODEC = StreamCodec.composite(
            NAMES, UiConfig::hubTabs,
            HUD_NAMES, UiConfig::hudElements,
            ByteBufCodecs.BOOL, UiConfig::selfDuty,
            UiConfig::new);

    public UiConfig {
        hubTabs = List.copyOf(hubTabs);
        hudElements = List.copyOf(hudElements);
    }

    /**
     * Normalizza i valori letti dalla configurazione. Schede: solo nomi noti. Elementi dell'HUD: nomi noti o id di riga di
     * modulo ben formati. Minuscole, senza duplicati, ordine conservato.
     */
    public static UiConfig of(Collection<?> tabs, Collection<?> hud, boolean selfDuty) {
        return new UiConfig(normalize(tabs, false), normalize(hud, true), selfDuty);
    }

    private static List<String> normalize(Collection<?> raw, boolean hud) {
        List<String> out = new ArrayList<>();
        for (Object o : raw) {
            if (!(o instanceof String s)) continue;
            String name = s.strip().toLowerCase(Locale.ROOT);
            boolean valid = hud ? ALL_HUD.contains(name) || isModuleRowId(name) : ALL_TABS.contains(name);
            if (valid && !out.contains(name)) out.add(name);
        }
        return out;
    }

    static boolean isModuleRowId(String name) {
        return name.length() <= MAX_HUD_ELEMENT_LENGTH && MODULE_ROW_ID.matcher(name).matches();
    }

    public boolean hasTab(String name) {
        return hubTabs.contains(name);
    }

    public boolean hasHud(String name) {
        return hudElements.contains(name);
    }
}
