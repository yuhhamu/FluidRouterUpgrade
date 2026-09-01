# DEVELOPMENT.md

Fluid Router Upgradeの内部設計と実装ノートです。ソースコード中に設計判断の経緯コメントを置かない方針(コメント除去・コピー元明記ポリシー)のため、本ファイルには公開API・内部構造のみを記載しています。設計判断の経緯・調査履歴はObsidian Vaultの`Knowledge/`、およびClaude Projectドキュメント(`FluidRouterUpgrade-完全移植-Filter-Regulator-JEI-2026-08-30.md`)を参照してください。

## 全体設計

`FluidRouterModeProvider`が`RouterModeProvider`(Router Upgrade Core)を実装し、Fluid Mode Upgradeが挿入されたRouterのタンクcapability・NBTセーブ&ロード・Puller/Sender/Distributor/Voidの各モジュール実行処理を提供する。Filter/Regulator Augmentは全モジュール種別(Puller/Sender/Distributor/Void)が対象(経緯はObsidian Knowledge/`fluidrouterupgrade-filter-regulator-augment-scope-fix`参照)。

## フィルタースロットの液体種別タグ

`FluidFilterTag`が、フィルタースロットに設定するアイテムスタックへ専用のNBT真偽値タグ(`FluidRouterUpgradeFluidFilter`)を立てる/読み取るユーティリティ。`ModuleMenuFilterClickMixin`がVanillaの`ModuleMenu#clicked`へ注入し、左クリック(バケツそのものの完全一致、タグ無し)と右クリック(液体種別一致、タグ付き)を区別して直接スロットへ設定する。いずれもVanillaの`isItemOKForFilter`(同一Item型の重複登録禁止チェックを含む)を経由しないため、同じ基底Item(バケツ)を複数スロットへ自由に登録できる。`FluidFilterSupportMixin`はVanillaの`ModuleItem#getFilterItemMatcher`等へ注入し、タグの有無に応じてFluidMatcher/SimpleItemMatcherを切り替える(対象はDistributorModule/VoidModule/PullerModule1/PullerModule2/SenderModule1/SenderModule2/SenderModule3の全モジュール実装。経緯はObsidian Knowledge/`fluidrouterupgrade-puller-sender-fluid-filter-matcher-fix`参照)。

## JEI連携

`FluidRouterUpgradeJeiPlugin`がGhost Ingredientハンドラー(`FluidModuleGhostIngredientHandler`/`FluidModuleGhostTarget`)を各モジュール画面へ登録し、ItemStack(バケツそのもの)のドラッグに対応する。液体(FluidStack)のドラッグについては、ModularRouters自身が同一画面に対して独自のJEI連携(`me.desht.modularrouters.integration.jei.GhostTarget`)を先に登録しており、JEIは座標が一致した最初のtargetのみacceptするため、そちらが常に先に選ばれてしまう。これに対処するため、`ModularRoutersGhostTargetFluidFixMixin`がModularRouters側の`GhostTarget#accept`のHEADへ注入し、FluidStackの場合のみ`FluidFilterTag`でタグ付けした状態でこちらから登録処理を行いキャンセルする(ItemStackの場合はModularRouters本来の処理をそのまま素通しする)。

ツールチップは`JeiFluidTooltipBridge`経由でJEI本体の`IIngredientRenderer<FluidStack>`から取得し、`IModIdHelper#getModNameForTooltip`で末尾にMod名行を追加することでJEI本体の表示内容と完全一致させている(`FluidRouterUpgradeJeiPlugin#onRuntimeAvailable`)。JEI未導入時は液体の表示名1行のみにフォールバックする。

## 送受信レーザーの配色

Vanilla本体の`BeamData`(1色のみ)に加え、実際に転送している液体の色を薄く重ねた「ハローライン」を独自RenderType(`FluidRenderTypes.HALO_LINE`、線幅18px相当・アルファ45/255程度の低視認性)で追加描画する2層構成。

