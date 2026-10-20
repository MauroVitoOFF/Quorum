package dev.qrm.police.weapon.mc;

import dev.qrm.police.weapon.logic.AmmoBand;
import dev.qrm.police.weapon.logic.MagazineState;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

/**
 * Caricatore della pistola. Il tooltip mostra la fascia di munizioni solo a chi l'ha controllato (il personaggio del
 * client è in {@link MagazineState#knownBy()}); agli altri dice soltanto che non è controllato. Nessuna barra.
 */
public class MagazineItem extends Item {
    public MagazineItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        WeaponComponents.ensureMagazineId(stack, UUID::randomUUID);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
            Consumer<Component> builder, TooltipFlag flag) {
        MagazineState mag = stack.getOrDefault(WeaponComponents.MAGAZINE.get(), WeaponComponents.DEFAULT_MAGAZINE);
        builder.accept(tooltipLine(mag, WeaponClientHooks.characterId.get()).copy().withStyle(ChatFormatting.GRAY));
    }

    /** Riga del tooltip: fascia se il personaggio conosce il caricatore, altrimenti "Non controllato". */
    public static Component tooltipLine(MagazineState mag, Optional<String> characterId) {
        if (characterId.isPresent() && mag.isKnownBy(characterId.get())) return bandName(AmmoBand.of(mag.rounds(), mag.capacity()));
        return Component.translatable("qrm_police.magazine.unchecked");
    }

    /** Nome tradotto di una fascia di munizioni. */
    public static Component bandName(AmmoBand band) {
        return Component.translatable("qrm_police.band." + band.name().toLowerCase(Locale.ROOT));
    }
}
