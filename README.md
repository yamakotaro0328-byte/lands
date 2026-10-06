# LightEco

Paper 26.3 用の軽量な経済プラグイン（Java 25）。

## ビルド方法

1. JDK 25 を入れる
2. このフォルダで実行
   - Windows: `gradlew.bat build`
   - Mac/Linux: `./gradlew build`
3. `build/libs/LightEco-1.0.0.jar` を サーバーの `plugins/` に入れて再起動

## コマンド

| コマンド | 内容 |
|---|---|
| `/menu` (`/lmenu`) | メニューアイテム（コンパス）を受け取る。右クリックでメニュー |
| `/money [名前]` `/pay <名前> <金額>` `/baltop` | お金 |
| `/gamble <coin\|slot> <金額>` | ギャンブル（メニューからも可） |
| `/l claim` | 今いるチャンクを購入（1000円〜、1つ買うごとに+500円） |
| `/l unclaim` `/l info` `/l list` `/l trust <名前>` `/l untrust <名前>` | 土地の管理 |
| `/jobs` | 職業メニュー（hunter / miner / farmer / woodcutter / fisher） |
| `/nation create <名前>` ほか | 町・国（5人以上で「国」表示） |
| `/leco give\|take\|set <名前> <金額>` `/leco reload` | 管理者用 |

## 設定

`plugins/LightEco/config.yml` で金額・土地の価格・上限などを変更できます。
データは `plugins/LightEco/data.yml` に保存されます。

## EssentialsX と併用する場合

EssentialsX にも `/balance`(`/money`) `/pay` `/baltop` `/eco` があり、名前が重なります。
Essentials の `config.yml` の `disabled-commands:` に次を追加してください。

```yaml
disabled-commands:
  - balance
  - pay
  - baltop
  - eco
```

Essentials のお金と LightEco のお金は別管理です（Vault 連携は入れていません）。
