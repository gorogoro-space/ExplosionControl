# ExplosionControl
[![Paper 1.21.11](https://img.shields.io/badge/Paper-1.21.11-brightgreen.svg)](https://fill-ui.papermc.io/projects/paper)
[![GitHub release](https://img.shields.io/github/release/gorogoro-space/ExplosionControl.svg)](https://github.com/gorogoro-space/ExplosionControl/releases)
[![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/gorogoro-space/ExplosionControl/issues)
[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://github.com/gorogoro-space/ExplosionControl/blob/main/LICENSE)

This plugin controls block damage and entity damage caused by explosions, for each type of explosion.

爆発による地形の破壊やダメージを、爆発の種類ごとに制御するプラグインです。

# Requirements
- Paper 1.21.11 以降(1.21.11 未満のサーバーでは読み込まれません)
- Java 21

# Installation method
Please place the .jar file in the Paper plugins folder.

.jar ファイルを Paper の plugins フォルダに置き、サーバーを再起動してください。

# Features
- 爆発の種類ごとに、地形を壊すかどうかを設定できます
  - 種類: クリーパー、TNT、TNT 付きトロッコ、火の玉(ガストの火の玉・ファイアチャージ)、ウィザー(出現時の爆発とウィザーの頭)、エンドクリスタル、そのほか(ベッド・リスポーンアンカー・ほかのプラグインによる爆発など)
  - 地形を壊さない設定でも、爆発そのもの(音・煙・ダメージ)は起こります
- 爆発の種類ごとに、プレイヤー・Mob・落ちているアイテム・飾り(絵画・額縁・防具立て)へのダメージを止められます
- エンダーマンがブロックを持ち上げたり置いたりできないようにできます(既定で有効)
- 動かすワールドを指定できます
- 設定はサーバーを再起動せずに読み込み直せます

既定の設定では、どの爆発でも地形と飾りは壊れません。
エンダードラゴンとウィザーが体当たりでブロックを壊すこと、ウィンドチャージ、トロッコやボートへのダメージは、バニラのままです。

# Commands
- `/explosioncontrol reload` — config.yml を読み込み直します。`explosioncontrol.reload` の権限が必要です(コンソールからも実行できます)

# Permissions
- `explosioncontrol.reload` — `/explosioncontrol reload` を使えます。既定では OP が持っています

# Config
`plugins/ExplosionControl/config.yml`
- `worlds` — このプラグインを動かすワールドの名前の一覧。空にすると、すべてのワールドで動きます
- `explosions.<種類>.break-blocks` — その種類の爆発で地形を壊すか。種類は `creeper`・`tnt`・`tnt-minecart`・`fireball`・`wither`・`end-crystal`・`other`。既定はすべて `false`
- `explosions.<種類>.damage.player` / `mob` / `item` / `decoration` — その種類の爆発で、プレイヤー・Mob・落ちているアイテム・飾り(絵画・額縁・防具立て)にダメージを与えるか。既定では `decoration` だけ `false`
- `enderman.prevent-pickup` / `enderman.prevent-place` — `true` にすると、エンダーマンがブロックを持ち上げたり置いたりできなくなります。既定はどちらも `true`
- `language` — メッセージの言語。`en`(英語)か `ja`(日本語)。既定は `en`
- `messages.en.*` / `messages.ja.*` — 各言語のメッセージ。文は自由に書き換えられます
  - ほかの言語を使うときは、`messages` の下に `fr:` などを書き足し、`language: fr` にします。書いていない項目は英語で表示します
  - `language` の言語が `messages` に無いときは、起動時に警告を出して英語にします

config.yml が既にある場合、足りない項目は起動時に既定値で追記されます。

# Disclaimer
Do not assume any responsibility by use. Please use it at your own risk.

## ビルド

JDK 21 が必要です。Gradle はラッパー(`gradlew`)が自動でダウンロードするので、別途インストールする必要はありません。

```
gradlew.bat clean build
```

(macOS / Linux では `./gradlew clean build`)

## IntelliJ IDEA でのビルド手順

本プロジェクトはビルドツールに Gradle を使用しています。
IntelliJ IDEA 上で正しくプラグイン（JARファイル）を生成するには、以下の手順を実行してください。
「ビルド → アーティファクトのビルド」はクラスファイルが入らないことがあるので使わないでください。

### 🛠️ ビルド手順

1. IntelliJ IDEA の画面右端にある **「Gradle」タブ** をクリックして開きます。
2. プロジェクト名（ExplosionControl）を展開し、 **`Tasks`** ツリーを開きます。
3. リスト内にある **`clean`** をダブルクリックして実行します（古いビルドキャッシュを削除します）。
4. 続けてリスト内にある **`build`** をダブルクリックして実行します。

### 📦 生成されたファイルの場所
ビルドが成功すると、プロジェクトのルート直下に `build/libs` フォルダが作成（または更新）され、その中に中身の詰まった正しい JAR ファイルが生成されます。

* **生成先:** `build/libs/ExplosionControl-<version>.jar`(例: `ExplosionControl-1.0.jar`。バージョンは `gradle.properties` の `version`)

この JAR ファイルを Minecraft サーバーの `plugins` フォルダに配置してください。

## 開発(IntelliJ IDEA で Claude Code を使う)

このリポジトリには、AI コーディングツール [Claude Code](https://docs.claude.com/ja/docs/claude-code/overview) 向けの作業ルールを書いた `CLAUDE.md` があります。IntelliJ IDEA で使う手順は次のとおりです。

1. Claude の有料プラン(Pro / Max)か、Anthropic API のアカウントを用意します。
2. Windows では、先に [Git for Windows](https://git-scm.com/downloads/win) をインストールします。
3. Claude Code 本体をインストールします。Windows では PowerShell で次を実行します。
   ```
   irm https://claude.ai/install.ps1 | iex
   ```
4. IntelliJ IDEA の「設定 → プラグイン → Marketplace」で「Claude Code」を検索してインストールし、IDE を再起動します。検索結果には Anthropic 以外が作った似た名前のプラグインも表示されます。プラグイン名の下に書かれた提供元が「Anthropic」になっているものを選んでください。
5. プロジェクトを開いて、ターミナルで `claude` を実行します(`Ctrl+Esc` でも起動できます)。初回はブラウザでログインします。

起動すると `CLAUDE.md` が自動で読み込まれ、このプロジェクトの設計方針に沿って作業します。

## ライセンス

[LICENSE](LICENSE) を参照してください。

## kubotan へのメモ

リポジトリを新規作成したら、**必ず Watch を設定すること**(忘れない!)。
GitHub の自動 Watch 機能は 2025 年 5 月に廃止されたため、設定しないと他の人が立てた issue や PR の通知が届かない。

1. リポジトリのページ右上の **「Watch」** を押す
2. **「Custom」** を選び、**Issues** と **Pull requests** にチェックを入れる
