package dev.qrm.immersion.client;

import dev.qrm.immersion.logic.BodyPose;
import dev.qrm.immersion.logic.ImmersionRules;
import dev.qrm.immersion.mc.ImmersionEntities;
import dev.qrm.immersion.net.ImmersionPayloads;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.util.TriState;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

/** Parte client del Module: tasti, etichette sopra la testa, pose delle braccia. Caricata solo sul client. */
@Mod(value = "qrm_immersion", dist = Dist.CLIENT)
public final class ImmersionClient {
    private static boolean pointingHeld;

    public ImmersionClient(IEventBus modBus) {
        modBus.addListener(ImmersionClient::onRegisterKeys);
        modBus.addListener(ImmersionClient::onRegisterRenderers);
        NeoForge.EVENT_BUS.addListener(ImmersionClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(ImmersionClient::onCanRenderNameTag);
        NeoForge.EVENT_BUS.addListener(ImmersionClient::onRenderPlayer);
        NeoForge.EVENT_BUS.addListener(ImmersionClient::onRenderHand);
        NeoForge.EVENT_BUS.addListener(ImmersionClient::onEntityLeave);
        NeoForge.EVENT_BUS.addListener(ImmersionClient::onLoggedOut);
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(ImmersionKeys.CATEGORY);
        event.register(ImmersionKeys.HANDS_UP);
        event.register(ImmersionKeys.POINT);
    }

    /** Il sedile e' invisibile: nessun modello da disegnare. */
    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ImmersionEntities.SEAT.get(), NoopRenderer::new);
    }

    // ---- tasti ----

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            pointingHeld = false;
            return;
        }
        boolean free = mc.gui.screen() == null;
        while (ImmersionKeys.HANDS_UP.consumeClick()) {
            if (free) request(ImmersionPayloads.REQUEST_TOGGLE_HANDS_UP);
        }
        // Indicare: tenuto premuto. Il server decide se si puo' (non con le mani alzate, non a terra).
        boolean down = free && ImmersionKeys.POINT.isDown();
        if (down != pointingHeld) {
            pointingHeld = down;
            request(down ? ImmersionPayloads.REQUEST_POINT_START : ImmersionPayloads.REQUEST_POINT_STOP);
        }
        while (ImmersionKeys.POINT.consumeClick()) {
            // Svuota la coda dei clic: lo stato lo legge isDown().
        }
    }

    private static void request(int kind) {
        ClientPacketDistributor.sendToServer(new ImmersionPayloads.PoseRequest(kind));
    }

    // ---- etichette ----

    /**
     * Nessun nome vanilla per nessuno. Un'etichetta compare solo entro 5 blocchi, guardando la persona quasi
     * dritto in faccia e con la linea di vista libera. Chi non si e' presentato (o ha il volto coperto) e' "Sconosciuto".
     */
    private static void onCanRenderNameTag(RenderNameTagEvent.CanRender event) {
        if (!(event.getEntity() instanceof Avatar target)) return;
        event.setCanRender(TriState.FALSE);
        LocalPlayer me = Minecraft.getInstance().player;
        if (me == null || target == me || target.isInvisible() || target.isSpectator()) return;
        if (!ImmersionRules.withinLabelRange(me.distanceToSqr(target))) return;

        Vec3 look = me.getViewVector(1.0F);
        Vec3 to = target.getEyePosition().subtract(me.getEyePosition());
        if (!ImmersionRules.inViewCone(look.x, look.y, look.z, to.x, to.y, to.z)) return;
        if (!me.hasLineOfSight(target)) return;

        String known = ClientImmersion.knownName(target.getUUID());
        boolean masked = !target.getItemBySlot(EquipmentSlot.HEAD).isEmpty();
        Component label = known != null && !masked
                ? Component.literal(known).withStyle(ChatFormatting.WHITE)
                : Component.translatable("qrm_immersion.stranger", ImmersionRules.strangerCode(target.getUUID()))
                        .withStyle(ChatFormatting.GRAY);
        event.setContent(label);
        event.setCanRender(TriState.TRUE);
    }

    // ---- pose ----

    /** Prima di disegnare un avatar (giocatore o manichino) gli si impone la posa ricevuta dal server. */
    private static void onRenderPlayer(RenderPlayerEvent.Pre<?> event) {
        AvatarRenderState state = event.getRenderState();
        BodyPose pose = ClientImmersion.poseOf(state.id);
        if (pose == BodyPose.HANDS_UP) {
            state.rightArmPose = ArmPoses.handsUp();
            state.leftArmPose = ArmPoses.handsUp();
        } else if (pose == BodyPose.POINTING) {
            state.rightArmPose = ArmPoses.pointing();
        }
    }

    /** In prima persona le braccia alzate sopra le spalle stanno fuori dall'inquadratura. */
    private static void onRenderHand(RenderHandEvent event) {
        LocalPlayer me = Minecraft.getInstance().player;
        if (me != null && ClientImmersion.poseOf(me.getId()) == BodyPose.HANDS_UP) event.setCanceled(true);
    }

    private static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) ClientImmersion.forgetEntity(event.getEntity().getId());
    }

    private static void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientImmersion.reset();
        pointingHeld = false;
    }
}
