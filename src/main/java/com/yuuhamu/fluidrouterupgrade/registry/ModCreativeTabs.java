package com.yuuhamu.fluidrouterupgrade.registry;

import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/*
 * 1.19.2向け移植メモ(2026-09-01): 1.19.2にはCreativeModeTab.Builder#displayItems()が存在しない。
 * さらにFLUID_MODE_UPGRADE等はModularRouters本体のUpgradeItem/AugmentItemを継承しており、
 * Item.Properties自体はModularRouters側のコンストラクタで固定されている(こちら側でtab()を
 * 指定できない)ため、1.20.1のdisplayItems()と同じ「アイテム自身のホームタブに関わらず
 * 明示的にこのタブへ追加する」効果を、CreativeModeTab#fillItemList(NonNullList)の
 * オーバーライドで再現する。
 */
public class ModCreativeTabs {

    public static final CreativeModeTab FLUID_ROUTER_UPGRADE_TAB = new CreativeModeTab(CreativeModeTab.TABS.length, "fluidrouterupgrade") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(ModItems.FLUID_MODE_UPGRADE.get());
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("itemGroup.fluidrouterupgrade");
        }

        @Override
        public void fillItemList(NonNullList<ItemStack> items) {
            items.add(new ItemStack(ModItems.FLUID_MODE_UPGRADE.get()));
            items.add(new ItemStack(ModItems.TANK_UPGRADE.get()));
            items.add(new ItemStack(ModItems.BALANCER_AUGMENT.get()));
        }
    };
}
