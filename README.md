# Backpack Listener API

(english version below)

這是全網第一個提供玩家開啟背包事件功能的 Paper 插件 API。

## 功能特性

- 提供 `PlayerOpenBackpackEvent` API 事件
- 允許其他插件監聽玩家打開背包的動作
- 基於 PacketEvents 實現

## 環境需求

- Paper Mojmap 1.21.8 或更高版本
- PacketEvents 插件

## 運作原理

本插件透過監聽玩家的配方表確認(seen recipe)
封包來檢測背包開啟事件。當玩家打開背包或其他容器介面時，客戶端會向伺服器發送一個特殊的封包來確認已經看過，就是利用這個機制來判斷玩家是否開啟了背包介面。

# English Version

This is the first Paper plugin API that provides player backpack open event functionality.

## Features

- Provides `PlayerOpenBackpackEvent` API event
- Allows other plugins to listen for player backpack opening actions
- Implemented based on PacketEvents

## Requirements

- Paper Mojmap 1.21.8 or higher
- PacketEvents plugin

## How it works

This plugin detects backpack opening events by monitoring the player's seen recipe packets. When a player
opens their backpack or other container interfaces, the client sends a special packet to the server. The plugin utilizes
this mechanism to determine whether a player has opened their backpack interface.
