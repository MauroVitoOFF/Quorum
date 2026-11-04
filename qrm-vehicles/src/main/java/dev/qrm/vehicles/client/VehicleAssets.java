package dev.qrm.vehicles.client;

import dev.qrm.client.api.rig.GeoModel;
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

    private VehicleAssets() {}

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
            model.meshes().forEach((bone, mesh) -> meshes.put(bone, new RigMesh(mesh)));
            data = new Data(model, Map.copyOf(meshes));
            LOG.debug("jeep_wrangler: {} ossa, {} mesh", model.rig().bones().size(), meshes.size());
        } catch (Exception e) {
            LOG.error("Modello del veicolo non caricato: {} ({})", JEEP_GEO, e.toString());
        }
    }
}
