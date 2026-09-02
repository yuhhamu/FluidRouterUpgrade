package com.yuuhamu.fluidrouterupgrade.logic;

/**
 * 廃止: NeoForge版ではRouterModeProvider#getCapabilityがBlockCapability(BLOCK)のみを扱う設計に
 * 再設計されたため(RouterUpgradeCore NeoForge 1.21.1移植時の変更)、Router自体をFLUID_HANDLER_ITEM
 * として公開する経路が無くなり、このラッパークラスは不要になった。
 * (device_bashのマウント制約でファイル削除ができないため、空のプレースホルダとして残置している。)
 */
final class RouterTankFluidHandlerItem {
    private RouterTankFluidHandlerItem() {
    }
}
