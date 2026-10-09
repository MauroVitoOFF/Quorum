package dev.qrm.neoforge;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.qrm.api.QRM;
import dev.qrm.api.economy.CurrencyRegistry;
import dev.qrm.api.job.JobRegistry;
import dev.qrm.api.org.OrganizationService;
import java.util.List;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;

/** Completamento con Tab degli argomenti dei comandi: propone ciò che esiste davvero sul server. */
final class CommandSuggestions {
    static final SuggestionProvider<CommandSourceStack> JOBS =
            of(c -> Completions.jobIds(QRM.get(JobRegistry.class)));
    static final SuggestionProvider<CommandSourceStack> GRADES =
            of(c -> Completions.gradeIds(QRM.get(JobRegistry.class), argument(c, "job")));
    static final SuggestionProvider<CommandSourceStack> CURRENCIES =
            of(c -> Completions.currencyCodes(QRM.get(CurrencyRegistry.class)));
    static final SuggestionProvider<CommandSourceStack> ORGS =
            of(c -> Completions.orgIds(QRM.get(OrganizationService.class)));
    static final SuggestionProvider<CommandSourceStack> RANKS =
            of(c -> Completions.rankIds(QRM.get(OrganizationService.class), argument(c, "org")));

    private CommandSuggestions() {}

    private static SuggestionProvider<CommandSourceStack> of(
            Function<CommandContext<CommandSourceStack>, List<String>> values) {
        return (context, builder) -> {
            List<String> candidates;
            try {
                candidates = values.apply(context);
            } catch (RuntimeException e) {
                candidates = List.of(); // servizi non ancora pronti: nessun suggerimento, mai un errore
            }
            return SharedSuggestionProvider.suggest(candidates, builder);
        };
    }

    /** Argomento già scritto prima di quello che si sta completando, o {@code null}. */
    private static String argument(CommandContext<CommandSourceStack> context, String name) {
        try {
            return context.getArgument(name, String.class);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
