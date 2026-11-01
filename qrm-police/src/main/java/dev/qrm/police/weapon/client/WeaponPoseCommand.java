package dev.qrm.police.weapon.client;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * {@code /pistolpose}: messa a punto in gioco della posa della pistola in prima persona.
 *
 * <ul>
 *   <li>senza argomenti apre il pannello con i cursori ({@link WeaponPoseScreen});
 *   <li>{@code add <param> <delta>} / {@code set <param> <valore>}: cambia un parametro (x, y, z in sedicesimi; rx,
 *       ry, rz in gradi; scale; armx..armrz per il braccio) e si vede subito; col freccia su in chat si ripete;
 *   <li>{@code show}: scrive tutti i valori in chat e li copia negli appunti;
 *   <li>{@code reset}: torna ai valori di partenza.
 * </ul>
 */
public final class WeaponPoseCommand {

    private WeaponPoseCommand() {}

    public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("pistolpose")
                .executes(c -> {
                    if (!holding(c.getSource())) return 0;
                    WeaponPoseScreen.requestOpen();
                    return 1;
                })
                .then(Commands.literal("show").executes(c -> holding(c.getSource()) ? show(c.getSource()) : 0))
                .then(Commands.literal("reset").executes(c -> {
                    if (!holding(c.getSource())) return 0;
                    WeaponPose.reset();
                    return show(c.getSource());
                }))
                .then(Commands.literal("set").then(param(false)))
                .then(Commands.literal("add").then(param(true))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> param(boolean add) {
        return Commands.argument("param", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(WeaponPose.NAMES, b))
                .then(Commands.argument("value", FloatArgumentType.floatArg()).executes(c -> {
                    if (!holding(c.getSource())) return 0;
                    String name = StringArgumentType.getString(c, "param");
                    float v = FloatArgumentType.getFloat(c, "value");
                    if (!WeaponPose.NAMES.contains(name)) {
                        c.getSource().sendFailure(Component.literal("Parametro sconosciuto: " + name + " (" + WeaponPose.NAMES + ")"));
                        return 0;
                    }
                    WeaponPose.set(name, add ? WeaponPose.get(name) + v : v);
                    return show(c.getSource());
                }));
    }

    /** Sceglie l'arma in mano per la messa a punto; senza arma in mano avvisa. */
    private static boolean holding(CommandSourceStack src) {
        if (WeaponPose.selectHeld()) return true;
        src.sendFailure(Component.literal("Impugna un'arma per regolarne la posa."));
        return false;
    }

    private static int show(CommandSourceStack src) {
        String line = WeaponPose.summary();
        Minecraft.getInstance().keyboardHandler.setClipboard(line);
        src.sendSuccess(() -> Component.literal(line + "  (copiato negli appunti)"), false);
        return 1;
    }
}
