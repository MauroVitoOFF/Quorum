package dev.qrm.medical.client;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.qrm.client.api.body.BodyPart;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * {@code /qrmbody toggle} mostra o nasconde la sagoma (e salva la scelta nella configurazione). {@code /qrmbody <zona|all>
 * <0-100>} e {@code /qrmbody clear} impostano a mano la gravità delle zone per provare la sagoma senza la logica delle
 * ferite. Solo client: non tocca il server.
 */
final class BodyCommand {
    private BodyCommand() {}

    static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("qrmbody")
                .then(Commands.literal("toggle").executes(c -> {
                    boolean on = !MedicalClientConfig.BODY_ENABLED.get();
                    MedicalClientConfig.BODY_ENABLED.set(on);
                    MedicalClientConfig.BODY_ENABLED.save();
                    c.getSource().sendSuccess(
                            () -> Component.translatable(on ? "qrm_medical.body.shown" : "qrm_medical.body.hidden"), false);
                    return 1;
                }))
                .then(Commands.literal("clear").executes(c -> {
                    ClientBody.clear();
                    c.getSource().sendSuccess(() -> Component.translatable("qrm_medical.body.cleared"), false);
                    return 1;
                }))
                .then(Commands.argument("part", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(names(), b))
                        .then(Commands.argument("percent", IntegerArgumentType.integer(0, 100)).executes(c -> {
                            String name = StringArgumentType.getString(c, "part");
                            float severity = IntegerArgumentType.getInteger(c, "percent") / 100f;
                            if (name.equals("all")) {
                                for (BodyPart part : BodyPart.values()) ClientBody.set(part, severity);
                                return 1;
                            }
                            for (BodyPart part : BodyPart.values()) {
                                if (part.id().equals(name)) {
                                    ClientBody.set(part, severity);
                                    return 1;
                                }
                            }
                            c.getSource().sendFailure(Component.translatable("qrm_medical.body.unknown_part", name));
                            return 0;
                        }))));
    }

    private static List<String> names() {
        List<String> out = new ArrayList<>();
        out.add("all");
        for (BodyPart part : BodyPart.values()) out.add(part.id());
        return out;
    }
}
