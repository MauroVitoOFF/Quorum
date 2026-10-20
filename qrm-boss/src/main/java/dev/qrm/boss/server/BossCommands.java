package dev.qrm.boss.server;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.org.Organization;
import dev.qrm.boss.logic.BossSnapshot;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** {@code /boss [azienda]} e {@code /qrm boss [azienda]}: apre il menu del boss. Le aziende suggerite sono solo quelle dell'utente. */
public final class BossCommands {
    private BossCommands() {}

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        // /boss resta sempre attivo (non dipende da commands.shortAliases); /qrm boss e' lo stesso nodo.
        event.getDispatcher().register(bossNode());
        event.getDispatcher().register(Commands.literal("qrm").then(bossNode()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> bossNode() {
        return Commands.literal("boss")
                .requires(s -> s.getEntity() instanceof ServerPlayer)
                .executes(c -> open(c, Optional.empty()))
                .then(Commands.argument("org", StringArgumentType.word())
                        .suggests((c, b) -> {
                            ServerPlayer p = c.getSource().getPlayer();
                            if (p == null) return b.buildFuture();
                            return SharedSuggestionProvider.suggest(
                                    menuOrgs(p).stream().map(Organization::id).toList(), b);
                        })
                        .executes(c -> open(c, Optional.of(StringArgumentType.getString(c, "org")))));
    }

    private static List<Organization> menuOrgs(ServerPlayer player) {
        return BossServer.activeCharacter(player)
                .map(pc -> BossServer.service().menuOrganizations(pc.id())).orElse(List.of());
    }

    private static int open(CommandContext<CommandSourceStack> c, Optional<String> argument) {
        CommandSourceStack source = c.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        Optional<PlayerCharacter> character = BossServer.activeCharacter(player);
        if (character.isEmpty()) {
            source.sendFailure(Component.translatable("qrm_boss.cmd.no_character"));
            return 0;
        }
        List<Organization> orgs = BossServer.service().menuOrganizations(character.get().id());
        Optional<Organization> chosen;
        if (argument.isPresent()) {
            chosen = orgs.stream().filter(o -> o.id().equals(argument.get())).findFirst();
            if (chosen.isEmpty()) {
                // Stesso messaggio per azienda inesistente o altrui: nessun dettaglio sulle aziende altrui.
                source.sendFailure(Component.translatable("qrm_boss.cmd.invalid_org"));
                return 0;
            }
        } else if (orgs.isEmpty()) {
            source.sendFailure(Component.translatable("qrm_boss.cmd.no_access"));
            return 0;
        } else if (orgs.size() > 1) {
            BossServer.openPicker(player, character.get().id(), orgs.stream()
                    .map(o -> new BossSnapshot.OrgChoice(o.id(), o.label())).toList());
            return 1;
        } else {
            chosen = Optional.of(orgs.get(0));
        }
        if (!BossServer.open(player, character.get().id(), chosen.get().id())) {
            source.sendFailure(Component.translatable("qrm_boss.cmd.no_access"));
            return 0;
        }
        return 1;
    }
}
