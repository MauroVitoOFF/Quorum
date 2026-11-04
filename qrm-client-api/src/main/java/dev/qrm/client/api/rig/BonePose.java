package dev.qrm.client.api.rig;

import java.util.HashMap;
import java.util.Map;
import org.joml.Matrix4f;

/**
 * Composizione pura delle matrici dei bone (solo JOML, nessuna classe di Minecraft).
 *
 * <p>Contratto della posa (lo stesso dell'esportatore): la trasformazione locale di un bone attorno al suo perno è
 * {@code T(pos) · T(perno) · Rz · Ry · Rx · S · T(-perno)}; i figli ereditano quella del genitore. Le mesh sono in
 * blocchi, perni e posizioni delle animazioni in unità Blockbench: entrambi si dividono per 16. Rotazioni in gradi,
 * destrorse come in JOML e nel vanilla.
 */
public final class BonePose {

    private static final float UNIT = 1f / 16f;

    private BonePose() {}

    /**
     * Matrice di ogni bone del rig nello spazio del modello (in blocchi), dati i bone animati; un bone assente dalla
     * posa vale riposo. I bone del rig sono già ordinati genitore prima del figlio.
     */
    public static Map<String, Matrix4f> compose(Rig rig, Map<String, BoneTransform> pose) {
        Map<String, Matrix4f> out = new HashMap<>();
        for (Rig.Bone bone : rig.bones()) {
            Matrix4f parent = bone.parent() == null ? null : out.get(bone.parent());
            Matrix4f m = parent == null ? new Matrix4f() : new Matrix4f(parent);
            m.mul(local(bone.pivot(), pose.getOrDefault(bone.name(), BoneTransform.REST)));
            out.put(bone.name(), m);
        }
        return out;
    }

    /** Trasformazione locale di un bone attorno al suo perno (unità Blockbench), espressa in blocchi. */
    public static Matrix4f local(float[] pivot, BoneTransform t) {
        float[] p = t.position();
        float[] r = t.rotationDeg();
        float[] s = t.scale();
        float px = pivot[0] * UNIT;
        float py = pivot[1] * UNIT;
        float pz = pivot[2] * UNIT;
        return new Matrix4f()
                .translate(p[0] * UNIT, p[1] * UNIT, p[2] * UNIT)
                .translate(px, py, pz)
                .rotateZ((float) Math.toRadians(r[2]))
                .rotateY((float) Math.toRadians(r[1]))
                .rotateX((float) Math.toRadians(r[0]))
                .scale(s[0], s[1], s[2])
                .translate(-px, -py, -pz);
    }

    /**
     * Cornice con l'origine nel perno del bone, già trasformata dalla sua matrice: serve per appendere qualcosa al
     * bone (per esempio la scritta del controllo sul caricatore).
     */
    public static Matrix4f pivotFrame(Rig.Bone bone, Matrix4f boneMatrix) {
        float[] p = bone.pivot();
        return new Matrix4f(boneMatrix).translate(p[0] * UNIT, p[1] * UNIT, p[2] * UNIT);
    }
}
