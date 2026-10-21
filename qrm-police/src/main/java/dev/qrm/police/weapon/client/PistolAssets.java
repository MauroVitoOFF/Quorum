package dev.qrm.police.weapon.client;

import dev.qrm.police.weapon.logic.anim.AnimClip;
import dev.qrm.police.weapon.logic.anim.AnimFormat;
import dev.qrm.police.weapon.logic.mesh.MeshBuilder;
import dev.qrm.police.weapon.logic.mesh.Rig;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Rig, mesh delle parti e clip di animazione della pistola, ricaricati con le risorse del client (F3+T compreso).
 *
 * <p>Un file mancante o rotto produce un solo errore nel log e lascia i dati che c'erano prima (vuoti al primo
 * caricamento): il client non si ferma mai per un asset della pistola.
 */
public final class PistolAssets implements ResourceManagerReloadListener {

    public static final Identifier ID = Identifier.fromNamespaceAndPath("qrm_police", "pistol_assets");
    public static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("qrm_police", "textures/item/police_pistol.png");
    private static final Identifier RIG = Identifier.fromNamespaceAndPath("qrm_police", "rigs/police_pistol.json");
    private static final Identifier ANIMATIONS =
            Identifier.fromNamespaceAndPath("qrm_police", "animations/police_pistol.json");
    private static final String DEFAULT_PARTS_PREFIX = "qrm_police:item/police_pistol_parts/";

    private static final Logger LOG = LoggerFactory.getLogger("qrm_police");
    private static final PistolAssets INSTANCE = new PistolAssets();

    private static volatile Rig rig = Rig.EMPTY;
    private static volatile Map<String, RigMesh> meshes = Map.of();
    private static volatile Map<String, AnimClip> clips = Map.of();

    private PistolAssets() {}

    /** Da chiamare sul bus del Module, lato client. */
    public static void onAddReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(ID, INSTANCE);
    }

    public static Rig rig() {
        return rig;
    }

    public static Map<String, AnimClip> clips() {
        return clips;
    }

    /** Mesh del bone, o {@link RigMesh#EMPTY} se il bone non ha cubi o il suo file non si è caricato. */
    public static RigMesh mesh(String bone) {
        return meshes.getOrDefault(bone, RigMesh.EMPTY);
    }

    /** Tipo di render della pistola: entità ritagliata (alpha a soglia), senza eliminazione delle facce nascoste. */
    public static RenderType renderType() {
        return RenderTypes.entityCutout(TEXTURE);
    }

    @Override
    public void onResourceManagerReload(ResourceManager resources) {
        Rig newRig = load(resources, RIG, Rig::parse);
        if (newRig != null) rig = newRig;

        Rig current = rig;
        String prefix = current.partsPrefix() != null ? current.partsPrefix() : DEFAULT_PARTS_PREFIX;
        Map<String, RigMesh> previous = meshes;
        Map<String, RigMesh> loaded = new HashMap<>();
        for (Rig.Bone bone : current.bones()) {
            if (!bone.hasModel()) continue;
            Identifier file;
            try {
                Identifier model = Identifier.parse(prefix + bone.name());
                file = Identifier.fromNamespaceAndPath(model.getNamespace(), "models/" + model.getPath() + ".json");
            } catch (RuntimeException e) {
                // Rig.parse già rifiuta nomi non validi: qui si protegge comunque la ricarica delle risorse.
                LOG.error("Mesh della pistola non caricata: bone '{}' ({})", bone.name(), e.toString());
                file = null;
            }
            RigMesh mesh = file == null ? null : load(resources, file, r -> new RigMesh(MeshBuilder.parse(r)));
            if (mesh == null) mesh = previous.getOrDefault(bone.name(), RigMesh.EMPTY);
            loaded.put(bone.name(), mesh);
        }
        meshes = Map.copyOf(loaded);

        Map<String, AnimClip> newClips = load(resources, ANIMATIONS, AnimFormat::parse);
        if (newClips != null) clips = Map.copyOf(newClips);

        LOG.debug("Pistola: {} bone, {} mesh, {} clip", current.bones().size(), loaded.size(), clips.size());
    }

    private interface Parser<T> {
        T parse(Reader reader) throws Exception;
    }

    /** Legge e interpreta un file; in caso di problemi registra un errore e restituisce null. */
    private static <T> T load(ResourceManager resources, Identifier file, Parser<T> parser) {
        try (Reader reader = resources.openAsReader(file)) {
            return parser.parse(reader);
        } catch (Exception e) {
            LOG.error("Asset della pistola non caricato: {} ({})", file, e.toString());
            return null;
        }
    }
}
