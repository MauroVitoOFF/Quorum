package dev.qrm.core;

public record CoreConfig(
        int defaultCharacterSlots, int nameMinLength, int nameMaxLength, String namePattern, boolean uniqueNames) {

    public static CoreConfig defaults() {
        return new CoreConfig(3, 2, 24, "^[\\p{L}][\\p{L}' -]*$", true);
    }

    public CoreConfig withCharacterSlots(int slots) {
        return new CoreConfig(slots, nameMinLength, nameMaxLength, namePattern, uniqueNames);
    }
}
