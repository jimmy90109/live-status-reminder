# Production 上架準備清單

本文件用來追蹤從 Alpha 封閉測試走到正式版公開上架的剩餘工作。

## Alpha 審查通過後

- [x] Alpha `2608022 (1.0.4)` 審查已通過並可供測試人員使用（2026-08-02 18:13）。
- [x] Alpha `2608030 (1.0.4)` 已於 2026-08-03 01:12 通過審查並可供測試人員使用。
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
| 已審查通過版本 | `2608030 (1.0.4)` |
| 最新 Alpha 上傳版本 | `2608030 (1.0.4)`，已於 2026-08-03 01:12 通過審查並可供測試人員使用 |
| 最新正式版 | `2608030 (1.0.4)` |
| Production 狀態 | `2608030 (1.0.4)` 已於 2026-08-03 01:35 通過審查並上架；Play Console 顯示為有效的最新正式版，100% 全面推出 |
| 最新正式版 AAB SHA-256 | `c6e64f1874eabdb0691a1ccd4c81c6cdccafd3375a0e4f81ebb3a330a24e6a1c` |
| Play 商店網址 | <https://play.google.com/store/apps/details?id=com.github.jimmy90109.livestatus> |
| Native debug symbols | `app/build/outputs/native-debug-symbols/release/native-debug-symbols-play.zip` |
| Native debug symbols SHA-256 | `528bb0393812360b3257a6f08eb06ab981a1138676f8bb7b40b0dcfb2c1b2b0a` |
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
| 待填 | 待填 | 台灣 Pay 提醒 | 待填 | 待填 |
| 待填 | 待填 | YouBike 2.0／2.0E 費用追蹤 | 待填 | 待填 |
| 待填 | 待填 | foodpanda 提醒 | 待填 | 待填 |
| 待填 | 待填 | 55688 乘車提醒 | 待填 | 待填 |
| 待填 | 待填 | Uber Eats 提醒 | 待填 | 待填 |

## Production access 申請素材

若 Play Console 要求申請 Production access，可使用以下內容整理回答。

### App 用途

「LiveStatus 即時狀態提醒」是一款工具型 App，將使用者已收到的 iPASS MONEY、台灣 Pay、YouBike、foodpanda、55688、Uber、Uber Eats 與 Pikmin Bloom 通知，在裝置上整理成 Android Live Update，協助使用者更容易掌握乘車、騎乘費用、外送與種花狀態。

### 測試方式

測試者會從 Google Play Alpha 測試軌安裝 App，授予 Android 通知存取權限，並驗證：

- App 會在授權前顯示醒目揭露。
- 使用者可拒絕或撤銷通知存取權限。
- iPASS MONEY 進出站通知可建立與結束乘車提醒。
- 台灣 Pay 上下車通知可建立與結束乘車提醒。
- YouBike 2.0／2.0E 借還車通知可建立與結束費用追蹤；驗證依車號辨識車種、全臺服務區域一般會員估價、嘉義補助期限、臺東專屬費率、未知站點地區 Dialog、其他地區與車號不符還車。
- YouBike 卡片可由使用者主動開啟 `SCHEDULE_EXACT_ALARM`；授權後在下一個費用變更邊界使用 exact alarm 更新，拒絕或撤銷後降級且不影響其他功能。
- YouBike 未知／同名站點經手動選區且正常還車後，會顯示可忽略的靜音回報通知；只有使用者在外部 Email App 確認寄出後，去識別化站點回報才會離開裝置。
- foodpanda 外送通知可更新外送狀態。
- 55688 叫車通知可顯示車牌、更新車輛抵達狀態，並在行程完成後清除；車牌僅在裝置上處理。
- Uber 乘車通知可更新行程狀態，乘車 PIN 僅在裝置上處理。
- Uber Eats 訂單通知可更新進度，交付 PIN 僅在裝置上處理。
- Pikmin Bloom 背景種花通知可建立提醒，來源通知消失後自動結束。

### 隱私與安全

- App 不需要帳號登入。
- App 不含廣告或分析 SDK。
- App 不上傳、出售或分享通知內容、PIN 或其他使用者資料。
- 通知內容僅在裝置上即時解析，不永久儲存。
- 使用者可隨時在 Android 系統設定撤銷通知存取權限。
- `SCHEDULE_EXACT_ALARM` 只用於已開始的 YouBike 騎乘，不使用受限的 `USE_EXACT_ALARM`，也不用於網路、聲音、廣告、分析或背景同步。
- YouBike 站點回報不自動寄送；本機去重紀錄只保存索引版本與站名雜湊，不保存站名明文。Email 不含車號、車柱、時間、通知全文或付款資料。

## 正式版前版本設定

Production release 前請確認：

- [x] 若 Alpha 後沒有程式碼變更，可沿用既有 AAB；若有任何變更，必須遞增 `versionCode`。
- [x] 若要正式發布為 `1.0.0`，將 `versionName` 從目前測試版名稱改為 `1.0.0`。
- [x] 正式版不再以 beta 名義公開，商店資訊與版本資訊已改為正式版。
- [x] 重新產生正式簽署 AAB；若簽署失敗，先依 [upload-keystore-troubleshooting-zh-TW.md](upload-keystore-troubleshooting-zh-TW.md) 排查。
- [x] 執行 `./gradlew verifyReleaseSigning test lintRelease bundleRelease`。
- [x] 確認 `app/build/outputs/bundle/release/app-release.aab` 是要上傳的最新檔案。
- [x] 若 Play Console 要求 native debug symbols，使用 `native-debug-symbols-play.zip`；不要使用根目錄含 `lib/` 的 Gradle 原始 zip。

## Production release note

`1.0.4`：

```text
1.0.4 更新：

- 媒體播放 Live Update 新增播放進度、專輯資訊及 Podcast 倒退／快轉控制。
- 媒體暫停後保留提醒 1 分鐘，可直接恢復播放。
- Pikmin Bloom 新增英文種花通知辨識與英文提醒文案。
```

## 送出 Production 前最後確認

- [ ] 商店資訊、截圖、圖示與 Feature Graphic 都是正式要公開的版本。
- [x] 隱私權政策網址可公開存取。
- [ ] 資料安全性問卷仍符合目前程式碼。
- [ ] 內容分級與目標年齡層仍符合 App 功能。
- [ ] App access instructions 可讓審查人員不用真實帳號也能測試核心流程。
- [ ] 已保存 upload keystore、alias、密碼與 Play App signing 資訊。
