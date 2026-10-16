package dev.qrm.boss.logic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Formato dei pacchetti del menu, senza Minecraft (così si prova nei test). I pacchetti lo avvolgono in un array di byte.
 * In lettura ogni stringa e ogni lista ha un tetto: un pacchetto malformato lancia {@link IllegalArgumentException}.
 */
public final class BossWire {
    static final int MAX_TEXT = 160;
    static final int MAX_NAME = 160;

    private BossWire() {}

    // ---- richiesta ---------------------------------------------------------------------------------------

    public static byte[] encode(BossRequest r) {
        return write(out -> {
            out.writeByte(r.action().ordinal());
            str(out, r.orgId(), BossRequest.MAX_ID);
            uuid(out, r.character());
            out.writeBoolean(r.target().isPresent());
            if (r.target().isPresent()) uuid(out, r.target().get());
            str(out, r.rankId(), BossRequest.MAX_ID);
            str(out, r.amount(), BossRequest.MAX_AMOUNT);
            str(out, r.text(), BossRequest.MAX_TEXT);
            uuid(out, r.requestId());
        });
    }

    public static BossRequest decodeRequest(byte[] bytes) {
        return read(bytes, in -> {
            BossAction action = BossAction.fromOrdinal(in.readUnsignedByte());
            String orgId = str(in, BossRequest.MAX_ID);
            UUID character = uuid(in);
            Optional<UUID> target = in.readBoolean() ? Optional.of(uuid(in)) : Optional.empty();
            String rankId = str(in, BossRequest.MAX_ID);
            String amount = str(in, BossRequest.MAX_AMOUNT);
            String text = str(in, BossRequest.MAX_TEXT);
            return new BossRequest(action, orgId, character, target, rankId, amount, text, uuid(in));
        });
    }

    // ---- risposta ----------------------------------------------------------------------------------------

    public static byte[] encode(BossReply r) {
        return write(out -> {
            out.writeByte(r.result().ordinal());
            out.writeBoolean(r.snapshot().isPresent());
            if (r.snapshot().isPresent()) snapshot(out, r.snapshot().get());
        });
    }

    public static BossReply decodeReply(byte[] bytes) {
        return read(bytes, in -> {
            BossResult result = BossResult.fromOrdinal(in.readUnsignedByte());
            return new BossReply(result, in.readBoolean() ? Optional.of(snapshot(in)) : Optional.empty());
        });
    }

    private static void snapshot(DataOutput out, BossSnapshot s) throws IOException {
        str(out, s.orgId(), BossRequest.MAX_ID);
        str(out, s.orgLabel(), MAX_NAME);
        uuid(out, s.character());
        str(out, s.rankLabel(), MAX_NAME);
        out.writeInt(s.level());
        out.writeBoolean(s.showMembers());
        out.writeBoolean(s.canManage());
        out.writeInt(s.memberCount());
        out.writeInt(s.online());
        out.writeInt(s.onDuty());
        str(out, s.notice(), BossPerks.MAX_NOTICE);
        str(out, s.noticeAuthor(), MAX_NAME);
        out.writeLong(s.noticeAt());
        out.writeBoolean(s.canNotice());
        str(out, s.currency(), MAX_TEXT);
        list(out, s.members(), BossSnapshot.MAX_MEMBERS, m -> {
            uuid(out, m.character());
            str(out, m.name(), MAX_NAME);
            str(out, m.rankId(), BossRequest.MAX_ID);
            str(out, m.rankLabel(), MAX_NAME);
            out.writeInt(m.level());
            out.writeBoolean(m.editable());
            str(out, m.title(), BossPerks.MAX_TITLE);
            out.writeBoolean(m.canTitle());
            out.writeBoolean(m.canBonus());
            uuid(out, m.player());
            out.writeByte(m.presence());
            list(out, m.log(), BossSnapshot.MAX_LOG, e -> {
                out.writeByte(e.kind());
                out.writeLong(e.at());
                str(out, e.text(), BossPerks.MAX_LOG_TEXT);
            });
        });
        list(out, s.ranks(), BossSnapshot.MAX_RANKS, k -> {
            str(out, k.id(), BossRequest.MAX_ID);
            str(out, k.label(), MAX_NAME);
            out.writeInt(k.level());
        });
        list(out, s.candidates(), BossSnapshot.MAX_CANDIDATES, c -> {
            uuid(out, c.character());
            str(out, c.name(), MAX_NAME);
        });
    }

