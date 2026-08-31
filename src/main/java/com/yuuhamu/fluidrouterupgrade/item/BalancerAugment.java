package com.yuuhamu.fluidrouterupgrade.item;

import me.desht.modularrouters.item.augment.AugmentItem;
import me.desht.modularrouters.item.module.DistributorModule;
import me.desht.modularrouters.item.module.ModuleItem;

public class BalancerAugment extends AugmentItem {

    @Override
    public int getMaxAugments(ModuleItem moduleType) {
        return moduleType instanceof DistributorModule ? 1 : 0;
    }
}

