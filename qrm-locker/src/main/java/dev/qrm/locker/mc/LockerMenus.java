package dev.qrm.locker.mc;

import dev.qrm.api.QRM;
import dev.qrm.api.character.CharacterService;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.data.ModDataService;
import dev.qrm.api.id.CharacterId;
import dev.qrm.api.id.PlayerId;
import dev.qrm.api.point.InteractionPoint;
import dev.qrm.api.point.PointService;
import dev.qrm.api.point.PointUse;
import dev.qrm.locker.logic.LockerFlow;
import dev.qrm.locker.logic.LockerWorld;
import dev.qrm.locker.logic.ModDataLockerStorage;
import dev.qrm.locker.net.LockerSkinPayload;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** Apre l'armadietto di un Character sopra un baule vanilla. */
public final class LockerMenus {
    private static LockerFlow flow;

    private LockerMenus() {}

    /** Il flusso del mondo corrente (creato al primo uso, scartato a fine mondo con {@link #reset()}). */
    public static synchronized LockerFlow flow() {
        if (flow == null) {
            PointService points = QRM.get(PointService.class);
            CharacterService characters = QRM.get(CharacterService.class);
            flow = new LockerFlow(new ModDataLockerStorage(QRM.get(ModDataService.class)), new LockerWorld() {
                @Override
                public Optional<InteractionPoint> point(int id) {
                    return points.find(id);
                }

                @Override
                public boolean canUse(CharacterId character, InteractionPoint point) {
                    return points.canUse(character, point);
                }

                @Override
                public boolean isActive(PlayerId player, CharacterId character) {
                    return characters.activeOf(player).map(PlayerCharacter::id).filter(character::equals).isPresent();
                }
            });
        }
        return flow;
    }

    public static synchronized void reset() {
        flow = null;
    }

    public static void open(PointUse use) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        ServerPlayer player = server.getPlayerList().getPlayer(use.player().value());
        if (player == null) return;

        LockerFlow f = flow();
        LockerFlow.OpenResult result = f.open(use.player(), use.character(), use.point());
        if (!result.ok()) {
            if (result.failure() == LockerFlow.Failure.CORRUPT) {
                player.sendSystemMessage(Component.translatableWithFallback("qrm_locker.corrupt",
                        "Your locker cannot be opened right now. Contact the staff."));
            }
            return;
        }
        LockerFlow.Session session = result.session();
        LockerContainer container = new LockerContainer(session);
        LockerItems items = new LockerItems(server.registryAccess());
        items.load(session.loaded(), container);
        session.bind(() -> items.snapshot(container));

        if (player.connection.hasChannel(LockerSkinPayload.TYPE)) {
            PacketDistributor.sendToPlayer(player, new LockerSkinPayload());
        }
        Component title = use.point().name() != null ? Component.literal(use.point().name())
                : Component.translatableWithFallback("qrm_locker.point.locker", "Locker");
        var opened = player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new LockerMenu(id, inventory, container, () -> f.close(session)), title));
        if (opened.isEmpty()) f.close(session);
    }
}
