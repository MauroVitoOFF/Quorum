package dev.qrm.neoforge.server;

import dev.qrm.api.point.InactiveReason;
import dev.qrm.api.point.InteractionPoint;
import dev.qrm.api.point.PointError;
import dev.qrm.api.point.PointFace;
import dev.qrm.api.point.PointOwner;
import dev.qrm.api.point.PointPosition;
import dev.qrm.api.point.PointResult;
import dev.qrm.api.point.PointService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * La logica dei comandi {@code /qrm admin point}, separata dal parsing Brigadier così la futura schermata staff la
 * riusa. Pura: nessuna dipendenza da Minecraft.
 */
public final class PointCommandLogic {
    /** Esito di un comando: {@code ok} false = errore da mostrare in rosso. */
    public record Reply(boolean ok, String text) {
        static Reply ok(String text) {
            return new Reply(true, text);
        }

        static Reply fail(String text) {
            return new Reply(false, text);
        }
    }

    private final PointService points;
    private final Function<InteractionPoint, Optional<InactiveReason>> inactive;

    /** @param inactive causa di inattività completa (Core + mondo); vuoto = punto attivo */
    public PointCommandLogic(PointService points, Function<InteractionPoint, Optional<InactiveReason>> inactive) {
        this.points = points;
        this.inactive = inactive;
    }

    /** @param looked la faccia laterale inquadrata dallo staff: l'icona parte da lì ({@code null} = tutte e quattro) */
    public Reply create(String typeId, String ownerKind, String ownerId, String gradeOrNull, PointPosition target,
            PointFace looked) {
        Optional<PointOwner.Kind> kind = parseKind(ownerKind);
        if (kind.isEmpty()) return Reply.fail("Il proprietario deve essere 'job' oppure 'org'.");
        PointOwner owner;
        try {
            owner = new PointOwner(kind.get(), ownerId);
        } catch (IllegalArgumentException e) {
            return Reply.fail("Id del proprietario non valido: " + ownerId);
        }
        PointResult<InteractionPoint> r = points.create(typeId, owner, target, gradeOrNull, null, looked);
        if (r instanceof PointResult.Ok<InteractionPoint> ok) {
            return Reply.ok("Punto #" + ok.value().id() + " (" + typeId + ") creato per " + describe(owner) + " in "
                    + describe(target) + (looked != null ? ", icona sulla faccia " + faceName(looked) : "") + ".");
        }
        return error(r);
    }

    public Reply setGrade(PointPosition target, String gradeOrNone) {
        return atTarget(target, p -> {
            boolean none = gradeOrNone == null || gradeOrNone.equals("none");
            PointResult<InteractionPoint> r = points.setMinGrade(p.id(), none ? null : gradeOrNone);
            return r instanceof PointResult.Ok
                    ? Reply.ok("Punto #" + p.id() + ": " + (none ? "aperto a tutti i membri." : "grado minimo " + gradeOrNone + "."))
                    : error(r);
        });
    }

    public Reply setPublic(PointPosition target, boolean value) {
        return atTarget(target, p -> {
            PointResult<InteractionPoint> r = points.setPublicVisible(p.id(), value);
            return r instanceof PointResult.Ok
                    ? Reply.ok("Punto #" + p.id() + ": " + (value ? "visibile a tutti." : "visibile solo a chi può usarlo."))
                    : error(r);
        });
    }

    public Reply setName(PointPosition target, String nameOrNone) {
        return atTarget(target, p -> {
            boolean none = nameOrNone == null || nameOrNone.equals("none");
            if (!none && (nameOrNone.isBlank() || nameOrNone.length() > InteractionPoint.MAX_NAME)) {
                return Reply.fail("Il nome deve essere lungo da 1 a " + InteractionPoint.MAX_NAME + " caratteri.");
            }
            PointResult<InteractionPoint> r = points.setName(p.id(), none ? null : nameOrNone);
            return r instanceof PointResult.Ok
                    ? Reply.ok("Punto #" + p.id() + ": " + (none ? "nome rimosso." : "nome \"" + nameOrNone + "\"."))
                    : error(r);
        });
    }

    /**
     * @param mode {@code all}, {@code here} (la faccia che lo staff sta guardando) o {@code north|south|east|west}
     * @param looked la faccia inquadrata dallo staff, o {@code null} se non è una faccia laterale
     */
    public Reply setFace(PointPosition target, String mode, PointFace looked) {
        PointFace face;
        switch (mode == null ? "" : mode) {
            case "all" -> face = null;
            case "here" -> {
                if (looked == null) return Reply.fail("Inquadra una faccia laterale del blocco (non sopra né sotto).");
                face = looked;
            }
            case "north", "south", "east", "west" -> face = PointFace.valueOf(mode.toUpperCase(java.util.Locale.ROOT));
            default -> {
                return Reply.fail("La faccia deve essere all, here, north, south, east o west.");
            }
        }
        PointFace chosen = face;
        return atTarget(target, p -> {
            PointResult<InteractionPoint> r = points.setFace(p.id(), chosen);
            return r instanceof PointResult.Ok
                    ? Reply.ok("Punto #" + p.id() + ": icona " + faceText(chosen) + ".")
                    : error(r);
        });
    }

