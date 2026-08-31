# DEVELOPMENT.md

Fluid Router Upgradeの内部設計と実装ノートです。ソースコード中に設計判断の経緯コメントを置かない方針(コメント除去・コピー元明記ポリシー)のため、代わりに本ファイルへ集約しています。より詳細な調査履歴はClaude Projectドキュメント(`FluidRouterUpgrade-完全移植-Filter-Regulator-JEI-2026-08-30.md`)を参照してください。

## 全体設計

`FluidRouterModeProvider`が`RouterModeProvider`(Router Upgrade Core)を実装し、Fluid Mode Upgradeが挿入されたRouterのタンクcapability・NBTセーブ&ロード・Puller/Sender/Distributor/Voidの各モジュール実行処理を提供する。Filter/Regulator AugmentはDistributor/Voidのみが対象(旧FluidRoutersの実装スコープを踏襲)。

## フィルタースロットの液体種別タグ

`FluidFilterTag`が、フィルタースロットに設定するアイテムスタックへ専用のNBT真偽値タグ(`FluidRouterUpgradeFluidFilter`)を立てる/読み取るユーティリティ。`ModuleMenuFilterClickMixin`がVanillaの`ModuleMenu#clicked`へ注入し、左クリック(バケツそのものの完全一致、タグ無し)と右クリック(液体種別一致、タグ付き)を区別して直接スロットへ設定する。いずれもVanillaの`isItemOKForFilter`(同一Item型の重複登録禁止チェックを含む)を経由しないため、同じ基底Item(バケツ)を複数スロットへ自由に登録できる。`FluidFilterSupportMixin`はVanillaの`ModuleItem#getFilterItemMatcher`等へ注入し、タグの有無に応じてFluidMatcher/SimpleItemMatcherを切り替える(対象はDistributorModule/VoidModuleのみ)。

## JEI連携

`FluidRouterUpgradeJeiPlugin`がGhost Ingredientハンドラー(`FluidModuleGhostIngredientHandler`/`FluidModuleGhostTarget`)を各モジュール画面へ登録し、ItemStack(バケツそのもの)のドラッグに対応する。液体(FluidStack)のドラッグについては、ModularRouters自身が同一画面に対して独自のJEI連携(`me.desht.modularrouters.integration.jei.GhostTarget`)を先に登録しており、JEIは座標が一致した最初のtargetのみacceptするため、そちらが常に先に選ばれてしまう。これに対処するため、`ModularRoutersGhostTargetFluidFixMixin`がModularRouters側の`GhostTarget#accept`のHEADへ注入し、FluidStackの場合のみ`FluidFilterTag`でタグ付けした状態でこちらから登録処理を行いキャンセルする(ItemStackの場合はModularRouters本来の処理をそのまま素通しする)。

ツールチップは`JeiFluidTooltipBridge`経由でJEI本体の`IIngredientRenderer<FluidStack>`から取得し、`IModIdHelper#getModNameForTooltip`で末尾にMod名行を追加することでJEI本体の表示内容と完全一致させている(`FluidRouterUpgradeJeiPlugin#onRuntimeAvailable`)。JEI未導入時は液体の表示名1行のみにフォールバックする。

## フィルタースロットの液体アイコン表示

`FluidFilterSlotRenderer`が、タグ付きスロットの液体アイコンをタイント付きで重ね描きする。アイコンの奥行き(Z順)は次の理由で「Vanilla本体の`AbstractContainerScreen#renderSlot`のTAIL」から`blitOffset=300`固定・深度テストとブレンドを一時的に無効化して描画する方式に落ち着いている。

- `render()`全体のTAIL(ツールチップ描画後)から描画する方式では、アイコンが常にツールチップより手前に出るか、逆にVanilla本来のホバーハイライトを覆い隠してしまう(ハイライトはVanillaの通常描画ループ内で、アイコンより先に描かれるため)。
- `renderSlot`のTAIL(アイテムアイコン描画の直後、Vanilla本来のホバーハイライトより前)へ移すことで、ハイライトは完全に無改造のVanilla処理のまま正しい奥行きで描かれるようになる。
- この位置では`slot.x`/`slot.y`をそのまま使う(guiLeft/guiTopを加算しない)。既にpushPose/translateで平行移動済みの座標系のため。
- アイテムアイコンとの描画順が近接するため、深度テスト・ブレンドを一時的に無効化して完全不透明で上書きする(Zファイティングによる透過表示を防ぐため)。

`FluidFilterIconRenderMixin`がVanillaの`AbstractContainerScreen#renderSlot`(private、`remap = false`+公式名/SRG名の配列指定)のTAILへ注入し、`ModuleMenu`のフィルタースロット(index 0-8)のみを対象にこの描画を呼び出す。ツールチップ内容は`ModuleFilterTooltipMixin`がVanillaの`renderTooltip`のHEADへ注入して差し替える。

## Mixinクラス一覧

- `FluidFilterSupportMixin` — Distributor/VoidのFilterMatcherをタグの有無で切り替える。
- `ModuleMenuFilterClickMixin` — フィルタースロットへの左右クリック登録を直接処理する。
- `ModuleFilterFluidRenderMixin` — デバッグ用の状態ログ出力のみ(液体アイコン描画自体は`FluidFilterIconRenderMixin`が担当)。
- `FluidFilterIconRenderMixin` — Vanilla`renderSlot`のTAILで液体アイコンを描画する。
- `ModuleFilterTooltipMixin` — Vanilla`renderTooltip`をJEI互換のツールチップへ差し替える。
- `ModularRoutersGhostTargetFluidFixMixin` — ModularRouters自身のJEI連携`GhostTarget#accept`に割り込み、FluidStackドラッグにタグを付与する。
