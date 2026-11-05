package dev.qrm.client.api.body;

import net.minecraft.resources.Identifier;

/**
 * Un set di sette texture della sagoma: {@code <namespace>:textures/gui/<folder>/<parte>.png}, tutte della stessa
 * dimensione e con la stessa inquadratura, così si sovrappongono senza ritocchi.
 */
public record BodySkin(String namespace, String folder, int width, int height) {
    public Identifier texture(BodyPart part) {
        return Identifier.fromNamespaceAndPath(namespace, "textures/gui/" + folder + "/" + part.id() + ".png");
    }
}
