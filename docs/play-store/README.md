# Google Play 上架清單

## 程式與發佈檔

- [x] `targetSdk` 符合目前 Google Play 要求。
- [x] 可成功執行單元測試、Release Lint 與 AAB 建置。
- [x] App 內在前往通知存取設定前提供醒目揭露與使用者選擇。
- [x] 停用 App 資料備份，避免未來意外備份敏感的本機資料。
- [x] 使用永久且唯一的正式 application ID：`com.github.jimmy90109.livestatus`。
- [ ] 確認 upload keystore 已安全備份，並修正本機 `keystore.properties`，讓 `./gradlew verifyReleaseSigning bundleRelease` 可重新產生正式簽署 AAB。
- [ ] 在實體 Android 16 裝置完整驗證三種來源通知與權限撤銷流程。

## 商店與政策

- [x] 準備繁體中文商店文案與發布說明。
- [x] 準備隱私權政策內容。
- [x] 準備資料安全與 App content 填寫建議。
- [ ] 填入公開的隱私問題聯絡電子郵件。
- [x] 隱私權政策發布位置：<https://jimmy90109.github.io/live-status-reminder/>
- [x] 建立 Play Console App 並完成商店設定、內容分級、目標年齡、資料安全及 App 存取聲明。
- [x] 預設商店資訊語言：繁體中文（zh-TW）。
- [x] 應用程式類別：工具應用程式。

## 測試與發布

- [x] 上傳正式簽署的 AAB 至封閉測試 Alpha。
- [x] Alpha 版本：`2607090 (1.0.0-beta)`。
- [x] Alpha 國家／地區：台灣。
- [x] Alpha 測試群組：`livestatus--app@googlegroups.com`。
- [x] 準備 Alpha 實測清單：[alpha-testing-checklist-zh-TW.md](alpha-testing-checklist-zh-TW.md)。
- [x] 準備正式版上架準備清單：[production-readiness-zh-TW.md](production-readiness-zh-TW.md)。
- [ ] 等待 Alpha 審查結果，處理 Play Console 回報與 Pre-launch report 問題。
- [ ] 使用測試帳號從 Google Play 安裝 Alpha 版並驗證通知存取流程。
- [ ] 若為 2023-11-13 後建立的個人開發者帳號：完成至少 12 位測試者連續 14 天加入 Closed testing。
- [ ] 申請 Production access（若 Play Console 要求）。
- [ ] 建立 Production release、送審並確認 Google Play 已公開顯示。

## 尚需開發者確認

1. Play Console 帳號類型及建立日期。
2. 隱私問題聯絡電子郵件。

## 目前 Play Console 設定紀錄

- 發布軌道：封閉測試 - Alpha。
- 推出狀態：開始全面推出。
- 應用程式名稱：LiveStatus 即時狀態提醒。
- Package name：`com.github.jimmy90109.livestatus`。
- 目標年齡層：13 歲以上。
- 隱私權政策：<https://jimmy90109.github.io/live-status-reminder/>。
- 廣告聲明：無廣告。
- 資料安全性：不收集或分享使用者資料；通知內容僅在裝置上處理。
- App 存取權：不需特殊帳號即可使用完整功能；審查人員需授予通知存取權限以測試核心功能。
