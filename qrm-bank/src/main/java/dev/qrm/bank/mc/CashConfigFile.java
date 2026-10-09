package dev.qrm.bank.mc;

import dev.qrm.bank.logic.CashDenominations;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Legge (o crea con i valori predefiniti) {@code config/qrm_bank/cash.json}. */
public final class CashConfigFile {
    private CashConfigFile() {}

    /** @throws IllegalStateException con un messaggio che indica il file, se illeggibile o invalido */
    public static CashDenominations load(Path dir) {
        Path file = dir.resolve("cash.json");
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(dir);
                Files.writeString(file, CashDenominations.DEFAULT_JSON, StandardCharsets.UTF_8);
            }
            return CashDenominations.parse(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + file + ": " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(file + ": " + e.getMessage(), e);
        }
    }
}
