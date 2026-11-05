package dev.qrm.vehicles.mc;

import dev.qrm.vehicles.logic.Surface;
import dev.qrm.vehicles.logic.VehicleDef;
import dev.qrm.vehicles.logic.VehicleParams;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * I file di taratura in {@code config/qrm_vehicles/}: {@code surfaces.json} per i fondi e {@code <id>.json} per ogni
 * veicolo. Alla prima avvio si scrivono con i valori predefiniti; dopo si possono modificare (anche tenendo solo i campi
 * da cambiare) e vengono riletti a ogni avvio del server. Un file sbagliato si ignora con un avviso nel log, senza
 * fermare il gioco.
 */
final class VehicleConfigFiles {
    private static final Logger LOGGER = LoggerFactory.getLogger("qrm_vehicles");

    private VehicleConfigFiles() {}

    static void onServerStarting(ServerStartingEvent event) {
        load(FMLPaths.CONFIGDIR.get().resolve(VehiclesMod.MOD_ID));
    }

    /** Legge (e al primo avvio crea) i file nella cartella; i valori letti sostituiscono quelli predefiniti. */
    static void load(Path dir) {
        VehicleDef.clearOverrides();
        Surface.clearOverrides();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            LOGGER.warn("Cartella di configurazione non disponibile: {}", dir, e);
            return;
        }
        Path surfaces = dir.resolve("surfaces.json");
        if (Files.notExists(surfaces)) {
            write(surfaces, VehicleParams.surfacesToJson(List.of(Surface.values())));
        } else {
            String text = read(surfaces);
            if (text != null) {
                try {
                    VehicleParams.parseSurfaces(text).forEach(Surface::override);
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("{} ignorato: {}", surfaces.getFileName(), e.getMessage());
                }
            }
        }
        for (VehicleDef base : VehicleDef.ALL) {
            Path file = dir.resolve(base.id() + ".json");
            if (Files.notExists(file)) {
                write(file, VehicleParams.toJson(base));
                continue;
            }
            String text = read(file);
            if (text == null) continue;
            try {
                VehicleDef.override(VehicleParams.overlay(base, text));
            } catch (IllegalArgumentException e) {
                LOGGER.warn("{} ignorato, restano i valori predefiniti: {}", file.getFileName(), e.getMessage());
            }
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.warn("Impossibile leggere {}", file, e);
            return null;
        }
    }

    private static void write(Path file, String json) {
        try {
            Files.writeString(file, json + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.warn("Impossibile scrivere {}", file, e);
        }
    }
}
