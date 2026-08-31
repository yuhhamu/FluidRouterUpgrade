# Fluid Router Upgrade

> This mod was developed with the help of Anthropic's AI assistant "Claude".

## What does this mod do?

Fluid Router Upgrade is an addon for ModularRouters (built on the Router Upgrade Core framework) that lets a vanilla Router transfer Forge Fluids (water, lava, and any modded fluid with a bucket) instead of just items. Rather than adding a dedicated new block, it works by inserting a Fluid Mode Upgrade item into an existing Router — the Router's Puller, Sender, Distributor, and Void modules then operate in fluid-transfer mode. Filter slots distinguish between an exact-item match (left-click a bucket) and a fluid-type match (right-click a bucket, or drag a fluid from JEI); a fluid-type filter displays the actual fluid's texture and color and, with JEI installed, shows the exact same tooltip JEI itself would show. Puller, Sender, Distributor, and Void modules all support Filter Augments (fluid-type blacklist/whitelist filtering) and Regulator Augments for threshold-based fluid retention. This is a separate mod from the older, now-frozen "FluidRouters" mod.

## Requirements

- Minecraft: 1.20.1
- ModLoader: Forge 47.4.6+
- Dependencies: Router Upgrade Core (required), Modular Routers (required), Just Enough Items / JEI (optional)

## Installation

1. Install Forge.
2. Place the dependency mods above into your `mods` folder.
3. Place this mod's jar file into your `mods` folder.

## Known Limitations

- This mod does not read or migrate data from the older "FluidRouters" mod; it is a separate, independent addon.

## Credits

- Developed by yuuhamu

## License

This mod is licensed under MIT. See [LICENSE](https://github.com/yuhhamu/FluidRouterUpgrade/blob/main/LICENSE).

Copyright (c) 2026 yuuhamu

---

# Fluid Router Upgrade

> 本MODの開発にはAnthropicのAIアシスタント「Claude」を活用しています。

## これは何をするMODですか?

Fluid Router Upgradeは、ModularRoutersのRouterへFluid Mode Upgradeアイテムを挿入するだけで、そのRouterがForge Fluid(水・溶岩、およびバケツを持つあらゆる導入Mod由来の液体)を転送できるようにするアドオンMODです(Router Upgrade Coreフレームワークを基盤とする)。専用の新規ブロックは追加せず、既存のRouterのPuller・Sender・Distributor・Voidモジュールが液体転送モードで動作するようになります。フィルタースロットは「アイテムとしての完全一致」(バケツを左クリック)と「液体種別での一致」(バケツを右クリック、またはJEIから液体をドラッグ&ドロップ)を区別でき、液体種別フィルタは実際の液体のテクスチャと色でアイコン表示され、JEI導入時はJEI本体と同一のツールチップが表示されます。Puller・Sender・Distributor・Voidの全モジュールがFilter Augment(液体種別のブラックリスト/ホワイトリストフィルタリング)・Regulator Augmentによるタンク残量ベースの閾値制御に対応しています。旧来の(現在は凍結中の)「FluidRouters」MODとは別系統の独立したMODです。

## 必要なもの

- Minecraft: 1.20.1
- ModLoader: Forge 47.4.6以降
- 依存Mod: Router Upgrade Core(必須)、Modular Routers(必須)、Just Enough Items/JEI(任意)

## 導入方法

1. Forgeを導入する。
2. 上記の依存Modを`mods`フォルダに配置する。
3. 本MODのjarファイルを`mods`フォルダに配置する。

## 既知の制限

- 旧「FluidRouters」MODのデータを引き継いだり移行したりする機能はありません。

## クレジット

- 開発: yuuhamu

## ライセンス

本MODは MIT ライセンスの下で公開されています。詳細は [LICENSE](https://github.com/yuhhamu/FluidRouterUpgrade/blob/main/LICENSE) を参照してください。

Copyright (c) 2026 yuuhamu
