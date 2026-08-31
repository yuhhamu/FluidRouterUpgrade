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

## [0.1.0] - 2026-08-29

### Added

- Fluid Mode Upgrade・Tank Upgradeアイテムを追加。
- Puller/Sender/Distributor/Voidモジュールの液体転送対応を追加。
- フィルタースロットによる液体種別フィルタリング、Regulator Augment対応(Distributor/Voidのみ)を追加。
- JEI Ghost Ingredient連携(ドラッグ&ドロップでの液体登録)を追加。
