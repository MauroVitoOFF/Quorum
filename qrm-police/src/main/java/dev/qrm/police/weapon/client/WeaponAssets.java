package dev.qrm.police.weapon.client;

import dev.qrm.client.api.rig.RigMesh;

import dev.qrm.police.weapon.logic.anim.AnimClip;
import dev.qrm.police.weapon.logic.anim.AnimFormat;
import dev.qrm.police.weapon.logic.WeaponDef;
import dev.qrm.client.api.rig.MeshBuilder;
import dev.qrm.client.api.rig.Rig;
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
 * Rig, mesh delle parti e clip di animazione di ogni arma ({@link WeaponDef#all()}), ricaricati con le risorse del
 * client (F3+T compreso).
 *
 * <p>Un file mancante o rotto produce un solo errore nel log e lascia i dati che c'erano prima (vuoti al primo
 * caricamento): il client non si ferma mai per un asset di un'arma.
 */
public final class WeaponAssets implements ResourceManagerReloadListener {

    public static final Identifier ID = Identifier.fromNamespaceAndPath("qrm_police", "weapon_assets");
    private static final String DEFAULT_PARTS_PREFIX = "qrm_police:item/police_pistol_parts/";

    private static final Logger LOG = LoggerFactory.getLogger("qrm_police");
    private static final WeaponAssets INSTANCE = new WeaponAssets();

    /** I dati caricati di un'arma. */
    private record Data(Rig rig, Map<String, RigMesh> meshes, Map<String, AnimClip> clips) {
        static final Data EMPTY = new Data(Rig.EMPTY, Map.of(), Map.of());
    }

    private static volatile Map<String, Data> data = Map.of();

    private WeaponAssets() {}

    /** Da chiamare sul bus del Module, lato client. */
    public static void onAddReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(ID, INSTANCE);
    }

    private static Data of(WeaponDef def) {
        return data.getOrDefault(def.id(), Data.EMPTY);
    }

    public static Rig rig(WeaponDef def) {
        return of(def).rig();
    }

    public static Map<String, AnimClip> clips(WeaponDef def) {
        return of(def).clips();
    }

    /** Mesh del bone, o {@link RigMesh#EMPTY} se il bone non ha cubi o il suo file non si è caricato. */
    public static RigMesh mesh(WeaponDef def, String bone) {
        return of(def).meshes().getOrDefault(bone, RigMesh.EMPTY);
    }

    /** Tipo di render dell'arma: entità ritagliata (alpha a soglia), senza eliminazione delle facce nascoste. */
    public static RenderType renderType(WeaponDef def) {
        return RenderTypes.entityCutout(Identifier.parse(def.texture()));
    }

    @Override
    public void onResourceManagerReload(ResourceManager resources) {
        Map<String, Data> previous = data;
        Map<String, Data> next = new HashMap<>();
        for (WeaponDef def : WeaponDef.all().values()) {
            next.put(def.id(), reload(resources, def, previous.getOrDefault(def.id(), Data.EMPTY)));
        }
        data = Map.copyOf(next);
    }

    private static Data reload(ResourceManager resources, WeaponDef def, Data before) {
        Rig newRig = load(resources, Identifier.parse(def.rig()), Rig::parse);
        Rig rig = newRig != null ? newRig : before.rig();

        String prefix = rig.partsPrefix() != null ? rig.partsPrefix() : DEFAULT_PARTS_PREFIX;
        Map<String, RigMesh> loaded = new HashMap<>();
        for (Rig.Bone bone : rig.bones()) {
            if (!bone.hasModel()) continue;
            Identifier file;
            try {
                Identifier model = Identifier.parse(prefix + bone.name());
                file = Identifier.fromNamespaceAndPath(model.getNamespace(), "models/" + model.getPath() + ".json");
            } catch (RuntimeException e) {
                // Rig.parse già rifiuta nomi non validi: qui si protegge comunque la ricarica delle risorse.
                LOG.error("Mesh dell'arma non caricata: bone '{}' ({})", bone.name(), e.toString());
                file = null;
            }
            RigMesh mesh = file == null ? null : load(resources, file, r -> new RigMesh(MeshBuilder.parse(r)));
            if (mesh == null) mesh = before.meshes().getOrDefault(bone.name(), RigMesh.EMPTY);
            loaded.put(bone.name(), mesh);
        }

        Map<String, AnimClip> newClips = load(resources, Identifier.parse(def.animations()), AnimFormat::parse);
        Map<String, AnimClip> clips = newClips != null
                ? WeaponDef.withAliases(Map.copyOf(newClips), def.clipAliases()) : before.clips();

        LOG.debug("{}: {} bone, {} mesh, {} clip", def.id(), rig.bones().size(), loaded.size(), clips.size());
        return new Data(rig, Map.copyOf(loaded), clips);
    }

    private interface Parser<T> {
        T parse(Reader reader) throws Exception;
    }

    /** Legge e interpreta un file; in caso di problemi registra un errore e restituisce null. */
    private static <T> T load(ResourceManager resources, Identifier file, Parser<T> parser) {
        try (Reader reader = resources.openAsReader(file)) {
            return parser.parse(reader);
        } catch (Exception e) {
            LOG.error("Asset dell'arma non caricato: {} ({})", file, e.toString());
            return null;
        }
    }
}
