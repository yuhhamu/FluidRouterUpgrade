package com.yuuhamu.fluidrouterupgrade.item;

import me.desht.modularrouters.item.upgrade.UpgradeItem;

public class FluidModeUpgrade extends UpgradeItem {

    public static final int MAX_COUNT = 1;

    @Override
    public int getInstalledStackLimit() {
        return MAX_COUNT;
    }
}