- 基本色: ModularRouters本体が搬出入で使用している色をそのまま採用(旧FluidRoutersの独自色ではなくVanilla本体の色)。Puller Mk1/Mk2・Distributor受信側は`0x6080FF`(Vanilla `CompiledPullerModule2`/`CompiledDistributorModule#getBeamColor()`と同値)、Sender Mk1は`0xFFC000`(Vanilla `CompiledSenderModule1#getBeamColor()`と同値)、Sender Mk2・Distributor送信側は`0xFF8000`(Vanilla `CompiledSenderModule2#getBeamColor()`と同値)。
- ハロー色: `FluidBeamRenderer.getFluidRepresentativeColor`が、液体のtintColorと焼きテクスチャの平均色を掛け合わせて算出する(液体ごとに1度計算し`FLUID_COLOR_CACHE`にキャッシュ)。
- Sender Module Mk3(異次元送信)のみ例外で、ターゲットが別ディメンションになり得るため実際の距離ベースのビームは描画できない。Vanilla本体の`CompiledSenderModule3#playParticles`と同じく、Router正面へ1ブロックだけ伸びる紫色(`0x800080`)の短いフェードビームを表示する(ハローラインは付与しない)。
- 実際の描画は`FluidBeamRenderer`が自前で行う(Vanilla本体の`addItemBeam`には乗せない)。サーバー→クライアントの同期は`FluidBeamStartMessage`/`FluidBeamStopMessage`(`FluidRouterUpgradeMod`の`PacketHandler`経由)、および再接続時の一括同期用に`getUpdateTag`/`handleUpdateTag`経由の`FluidBeamRenderer.syncRouter()`を用いる。

## フィルタースロットの液体アイコン表示

`FluidFilterSlotRenderer`が、タグ付きスロットの液体アイコンをタイント付きで重ね描きする。`FluidFilterIconRenderMixin`がVanillaの`AbstractContainerScreen#renderSlot`(private、`remap = false`+公式名/SRG名の配列指定)のTAILへ注入し、`ModuleMenu`のフィルタースロット(index 0-8)のみを対象にこの描画を呼び出す(`slot.x`/`slot.y`をそのまま使用、深度テスト・ブレンドを一時的に無効化して描画)。ツールチップ内容は`ModuleFilterTooltipMixin`がVanillaの`renderTooltip`のHEADへ注入して差し替える(経緯はObsidian Knowledge/`fluidrouterupgrade-fluid-filter-icon-zorder`参照)。

## Mixinクラス一覧

- `FluidFilterSupportMixin` — 全モジュール種別(Puller/Sender/Distributor/Void)のFilterMatcherをタグの有無で切り替える。
- `ModuleMenuFilterClickMixin` — フィルタースロットへの左右クリック登録を直接処理する。
- `FluidFilterIconRenderMixin` — Vanilla`renderSlot`のTAILで液体アイコンを描画する。
- `ModuleFilterTooltipMixin` — Vanilla`renderTooltip`をJEI互換のツールチップへ差し替える。
- `ModularRoutersGhostTargetFluidFixMixin` — ModularRouters自身のJEI連携`GhostTarget#accept`に割り込み、FluidStackドラッグにタグを付与する。

## リログイン時のビーム消失対策

タンク内容量の復元に使っている`getUpdateTag`/`handleUpdateTag`経路に、稼働中ビームのスナップショット(`RouterTankState.activeBeams`、`FluidBeamKey`→色・液体種別)も一緒に乗せている。クライアント側は受信のたびに`FluidBeamRenderer.syncRouter(routerPos, beams)`を呼び、そのRouterに属する描画エントリをスナップショットへ完全に同期する(スナップショットに無いキーは停止、あるキーは開始または更新)。これにより接続断・再接続をまたいでも表示状態が復元される(経緯はObsidian Knowledge/`fluidrouterupgrade-relogin-beam-persistence-fix`参照)。

## fluid_mode_upgradeのテクスチャを2レイヤー化(2026-09-01)

`fluid_mode_upgrade`のアイテムモデルを、単一テクスチャから`routerupgradecore:item/mode_upgrade_core`(共通ベース、layer0)+`fluidrouterupgrade:item/fluid_mode_upgrade`(このMOD固有のアクセント差分、layer1)の2レイヤー構成に変更した。`fluid_mode_upgrade.png`自体もこれに合わせて、ベース込みの単独アイコンから、アクセント部分のみを描いた透過主体のテクスチャに差し替えている。この構成はRouterUpgradeCore側でmode_upgrade系アイテムのdefault規約として定義済み(RouterUpgradeCoreのDEVELOPMENT.md参照)。
