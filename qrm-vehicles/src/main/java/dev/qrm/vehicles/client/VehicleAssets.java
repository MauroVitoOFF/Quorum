package dev.qrm.vehicles.client;

import dev.qrm.client.api.rig.GeoModel;
import dev.qrm.client.api.rig.PartMesh;
import java.util.ArrayList;
import java.util.List;
import dev.qrm.client.api.rig.Rig;
import dev.qrm.client.api.rig.RigMesh;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Il modello della Jeep e la mesh di ogni osso, ricaricati con le risorse del client (F3+T compreso). Un file
 * mancante o rotto produce un solo errore nel log e lascia i dati che c'erano prima (vuoti al primo caricamento):
 * il client non si ferma mai per un asset di un veicolo.
 */
public final class VehicleAssets implements ResourceManagerReloadListener {

    public static final Identifier ID = Identifier.fromNamespaceAndPath("qrm_vehicles", "vehicle_assets");
    public static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("qrm_vehicles", "textures/entity/jeep_wrangler.png");
    private static final Identifier JEEP_GEO = Identifier.fromNamespaceAndPath("qrm_vehicles", "geo/jeep_wrangler.geo.json");

    private static final Logger LOG = LoggerFactory.getLogger("qrm_vehicles");
    private static final VehicleAssets INSTANCE = new VehicleAssets();

    private record Data(GeoModel model, Map<String, RigMesh> meshes) {
        static final Data EMPTY = new Data(new GeoModel(Rig.EMPTY, Map.of(), Map.of(), 1, 1), Map.of());
    }

    private static volatile Data data = Data.EMPTY;

    /** Il vetro è quello di Minecraft, non la texture del veicolo. */
    public static final Identifier GLASS_TEXTURE =
            Identifier.fromNamespaceAndPath("minecraft", "textures/block/glass.png");

    private VehicleAssets() {}

    /** Le ossa disegnate con il vetro di Minecraft. */
    public static boolean isGlass(String bone) {
        return bone.equals("windshield_glass") || bone.equals("rear_window");
    }

    /** Ogni faccia mostra l'intera texture del vetro, invece del ritaglio della texture del veicolo. */
    private static PartMesh wholeTexturePerFace(PartMesh mesh) {
        List<PartMesh.Quad> quads = new ArrayList<>();
        for (PartMesh.Quad q : mesh.quads()) {
            List<PartMesh.Vertex> vs = new ArrayList<>(4);
            for (int i = 0; i < 4; i++) {
                PartMesh.Vertex v = q.vertices().get(i);
                vs.add(new PartMesh.Vertex(v.x(), v.y(), v.z(), i == 0 || i == 3 ? 0f : 1f, i < 2 ? 0f : 1f));
            }
            quads.add(new PartMesh.Quad(vs, q.nx(), q.ny(), q.nz()));
        }
        return new PartMesh(quads);
    }

    /** Da chiamare sul bus del Module, lato client. */
    public static void onAddReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(ID, INSTANCE);
    }

    public static GeoModel model() {
        return data.model();
    }

    /** Mesh dell'osso, o {@link RigMesh#EMPTY} se non ha cubi o il file non si è caricato. */
    public static RigMesh mesh(String bone) {
        return data.meshes().getOrDefault(bone, RigMesh.EMPTY);
    }

    @Override
    public void onResourceManagerReload(ResourceManager resources) {
        try (Reader reader = resources.openAsReader(JEEP_GEO)) {
            GeoModel model = GeoModel.parse(reader);
            Map<String, RigMesh> meshes = new HashMap<>();
            model.meshes().forEach((bone, mesh) ->
                    meshes.put(bone, new RigMesh(isGlass(bone) ? wholeTexturePerFace(mesh) : mesh)));
            data = new Data(model, Map.copyOf(meshes));
            LOG.debug("jeep_wrangler: {} ossa, {} mesh", model.rig().bones().size(), meshes.size());
        } catch (Exception e) {
            LOG.error("Modello del veicolo non caricato: {} ({})", JEEP_GEO, e.toString());
        }
    }
}
