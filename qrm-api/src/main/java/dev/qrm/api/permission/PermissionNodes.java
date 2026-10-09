package dev.qrm.api.permission;

import java.util.regex.Pattern;

/** Validazione di nodi permesso esatti ({@code police.arrest}) e pattern ({@code police.*}, {@code *}). */
public final class PermissionNodes {
    private static final int MAX_LENGTH = 128;
    private static final Pattern NODE = Pattern.compile("[a-z0-9_]+(\\.[a-z0-9_]+)*");
    private static final Pattern PATTERN = Pattern.compile("\\*|[a-z0-9_]+(\\.[a-z0-9_]+)*(\\.\\*)?");

    private PermissionNodes() {}

    public static boolean isExactNode(String s) {
        return s != null && s.length() <= MAX_LENGTH && NODE.matcher(s).matches();
    }

    public static boolean isPattern(String s) {
        return s != null && s.length() <= MAX_LENGTH && PATTERN.matcher(s).matches();
    }

    /** @return {@code s} se è un pattern valido, altrimenti lancia {@link IllegalArgumentException} */
    public static String requirePattern(String s) {
        if (!isPattern(s)) throw new IllegalArgumentException("invalid permission pattern: " + s);
        return s;
    }
}
