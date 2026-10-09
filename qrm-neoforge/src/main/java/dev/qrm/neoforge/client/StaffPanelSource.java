package dev.qrm.neoforge.client;

import dev.qrm.client.api.staff.StaffEntries;
import dev.qrm.client.api.staff.StaffPanelModel;
import dev.qrm.client.api.staff.StaffPlayerActions;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;

/** Alimenta {@link StaffPanelModel} con registri, permessi, ultimo snapshot e bersaglio del client. */
final class StaffPanelSource implements StaffPanelModel.Source {
    @Override
    public List<StaffEntries.Entry> entries() {
        return StaffEntries.all();
    }

    @Override
    public List<StaffPlayerActions.Action> playerActions() {
        return StaffPlayerActions.all();
    }

    @Override
    public Optional<List<StaffPanelModel.PlayerInfo>> players() {
        return PanelData.snapshot().map(s -> s.players().stream()
                .map(p -> new StaffPanelModel.PlayerInfo(p.id(), p.name(), p.character().orElse(""),
                        p.job().orElse(""), p.organization().orElse(""), p.standing().muteUntil(),
                        p.standing().warnings(), p.standing().notes()))
                .toList());
    }

    @Override
    public boolean allows(String node) {
        return ClientStaffAccess.allows(node);
    }

    @Override
    public boolean hasTarget() {
        return StaffTarget.effective(Minecraft.getInstance()).isPresent();
    }
}
