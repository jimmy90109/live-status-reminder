# Production 上架準備清單

本文件用來追蹤從 Alpha 封閉測試走到正式版公開上架的剩餘工作。

## Alpha 審查通過後

- [ ] 確認 Play Console 顯示 `2607090 (1.0.0-beta)` 已可供 Alpha 測試人員安裝。
- [ ] 使用測試者帳號從 Google Play 安裝，而不是使用本機 ADB 或 sideload。
- [ ] 完成 [Alpha 驗證清單](alpha-testing-checklist-zh-TW.md)。
- [ ] 檢查 Pre-launch report，若有 crash、ANR、無障礙或政策警告，先修正再進正式版。
- [ ] 記錄 Android vitals 是否有重大問題。

## 測試者與測試期紀錄

若 Play Console 要求完成封閉測試期，請在這裡紀錄證據。

| 項目 | 紀錄 |
| --- | --- |
| 測試群組 | `livestatus--app@googlegroups.com` |
| 測試版本 | `2607090 (1.0.0-beta)` |
| 測試開始日期 | 待填 |
| 測試結束日期 | 待填 |
| 有效測試者數量 | 待填 |
| 主要測試裝置／Android 版本 | 待填 |
| Pre-launch report 結果 | 待填 |
| Android vitals 結果 | 待填 |
| 已知問題 | 待填 |
| 修正版本 | 待填 |

建議每天或每次測試後補充：

| 日期 | 測試者／裝置 | 測試項目 | 結果 | 備註 |
| --- | --- | --- | --- | --- |
| 待填 | 待填 | 通知存取流程 | 待填 | 待填 |
| 待填 | 待填 | iPASS MONEY 提醒 | 待填 | 待填 |
| 待填 | 待填 | foodpanda 提醒 | 待填 | 待填 |
| 待填 | 待填 | Uber Eats 提醒 | 待填 | 待填 |

## Production access 申請素材

若 Play Console 要求申請 Production access，可使用以下內容整理回答。

### App 用途

「LiveStatus 即時狀態提醒」是一款工具型 App，將使用者已收到的 iPASS MONEY、foodpanda 與 Uber Eats 通知，在裝置上整理成 Android Live Update，協助使用者更容易掌握乘車與外送狀態。

### 測試方式

測試者會從 Google Play Alpha 測試軌安裝 App，授予 Android 通知存取權限，並驗證：

- App 會在授權前顯示醒目揭露。
- 使用者可拒絕或撤銷通知存取權限。
- iPASS MONEY 進出站通知可建立與結束乘車提醒。
- foodpanda 外送通知可更新外送狀態。
- Uber Eats 訂單通知可更新進度，交付 PIN 僅在裝置上處理。

### 隱私與安全

- App 不需要帳號登入。
- App 不含廣告或分析 SDK。
- App 不上傳、出售或分享通知內容、交付 PIN 或其他使用者資料。
- 通知內容僅在裝置上即時解析，不永久儲存。
- 使用者可隨時在 Android 系統設定撤銷通知存取權限。

## 正式版前版本設定

Production release 前請確認：

- [ ] 若 Alpha 後沒有程式碼變更，可沿用既有 AAB；若有任何變更，必須遞增 `versionCode`。
- [ ] 若要正式發布為 `1.0.0`，將 `versionName` 從 `1.0.0-beta` 改為 `1.0.0`。
- [ ] 若要先以 beta 名義公開，確認商店資訊與版本資訊仍標示為 beta。
- [ ] 重新產生正式簽署 AAB。
- [ ] 執行 `./gradlew test lintRelease bundleRelease`。
- [ ] 確認 `app/build/outputs/bundle/release/app-release.aab` 是要上傳的最新檔案。

## Production release note 草稿

若正式版使用 `1.0.0`，可貼：

```text
1.0.0 首個正式版本：

- 支援 iPASS MONEY 乘車進出站提醒。
- 支援 foodpanda 外送進度提醒。
- 支援 Uber Eats 訂單進度與交付 PIN 顯示。
- 支援 Android 16 Live Update。
- 通知內容僅在裝置上處理，不上傳或分享。
```

## 送出 Production 前最後確認

- [ ] 商店資訊、截圖、圖示與 Feature Graphic 都是正式要公開的版本。
- [ ] 隱私權政策網址可公開存取。
- [ ] 資料安全性問卷仍符合目前程式碼。
- [ ] 內容分級與目標年齡層仍符合 App 功能。
- [ ] App access instructions 可讓審查人員不用真實帳號也能測試核心流程。
- [ ] 已保存 upload keystore、alias、密碼與 Play App signing 資訊。
