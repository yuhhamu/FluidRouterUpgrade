# Fluid Router Upgrade

> 本MODの開発にはAnthropicのAIアシスタント「Claude」を活用しています。

## これは何をするMODですか?

Fluid Router Upgradeは、ModularRoutersのRouterへFluid Mode Upgradeアイテムを挿入するだけで、そのRouterがアイテムだけでなくForge Fluid(水・溶岩・バケツを持つあらゆる導入Mod由来の液体)を転送できるようにするアドオンMODです。専用の新規ブロックは追加せず、既存のRouterのPuller・Sender・Distributor・Voidモジュールが液体転送モードで動作するようになります。旧来の(現在は凍結中の)「FluidRouters」MODとは別系統の独立したMODです。

## 必要なもの

- Minecraft: 1.20.1
- ModLoader: Forge 47.4.6以降
- 依存Mod:
  - Router Upgrade Core — 必須
  - Modular Routers — 必須
  - Just Enough Items (JEI) — 任意(ドラッグ&ドロップでの液体登録、JEI本体と同一のツールチップに対応)

## 導入方法

1. Forgeを導入する。
2. Modular Routers・Router Upgrade Coreを`mods`フォルダに配置する。
3. 本MODのjarファイルを`mods`フォルダに配置する。
4. 必要に応じてJEIも配置する。

## 使い方

Fluid Mode UpgradeをRouterへ挿入すると、そのRouterが液体転送モードで動作します。Tank Upgradeを追加すると内蔵タンクの容量を拡張できます。

フィルタースロットへバケツを左クリックで設置すると「バケツそのものの完全一致」フィルタに、右クリックで設置する(またはJEIから液体をドラッグ&ドロップする)と「液体種別」フィルタになります。液体種別フィルタは実際の液体のテクスチャと色でアイコン表示され、ホバー時にはJEI本体と同一のツールチップが表示されます(JEI未導入時は液体の表示名のみ)。

Distributor・VoidモジュールはRegulator Augmentによるタンク残量ベースの閾値制御に対応しています。

## 既知の制限

- Filter・Regulator Augmentの対応は現時点ではDistributor・Voidモジュールのみで、Puller・Senderには適用されません(元のModularRoutersの設計スコープを踏襲した意図的な仕様です)。
- 旧「FluidRouters」MODのデータを引き継いだり移行したりする機能はありません。完全に独立した別のMODです。

## クレジット

- 開発: yuuhamu

## ライセンス

本MODは MIT ライセンスの下で公開されています。詳細は [LICENSE](./LICENSE) を参照してください。

Copyright (c) 2026 yuuhamu
