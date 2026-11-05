package dev.qrm.vehicles.client;

import dev.qrm.client.api.rig.GeoModel;
import dev.qrm.client.api.rig.PartMesh;
import dev.qrm.client.api.rig.Rig;
import dev.qrm.client.api.rig.RigMesh;
import dev.qrm.vehicles.logic.VehicleDef;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Il modello di ogni veicolo e la mesh di ogni osso, ricaricati con le risorse del client (F3+T compreso). Un file
 * mancante o rotto produce un solo errore nel log e lascia i dati che c'erano prima di quel veicolo (vuoti al primo
 * caricamento): il client non si ferma mai per un asset di un veicolo. Il geo di un veicolo sta in
 * {@code geo/<id>.geo.json}, la texture in {@code textures/entity/<id>.png}.
 */
public final class VehicleAssets implements ResourceManagerReloadListener {

    public static final Identifier ID = Identifier.fromNamespaceAndPath("qrm_vehicles", "vehicle_assets");

    private static final Logger LOG = LoggerFactory.getLogger("qrm_vehicles");
    private static final VehicleAssets INSTANCE = new VehicleAssets();

    private record Data(GeoModel model, Map<String, RigMesh> meshes) {
        static final Data EMPTY = new Data(new GeoModel(Rig.EMPTY, Map.of(), Map.of(), 1, 1), Map.of());
    }

    private static volatile Map<String, Data> data = Map.of();

    /** Il vetro è quello di Minecraft, non la texture del veicolo. */
    public static final Identifier GLASS_TEXTURE =
            Identifier.fromNamespaceAndPath("minecraft", "textures/block/glass.png");

    private VehicleAssets() {}

    public static Identifier texture(String vehicleId) {
        return Identifier.fromNamespaceAndPath("qrm_vehicles", "textures/entity/" + vehicleId + ".png");
    }

    private static Identifier geo(String vehicleId) {
        return Identifier.fromNamespaceAndPath("qrm_vehicles", "geo/" + vehicleId + ".geo.json");
    }

    /**
     * Le ossa disegnate con il vetro di Minecraft. Un {@code pane_} non lo è mai, anche se finisce in {@code _glass}
     * (per esempio {@code pane_side_glass}): usa i suoi UV sulla texture del veicolo.
     */
    public static boolean isGlass(String bone) {
        return !isPane(bone) && (bone.endsWith("_glass") || bone.equals("rear_window"));
    }

    /**
     * Vetri disegnati con la texture del veicolo in un passaggio translucido: la forma la decidono i pixel
     * trasparenti della texture (per esempio il parabrezza che segue l'inclinazione del montante).
     */
    public static boolean isPane(String bone) {
        return bone.startsWith("pane_");
    }

    /** Ossa che non si disegnano guardando dall'interno: i vetri di entrambi i tipi. */
    public static boolean isTransparent(String bone) {
        return isGlass(bone) || isPane(bone);
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

    private static Data of(String vehicleId) {
        return data.getOrDefault(vehicleId, Data.EMPTY);
    }

    public static GeoModel model(String vehicleId) {
        return of(vehicleId).model();
    }

    /** Mesh dell'osso, o {@link RigMesh#EMPTY} se non ha cubi o il file non si è caricato. */
    public static RigMesh mesh(String vehicleId, String bone) {
        return of(vehicleId).meshes().getOrDefault(bone, RigMesh.EMPTY);
    }

    @Override
    public void onResourceManagerReload(ResourceManager resources) {
        Map<String, Data> next = new HashMap<>(data);
        for (VehicleDef def : VehicleDef.ALL) {
            Identifier file = geo(def.id());
            try (Reader reader = resources.openAsReader(file)) {
                GeoModel model = GeoModel.parse(reader);
                Map<String, RigMesh> meshes = new HashMap<>();
                model.meshes().forEach((bone, mesh) ->
                        meshes.put(bone, new RigMesh(isGlass(bone) ? wholeTexturePerFace(mesh) : mesh)));
                next.put(def.id(), new Data(model, Map.copyOf(meshes)));
                LOG.debug("{}: {} ossa, {} mesh", def.id(), model.rig().bones().size(), meshes.size());
            } catch (Exception e) {
                LOG.error("Modello del veicolo non caricato: {} ({})", file, e.toString());
            }
        }
        data = Map.copyOf(next);
    }
}
