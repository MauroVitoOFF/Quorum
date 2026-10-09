package dev.qrm.neoforge.client.theme;

import dev.qrm.client.api.theme.QrmTheme;
import dev.qrm.client.api.theme.ThemeFragments;
import dev.qrm.client.api.theme.ThemeManifest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ricarica il tema a ogni ricarica delle risorse (F3+T, cambio pack, resource pack del server).
 * Il manifesto visibile ai pack è {@code assets/qrm/theme/default.json}: l'ultimo pack che lo fornisce vince.
 * Dopo il tema di QRM si uniscono i frammenti dei moduli registrati con {@link ThemeFragments}.
 */
public final class ThemeLoader implements ResourceManagerReloadListener {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("qrm", "theme");
    private static final Identifier MANIFEST = Identifier.fromNamespaceAndPath("qrm", "theme/default.json");
    private static final Logger LOG = LoggerFactory.getLogger("qrm");

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        ThemeManifest theme = loadMain(resourceManager);
        for (ThemeFragments.Fragment fragment : ThemeFragments.all()) {
            theme = mergeFragment(resourceManager, theme, fragment);
        }
        QrmTheme.set(theme);
    }

    private static ThemeManifest loadMain(ResourceManager resourceManager) {
        ThemeManifest defaults = BuiltInTheme.manifest();
        Optional<Resource> resource = resourceManager.getResource(MANIFEST);
        if (resource.isEmpty()) return defaults;
        try {
            ThemeManifest.Result result = ThemeManifest.parse(read(resource.get()), defaults);
            result.warnings().forEach(w -> LOG.warn("QRM theme: {}", w));
            if (result.usedFallback()) {
                LOG.error("QRM theme manifest from the active pack is invalid: using the built-in theme");
            }
            return result.manifest();
        } catch (IOException | RuntimeException e) {
            LOG.error("QRM theme could not be read: using the built-in theme", e);
            return defaults;
        }
    }

    /**
     * Pila di risorse del frammento (priorità crescente): la prima è il predefinito del modulo, l'ultima è quella di un
     * eventuale resource pack. Un frammento invalido è saltato.
     */
    private static ThemeManifest mergeFragment(ResourceManager resourceManager, ThemeManifest theme,
            ThemeFragments.Fragment fragment) {
        List<Resource> stack = resourceManager.getResourceStack(fragment.resource());
        if (stack.isEmpty()) {
            LOG.error("QRM theme fragment of {} not found: {}", fragment.modId(), fragment.resource());
            return theme;
        }
        try {
            ThemeManifest active = ThemeManifest.parseBuiltIn(read(stack.get(0)));
            if (stack.size() > 1) {
                ThemeManifest.Result result = ThemeManifest.parse(read(stack.get(stack.size() - 1)), active);
                result.warnings().forEach(w -> LOG.warn("QRM theme ({}): {}", fragment.modId(), w));
                active = result.manifest();
            }
            List<String> warnings = new ArrayList<>();
            ThemeManifest merged = theme.withFragment(fragment.modId(), active, warnings);
            warnings.forEach(w -> LOG.warn("QRM theme ({}): {}", fragment.modId(), w));
            return merged;
        } catch (IOException | RuntimeException e) {
            LOG.error("QRM theme fragment of {} could not be used", fragment.modId(), e);
            return theme;
        }
    }

    private static String read(Resource resource) throws IOException {
        try (BufferedReader reader = resource.openAsReader()) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
}
