# 封閉測試 Alpha 驗證清單

已審查通過版本：`2607090 (1.0.0-beta)`

最新 Alpha 上傳版本：`2607101 (1.0.0-beta3)`

Alpha 審查狀態：已通過（2026-07-10）

## 審查通過後立即確認

- [x] Play Console Alpha 審查已通過。
- [x] Play Console 已上傳 `2607101 (1.0.0-beta3)` 至 Alpha。
- [ ] Play Console 顯示最新 Alpha 版本已可供測試人員安裝。
- [ ] 測試者 Google 帳號已加入 `livestatus--app@googlegroups.com`。
- [ ] 測試者已開啟測試邀請連結並選擇加入測試。
- [ ] 測試裝置可從 Google Play 安裝「LiveStatus 即時狀態提醒」。
- [ ] 安裝來源顯示為 Google Play，而不是本機 sideload。

## 首次啟動與權限流程

- [ ] 首次開啟 App 可正常顯示首頁。
- [ ] 點選通知存取設定前，App 有顯示醒目揭露。
- [ ] 使用者可選擇取消，不會被強迫授權。
- [ ] 使用者同意後會前往 Android 通知存取設定。
- [ ] 授權通知存取後，回到 App 可正常辨識權限狀態。
- [ ] 撤銷通知存取後，App 可正常回到未授權狀態，不閃退。

## 核心功能驗證

- [ ] Google 時鐘開始倒數時可建立提醒，且剩餘時間與來源一致。
- [ ] 暫停、繼續與加一分鐘後可正確更新；倒數結束或來源通知移除後可清除。
- [ ] 同時存在多個計時器時只顯示來源指定的主要倒數。
- [ ] Android 16 chronometer 與 Android 17 MetricStyle 路徑皆完成實機驗證。
- [ ] iPASS MONEY 進站通知可建立乘車提醒。
- [ ] iPASS MONEY 出站通知可結束乘車提醒。
- [ ] foodpanda 外送中通知可建立外送提醒。
- [ ] foodpanda 即將抵達通知可更新狀態。
- [ ] foodpanda 送達或取消後可結束提醒。
- [ ] Uber 乘車進度通知可建立提醒。
- [ ] Uber 快抵達、已抵達、已上車等狀態可更新提醒。
- [ ] Uber 通知包含 PIN 時，App 僅在裝置上顯示，不上傳或永久儲存。
- [ ] Uber Eats 訂單進度通知可建立提醒。
- [ ] Uber Eats 取餐、配送、即將抵達等狀態可更新提醒。
- [ ] Uber Eats 通知包含交付 PIN 時，App 僅在裝置上顯示，不上傳或永久儲存。
- [ ] 點擊提醒可開啟對應 App；未安裝時可前往 Google Play。

## 隱私與資料安全驗證

- [ ] App 沒有要求帳號登入。
- [ ] App 沒有廣告或分析 SDK 行為。
- [ ] App 沒有傳送通知內容、PIN 或使用者資料到外部伺服器。
- [ ] 隱私權政策連結可正常開啟：<https://jimmy90109.github.io/live-status-reminder/>。
- [ ] Play Console「資料安全性」內容仍符合目前實作：不收集、不分享使用者資料。

## Play Console 觀察項目

- [ ] Pre-launch report 無重大 crash、ANR 或政策警告。
- [ ] Android vitals 無新增重大問題。
- [ ] 測試人員可成功安裝與啟動。
- [ ] 若 Play Console 要求 12 位測試者連續 14 天測試，記錄開始日期與結束日期。

## 正式版前最後檢查

- [ ] 若有任何程式碼變更，遞增 `versionCode`。
- [ ] 若已不再是測試版，將 `versionName` 從目前測試版名稱調整為正式版本名稱。
- [ ] 更新商店版本資訊。
- [ ] 重新執行 `./gradlew test lintRelease bundleRelease`。
- [ ] 使用正式簽署 AAB 建立 Production release。
