# Google Play 上架清單

## 程式與發佈檔

- [x] `targetSdk` 符合目前 Google Play 要求。
- [x] 可成功執行單元測試、Release Lint 與 AAB 建置。
- [x] App 內在前往通知存取設定前提供醒目揭露與使用者選擇。
- [x] 停用 App 資料備份，避免未來意外備份敏感的本機資料。
- [x] 使用永久且唯一的正式 application ID：`com.github.jimmy90109.livestatus`。
- [ ] 建立並安全保存 upload keystore；用正式 upload key 簽署 AAB。
- [ ] 在實體 Android 16 裝置完整驗證三種來源通知與權限撤銷流程。

## 商店與政策

- [x] 準備繁體中文商店文案與發布說明。
- [x] 準備隱私權政策內容。
- [x] 準備資料安全與 App content 填寫建議。
- [ ] 填入公開的隱私問題聯絡電子郵件。
- [x] 隱私權政策發布位置：<https://jimmy90109.github.io/live-status-reminder/>
- [ ] 準備至少 2 張手機螢幕截圖、512×512 PNG App 圖示及 1024×500 Feature Graphic。
- [ ] 建立 Play Console App 並完成商店設定、內容分級、目標年齡、資料安全及 App 存取聲明。

## 測試與發布

- [ ] 上傳正式簽署的 AAB 至 Internal testing，處理 Pre-launch report 問題。
- [ ] 若為 2023-11-13 後建立的個人開發者帳號：完成至少 12 位測試者連續 14 天加入 Closed testing。
- [ ] 申請 Production access（若 Play Console 要求）。
- [ ] 建立 Production release、送審並確認 Google Play 已公開顯示。

## 尚需開發者確認

1. Play Console 帳號類型及建立日期。
2. 隱私問題聯絡電子郵件。
