package dev.qrm.neoforge;

import dev.qrm.core.CoreConfig;
import dev.qrm.neoforge.net.UiConfig;
import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class QrmConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<String> DB_URL;
    public static final ModConfigSpec.ConfigValue<String> DB_USER;
    public static final ModConfigSpec.ConfigValue<String> DB_PASSWORD;
    public static final ModConfigSpec.IntValue CHARACTER_SLOTS;
    public static final ModConfigSpec.IntValue NAME_MIN;
    public static final ModConfigSpec.IntValue NAME_MAX;
    public static final ModConfigSpec.ConfigValue<String> NAME_PATTERN;
    public static final ModConfigSpec.BooleanValue UNIQUE_NAMES;
    public static final ModConfigSpec.ConfigValue<String> DEFAULT_CURRENCY;
    public static final ModConfigSpec.BooleanValue SELF_DUTY;
    public static final ModConfigSpec.BooleanValue SHORT_ALIASES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> HUB_TABS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> HUD_ELEMENTS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        CoreConfig d = CoreConfig.defaults();

        b.push("database");
        DB_URL = b.comment("JDBC URL. Vuoto = SQLite in <world>/qrm/qrm.db. v0.1 include solo il driver SQLite.")
                .define("url", "");
        DB_USER = b.define("user", "");
        DB_PASSWORD = b.define("password", "");
        b.pop();

        b.push("characters");
        CHARACTER_SLOTS = b.comment("Slot personaggio per giocatore")
                .defineInRange("slots", d.defaultCharacterSlots(), 1, 50);
        NAME_MIN = b.defineInRange("nameMinLength", d.nameMinLength(), 1, 64);
        NAME_MAX = b.defineInRange("nameMaxLength", d.nameMaxLength(), 1, 64);
        NAME_PATTERN = b.comment("Espressione regolare valida per nome e cognome").define("namePattern", d.namePattern());
        UNIQUE_NAMES = b.define("uniqueNames", d.uniqueNames());
        b.pop();

        b.push("economy");
        DEFAULT_CURRENCY = b.comment("Valuta principale: usata dai comandi quando non specificata e mostrata nell'HUD")
                .define("defaultCurrency", "QRM");
        b.pop();

        b.push("jobs");
        SELF_DUTY = b.comment("I giocatori possono entrare/uscire di servizio da soli (comando /qrm duty e pulsante dell'hub). "
                + "Se false lo stato di servizio lo cambiano solo i moduli (JobService.setOnDuty); il logout mette comunque fuori servizio")
                .define("selfDuty", true);
        b.pop();

        b.push("commands");
        SHORT_ALIASES = b.comment("Registra le scorciatoie /job, /duty, /money (/balance, /bal, /pay), /char, /org e /perm, "
                + "oltre ai comandi /qrm. Metti false se un altro mod usa gli stessi nomi. Si applica al riavvio o con /reload")
                .define("shortAliases", true);
        b.pop();

        b.push("gui");
        b.push("hub");
        HUB_TABS = b.comment("Schede dell'hub, nell'ordine indicato: accounts, job, orgs. Elenco vuoto = hub non disponibile")
                .defineListAllowEmpty("tabs", UiConfig.ALL_TABS, () -> "", o -> o instanceof String);
        b.pop();
        b.push("hud");
        HUD_ELEMENTS = b.comment("Righe dell'HUD nell'ordine indicato: name, balance, job e id di righe dei moduli (es. bank:debt). Elenco vuoto = HUD nascosto")
                .defineListAllowEmpty("elements", UiConfig.ALL_HUD, () -> "", o -> o instanceof String);
        b.pop();
        b.pop();

        SPEC = b.build();
    }

    private QrmConfig() {}

    /** Configurazione dell'interfaccia normalizzata, da inviare ai client. */
    public static UiConfig uiConfig() {
        return UiConfig.of(HUB_TABS.get(), HUD_ELEMENTS.get(), SELF_DUTY.get());
    }

    public static CoreConfig toCoreConfig() {
        return new CoreConfig(CHARACTER_SLOTS.get(), NAME_MIN.get(), NAME_MAX.get(), NAME_PATTERN.get(), UNIQUE_NAMES.get());
    }
}
