package dev.qrm.immersion.mc;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Configurazione lato server: le distanze e i tempi del modulo. Le modifiche si applicano senza riavvio. */
public final class ImmersionConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue LABEL_RANGE;
    public static final ModConfigSpec.BooleanValue LOCAL_CHAT;
    public static final ModConfigSpec.DoubleValue CHAT_RANGE;
    public static final ModConfigSpec.DoubleValue HANDSHAKE_REACH;
    public static final ModConfigSpec.IntValue HANDSHAKE_SECONDS;
    public static final ModConfigSpec.DoubleValue KNOCK_HEARD_NORMAL;
    public static final ModConfigSpec.DoubleValue KNOCK_HEARD_LOUD;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("names");
        LABEL_RANGE = b.comment("Distanza massima (in blocchi) a cui compare il nome di chi si conosce. Chi e' gia' dentro la vede cambiare al prossimo ingresso")
                .defineInRange("labelRange", 5.0, 1.0, 16.0);
        b.pop();

        b.push("chat");
        LOCAL_CHAT = b.comment("Se false, la chat e' quella normale di Minecraft: globale e con il nome utente")
                .define("localEnabled", true);
        CHAT_RANGE = b.comment("Distanza (in blocchi) entro cui si sente chi scrive in chat")
                .defineInRange("range", 15.0, 2.0, 64.0);
        b.pop();

        b.push("handshake");
        HANDSHAKE_REACH = b.comment("Distanza massima (in blocchi) per stringere la mano")
                .defineInRange("reach", 3.0, 1.0, 6.0);
        HANDSHAKE_SECONDS = b.comment("Per quanti secondi resta valida una mano tesa")
                .defineInRange("requestSeconds", 30, 5, 300);
        b.pop();

        b.push("knock");
        KNOCK_HEARD_NORMAL = b.comment("Distanza (in blocchi) a cui si sente una bussata normale")
                .defineInRange("heardNormal", 10.0, 2.0, 48.0);
        KNOCK_HEARD_LOUD = b.comment("Distanza (in blocchi) a cui si sente una bussata forte")
                .defineInRange("heardLoud", 20.0, 2.0, 64.0);
        b.pop();
        SPEC = b.build();
    }

    private ImmersionConfig() {}
}
