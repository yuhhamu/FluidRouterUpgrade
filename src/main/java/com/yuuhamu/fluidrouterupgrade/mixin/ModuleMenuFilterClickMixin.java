package com.yuuhamu.fluidrouterupgrade.mixin;

import com.yuuhamu.fluidrouterupgrade.logic.FluidFilterTag;
import me.desht.modularrouters.container.FilterSlot;
import me.desht.modularrouters.container.ModuleMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ModuleMenu.class, remap = false)
public abstract class ModuleMenuFilterClickMixin {

    @Inject(method = "m_150399_", at = @At("HEAD"), cancellable = true)
    private void fluidrouterupgrade$onFilterSlotClick(int slotId, int dragType, ClickType clickType, Player player, CallbackInfo ci) {
        if (clickType != ClickType.PICKUP || slotId < 0 || slotId >= 9 || (dragType != 0 && dragType != 1)) {
            return;
        }
        ModuleMenu self = (ModuleMenu) (Object) this;
        ItemStack carried = self.getCarried();
        if (carried.isEmpty() || !FluidFilterTag.isEligibleFluidContainer(carried)) {
            return;
        }
        Slot slot = self.getSlot(slotId);
        if (!(slot instanceof FilterSlot)) {
            return;
        }
        ItemStack toPlace = dragType == 1
                ? FluidFilterTag.createFluidFilterStack(carried)
                : ItemHandlerHelper.copyStackWithSize(carried, 1);
        slot.set(toPlace);
        ci.cancel();
    }
}