    public Reply move(int id, PointPosition target) {
        PointResult<InteractionPoint> r = points.move(id, target);
        return r instanceof PointResult.Ok ? Reply.ok("Punto #" + id + " spostato in " + describe(target) + ".") : error(r);
    }

    /** @param id il punto da rimuovere, o {@code null} per quello inquadrato */
    public Reply remove(Integer id, PointPosition target) {
        Optional<InteractionPoint> point = id != null ? points.find(id) : points.findAt(target);
        if (point.isEmpty()) return id != null ? Reply.fail("Il punto #" + id + " non esiste.") : noPointHere();
        points.remove(point.get().id());
        return Reply.ok("Punto #" + point.get().id() + " rimosso.");
    }

    public List<String> list(String ownerKind, String ownerId) {
        List<InteractionPoint> found;
        if (ownerKind == null) {
            found = points.all();
        } else {
            Optional<PointOwner.Kind> kind = parseKind(ownerKind);
            if (kind.isEmpty()) return List.of("Il proprietario deve essere 'job' oppure 'org'.");
            try {
                found = points.ownedBy(new PointOwner(kind.get(), ownerId));
            } catch (IllegalArgumentException e) {
                return List.of("Id del proprietario non valido: " + ownerId);
            }
        }
        if (found.isEmpty()) return List.of("Nessun punto.");
        List<String> lines = new ArrayList<>();
        for (InteractionPoint p : found) {
            lines.add("#" + p.id() + " " + p.typeId() + " - " + describe(p.owner()) + " - " + describe(p.position())
                    + " - " + status(p));
        }
        return lines;
    }

    public List<String> info(PointPosition target) {
        Optional<InteractionPoint> point = points.findAt(target);
        if (point.isEmpty()) return List.of(noPointHere().text());
        InteractionPoint p = point.get();
        List<String> lines = new ArrayList<>();
        lines.add("Punto #" + p.id() + " (" + p.typeId() + ")" + (p.name() != null ? " \"" + p.name() + "\"" : ""));
        lines.add("Proprietario: " + describe(p.owner()));
        lines.add("Posizione: " + describe(p.position()));
        lines.add("Grado minimo: " + (p.minGradeId() != null ? p.minGradeId() : "tutti i membri"));
        lines.add("Faccia dell'icona: " + faceText(p.face()));
        lines.add("Visibile a tutti: " + (p.publicVisible() ? "sì" : "no"));
        lines.add("Stato: " + status(p));
        return lines;
    }

    // ---- helpers -------------------------------------------------------------------------------------------

    private Reply atTarget(PointPosition target, Function<InteractionPoint, Reply> action) {
        Optional<InteractionPoint> point = points.findAt(target);
        return point.isEmpty() ? noPointHere() : action.apply(point.get());
    }

    private static Reply noPointHere() {
        return Reply.fail("Il blocco inquadrato non è un punto di interazione.");
    }

    private String status(InteractionPoint p) {
        return inactive.apply(p).map(r -> "inattivo (" + reasonText(r) + ")").orElse("attivo");
    }

    private static String faceName(PointFace face) {
        return switch (face) {
            case NORTH -> "nord";
            case SOUTH -> "sud";
            case EAST -> "est";
            case WEST -> "ovest";
        };
    }

    static String faceText(PointFace face) {
        if (face == null) return "su tutte le facce";
        return "solo sulla faccia " + switch (face) {
            case NORTH -> "nord";
            case SOUTH -> "sud";
            case EAST -> "est";
            case WEST -> "ovest";
        };
    }

    static String reasonText(InactiveReason reason) {
        return switch (reason) {
            case UNKNOWN_TYPE -> "tipo non registrato";
            case UNKNOWN_OWNER -> "proprietario inesistente";
            case UNKNOWN_GRADE -> "grado inesistente";
            case UNKNOWN_DIMENSION -> "dimensione inesistente";
            case MISSING_BLOCK -> "blocco mancante";
        };
    }

    static Optional<PointOwner.Kind> parseKind(String text) {
        if (text == null) return Optional.empty();
        return switch (text) {
            case "job" -> Optional.of(PointOwner.Kind.JOB);
            case "org" -> Optional.of(PointOwner.Kind.ORGANIZATION);
            default -> Optional.empty();
        };
    }

    static String describe(PointOwner owner) {
        return (owner.kind() == PointOwner.Kind.JOB ? "job " : "org ") + owner.id();
    }

    static String describe(PointPosition p) {
        return p.dimension() + " " + p.x() + " " + p.y() + " " + p.z();
    }

    private static Reply error(PointResult<?> r) {
        PointError e = ((PointResult.Failed<?>) r).error();
        return Reply.fail(switch (e) {
            case PointError.UnknownType t -> "Tipo di punto sconosciuto: " + t.typeId();
            case PointError.OwnerNotAllowed o -> "Il tipo " + o.typeId() + " non accetta proprietari di tipo "
                    + (o.kind() == PointOwner.Kind.JOB ? "job" : "org") + ".";
            case PointError.UnknownOwner o -> "Proprietario inesistente o archiviato: " + describe(o.owner());
            case PointError.UnknownGrade g -> "Grado inesistente nel proprietario: " + g.gradeId();
            case PointError.PositionTaken t -> "Il blocco ospita già il punto #" + t.pointId() + ".";
            case PointError.NotFound n -> "Il punto #" + n.pointId() + " non esiste.";
        });
    }
}
