package dev.qrm.bank.logic;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** L'esempio di {@code cash.json} mostrato nella wiki ({@code docs/samples/bank}) deve restare valido. */
class WikiSamplesTest {
    @Test
    void cashSampleIsValid() throws IOException {
        CashDenominations d = CashDenominations.parse(
                Files.readString(Path.of("..", "docs", "samples", "bank", "cash.json")));
        assertEquals(List.of(10000L, 5000L, 1000L, 500L, 100L), d.descending("QRM"));
    }
}
