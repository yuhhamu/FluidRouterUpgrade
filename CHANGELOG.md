# Changelog

このプロジェクトの変更点は [Keep a Changelog](https://keepachangelog.com/ja/1.0.0/) 形式で記録し、
[Semantic Versioning](https://semver.org/lang/ja/) (バージョニングルール参照) に従う。

## [Unreleased]

### Added

- フィルタースロットの液体アイコン表示にホバーハイライトを追加。表示位置をVanilla本体の`renderSlot`直後へ移し、Vanilla標準のハイライト処理をそのまま活用するようにした。
- JEI本体のツールチップと完全一致するよう、Mod名行の表示を追加。

### Changed

- JEIドラッグ&ドロップ時の液体登録先を、ModularRouters自身が持つ競合するJEI連携ハンドラーへ割り込む方式に変更し、常に液体タグ付きで登録されるようにした。
- フィルタースロットへの左クリック登録(バケツそのものの完全一致)を、Vanillaの重複アイテム禁止チェックを経由しない直接設定方式に変更した。

### Fixed

- フィルタースロットの液体アイコンが、一部の液体でツールチップより手前に表示される問題を修正。
- 液体アイコンの描画位置がスロットからずれる問題を修正。
- 液体アイコンの下にアイテムアイコンが薄く透けて見える問題を修正。
- Puller/Senderモジュールで液体種別フィルタ(右クリック登録)を設定しても、ツールチップがバケツのアイテム名のまま表示され、ブラックリスト/ホワイトリストによる液体の判定が一切機能しない(常に転送されてしまう)問題を修正。あわせてRegulator Augment(タンク残量による閾値制御)もPuller/Senderでは常に無効化されていた問題を修正。Distributor/Voidと同様にFilter/Regulator Augmentが機能するようになった。
- Sender Module Mk3(異次元送信)がFluid Modeで一切動作しない問題を修正。ターゲットが別ディメンションの場合に常に送信失敗していた。
- Sender Module Mk3の送信レーザーが、別ディメンション側の生の座標値をそのまま描画してしまい、意図しない位置(そのディメインの0,0,0付近)まで伸びて見える問題を修正。Vanilla本体のアイテムモードと同様、Router正面へ1ブロックだけ伸びる短い紫色のビームに変更した。
- 転送レーザーの線が太すぎる問題を修正。Vanilla本体は全ビームを太さ10.0/3.0の2本のLineStateShardで描画するため、旧FluidRoutersのBeamLineThicknessMixinを移植し、両定数を2/3スケールに縮小した。

### Changed

- 送受信レーザーの色を、Modの固定イメージカラー1色から、ModularRouters本体が搬出入で使用しているのと全く同じ色に変更。Puller/Distributor受信側は`0x6080FF`、Sender Mk1は`0xFFC000`、Sender Mk2/Distributor送信側は`0xFF8000`、Sender Mk3(異次元送信)は`0x800080`。その周囲に実際に転送している液体の色を薄く重ねたハローラインを表示する(Sender Mk3を除く)。ハローライン自体の仕組み(液体の代表色算出・専用RenderType)は旧FluidRoutersから移植。
- 送受信レーザーの基本色を、ModularRouters本体のアイテム転送ビームと同一色(上記)から、旧FluidRoutersが元々使っていた独自配色へ変更。アイテム転送ビームと同じ色だと液体転送との区別がつかなかったため。Pull(Puller/Distributor受信)は`0x2060FF`、Send(Sender/Distributor送信)は`0x30C040`、Sender Mk3(異次元送信)は`0x800080`のまま変更なし。Sender Mk1/Mk2の色分けも廃止し、送信は常に単一色とした。

## [0.1.0] - 2026-08-29

### Added

- Fluid Mode Upgrade・Tank Upgradeアイテムを追加。
- Puller/Sender/Distributor/Voidモジュールの液体転送対応を追加。
- フィルタースロットによる液体種別フィルタリング、Regulator Augment対応(Distributor/Voidのみ)を追加。
- JEI Ghost Ingredient連携(ドラッグ&ドロップでの液体登録)を追加。
