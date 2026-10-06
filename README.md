# SimpleLands

Lands風の土地保護プラグイン（Spigot / Paper 1.21+、Java 21）。**ほぼ全ての操作を GUI で行えます。**

## ビルド
```
mvn package
```
`target/SimpleLands-1.0.0.jar` を `plugins/` に入れてください。

## コマンド
| コマンド | 説明 |
|---|---|
| `/lands` (`/land`, `/l`) | メインメニューを開く（全機能ここから） |
| `/lands claim` / `unclaim` | 現在のチャンクを保護 / 解除（ショートカット） |
| `/lands map` | 土地マップを開く |
| `/lands spawn` | 自分の土地へテレポート |
| `/lands reload` | 設定・データ再読込（`lands.admin`） |

## GUI でできること
- 土地の作成（名前はチャットで入力）・名前変更・削除
- 現在チャンクの保護/解除、**9×5チャンクのマップ**からクリックで保護/解除
- メンバー招待（オンラインプレイヤー一覧から選択）、招待の承認/拒否
- 役職変更（メンバー / 信頼メンバー）、オーナー譲渡、追放、脱退
- 設定フラグ切替：訪問者の建築・ドア/ボタン・チェスト・動物攻撃、PvP、モンスタースポーン、爆発、延焼
- 訪問禁止リスト（立ち入り禁止）
- スポーン地点設定、土地一覧から他の土地へテレポート
- 管理者は土地一覧で Shift+右クリックで任意の土地を削除

## 権限
- `lands.use` (デフォルト: 全員)
- `lands.admin` (デフォルト: OP) — 全土地での操作バイパス、上限無視、削除

## 設定 (`config.yml`)
`max-chunks`, `max-name-length`, `disabled-worlds`, `show-enter-title`

データは `plugins/SimpleLands/lands.yml` に保存されます。
