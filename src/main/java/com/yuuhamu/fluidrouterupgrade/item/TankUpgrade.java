package com.yuuhamu.fluidrouterupgrade.item;

import me.desht.modularrouters.item.upgrade.UpgradeItem;

public class TankUpgrade extends UpgradeItem {

    public static final int MAX_COUNT = 56;

    @Override
    public int getStackLimit(int slot) {
        return MAX_COUNT;
    }
}
