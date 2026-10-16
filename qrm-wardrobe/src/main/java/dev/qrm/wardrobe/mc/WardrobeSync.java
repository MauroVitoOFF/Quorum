package dev.qrm.wardrobe.mc;

import dev.qrm.wardrobe.logic.OutfitCatalog;
import dev.qrm.wardrobe.logic.OutfitDefinition;
import dev.qrm.wardrobe.logic.OutfitModel;
import dev.qrm.wardrobe.logic.WardrobeWire;
import dev.qrm.wardrobe.net.WardrobePayloads;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** Manda ai client con il mod il catalogo e chi indossa che cosa. */
final class WardrobeSync implements WardrobeServer.Sync {
    @Override
    public void catalogChanged() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) sendCatalog(p);
    }

    @Override
    public void wornChanged() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) sendWorn(p);
    }

    @Override
    public void playerJoined(ServerPlayer player) {
        sendCatalog(player);
        sendWorn(player);
    }

    private static boolean hasMod(ServerPlayer p) {
        return p.connection.hasChannel(WardrobePayloads.Worn.TYPE);
    }

    static void sendCatalog(ServerPlayer p) {
        if (!hasMod(p)) return;
        OutfitCatalog catalog = WardrobeServer.service().catalog();
        List<OutfitDefinition> all = catalog.all();
        if (all.isEmpty()) {
            PacketDistributor.sendToPlayer(p, new WardrobePayloads.Catalog(0, 0, new byte[0]));
            return;
        }
        for (int i = 0; i < all.size(); i++) {
            OutfitDefinition o = all.get(i);
            byte[] entry = WardrobeWire.encode(new WardrobeWire.CatalogEntry(o.id(), o.label(), o.job(),
                    o.minGrade() == null ? "" : o.minGrade(), o.model() == OutfitModel.SLIM, o.png()));
            PacketDistributor.sendToPlayer(p, new WardrobePayloads.Catalog(all.size(), i, entry));
        }
    }

    static void sendWorn(ServerPlayer p) {
        if (!hasMod(p)) return;
        PacketDistributor.sendToPlayer(p, new WardrobePayloads.Worn(
                WardrobeWire.encodeWorn(WardrobeServer.wornByOnlinePlayer())));
    }
}