    private static BossSnapshot snapshot(DataInput in) throws IOException {
        String orgId = str(in, BossRequest.MAX_ID);
        String orgLabel = str(in, MAX_NAME);
        UUID character = uuid(in);
        String rankLabel = str(in, MAX_NAME);
        int level = in.readInt();
        boolean showMembers = in.readBoolean();
        boolean canManage = in.readBoolean();
        int memberCount = in.readInt();
        int online = in.readInt();
        int onDuty = in.readInt();
        String notice = str(in, BossPerks.MAX_NOTICE);
        String noticeAuthor = str(in, MAX_NAME);
        long noticeAt = in.readLong();
        boolean canNotice = in.readBoolean();
        String currency = str(in, MAX_TEXT);
        List<BossSnapshot.Member> members = list(in, BossSnapshot.MAX_MEMBERS, () -> new BossSnapshot.Member(
                uuid(in), str(in, MAX_NAME), str(in, BossRequest.MAX_ID), str(in, MAX_NAME), in.readInt(),
                in.readBoolean(), str(in, BossPerks.MAX_TITLE), in.readBoolean(), in.readBoolean(), uuid(in),
                in.readUnsignedByte(), list(in, BossSnapshot.MAX_LOG, () -> new BossSnapshot.LogEntry(
                        in.readUnsignedByte(), in.readLong(), str(in, BossPerks.MAX_LOG_TEXT)))));
        List<BossSnapshot.RankOption> ranks = list(in, BossSnapshot.MAX_RANKS, () -> new BossSnapshot.RankOption(
                str(in, BossRequest.MAX_ID), str(in, MAX_NAME), in.readInt()));
        List<BossSnapshot.Candidate> candidates = list(in, BossSnapshot.MAX_CANDIDATES,
                () -> new BossSnapshot.Candidate(uuid(in), str(in, MAX_NAME)));
        return new BossSnapshot(orgId, orgLabel, character, rankLabel, level, showMembers, canManage, memberCount,
                online, onDuty, notice, noticeAuthor, noticeAt, canNotice, currency, members, ranks, candidates);
    }

    // ---- elenco aziende ----------------------------------------------------------------------------------

    public static byte[] encode(BossReply.OrgList l) {
        return write(out -> {
            uuid(out, l.character());
            list(out, l.orgs(), BossSnapshot.MAX_ORGS, o -> {
                str(out, o.id(), BossRequest.MAX_ID);
                str(out, o.label(), MAX_NAME);
            });
        });
    }

    public static BossReply.OrgList decodeOrgList(byte[] bytes) {
        return read(bytes, in -> new BossReply.OrgList(uuid(in), list(in, BossSnapshot.MAX_ORGS,
                () -> new BossSnapshot.OrgChoice(str(in, BossRequest.MAX_ID), str(in, MAX_NAME)))));
    }

    // ---- primitive ---------------------------------------------------------------------------------------

    @FunctionalInterface
    private interface Writer {
        void run(DataOutput out) throws IOException;
    }

    @FunctionalInterface
    private interface Item<T> {
        void write(T value) throws IOException;
    }

    @FunctionalInterface
    private interface Reader<T> {
        T run(DataInput in) throws IOException;
    }

    @FunctionalInterface
    private interface ItemReader<T> {
        T read() throws IOException;
    }

    private static byte[] write(Writer w) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            w.run(new DataOutputStream(bytes));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    private static <T> T read(byte[] bytes, Reader<T> r) {
        try {
            return r.run(new DataInputStream(new ByteArrayInputStream(bytes)));
        } catch (IOException e) {
            throw new IllegalArgumentException("malformed boss packet", e);
        }
    }

    private static void str(DataOutput out, String s, int max) throws IOException {
        String v = s == null ? "" : s;
        out.writeUTF(v.length() > max ? v.substring(0, max) : v);
    }

    private static String str(DataInput in, int max) throws IOException {
        String s = in.readUTF();
        if (s.length() > max) throw new IllegalArgumentException("string too long");
        return s;
    }

    private static void uuid(DataOutput out, UUID u) throws IOException {
        out.writeLong(u.getMostSignificantBits());
        out.writeLong(u.getLeastSignificantBits());
    }

    private static UUID uuid(DataInput in) throws IOException {
        return new UUID(in.readLong(), in.readLong());
    }

    private static <T> void list(DataOutput out, List<T> items, int max, Item<T> writer) throws IOException {
        int n = Math.min(items.size(), max);
        out.writeShort(n);
        for (int i = 0; i < n; i++) writer.write(items.get(i));
    }

    private static <T> List<T> list(DataInput in, int max, ItemReader<T> reader) throws IOException {
        int n = in.readUnsignedShort();
        if (n > max) throw new IllegalArgumentException("list too long");
        List<T> items = new ArrayList<>(n);
        for (int i = 0; i < n; i++) items.add(reader.read());
        return List.copyOf(items);
    }
}
