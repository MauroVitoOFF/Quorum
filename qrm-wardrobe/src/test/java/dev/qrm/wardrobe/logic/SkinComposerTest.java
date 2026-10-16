package dev.qrm.wardrobe.logic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SkinComposerTest {
    private static int[] image(int height, int tag) {
        int[] px = new int[SkinComposer.SIZE * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < SkinComposer.SIZE; x++) px[y * SkinComposer.SIZE + x] = tag | (y << 8) | x;
        }
        return px;
    }

    private static boolean isHead(int x, int y) {
        return y < 16;
    }

    @Test
    void headAndHatComeFromTheOriginalEverythingElseFromTheOutfit() {
        int[] outfit = image(64, 0x01000000);
        int[] original = image(64, 0x02000000);
        int[] out = SkinComposer.compose(outfit, original);
        assertEquals(64 * 64, out.length);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                int expected = isHead(x, y) ? original[y * 64 + x] : outfit[y * 64 + x];
                assertEquals(expected, out[y * 64 + x], "pixel " + x + "," + y);
            }
        }
    }

    @Test
    void doesNotModifyTheInputs() {
        int[] outfit = image(64, 0x01000000);
        int[] copy = outfit.clone();
        SkinComposer.compose(outfit, image(64, 0x02000000));
        assertArrayEquals(copy, outfit);
    }

    @Test
    void legacy64x32OriginalWorks() {
        int[] out = SkinComposer.compose(image(64, 0x01000000), image(32, 0x02000000));
        assertEquals(0x02000000, out[0] & 0xFF000000);
        assertEquals(0x01000000, out[20 * 64] & 0xFF000000);
    }

    @Test
    void mannequinHeadIsAValidOriginalAndPaintsOnlyTheHeadBase() {
        int grey = 0xFF8C8C8C;
        int[] head = SkinComposer.flatHead(grey);
        int[] out = SkinComposer.compose(image(64, 0x01000000), head);
        assertEquals(grey, out[0]);
        assertEquals(grey, out[15 * 64 + 31]);
        assertEquals(0, out[32], "lo strato esterno della testa resta trasparente");
        assertEquals(0x01000000, out[16 * 64] & 0xFF000000, "il corpo resta quello della divisa");
    }

    @Test
    void wrongLengthsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> SkinComposer.compose(new int[10], image(64, 0)));
        assertThrows(IllegalArgumentException.class, () -> SkinComposer.compose(image(64, 0), new int[10]));
    }
}
