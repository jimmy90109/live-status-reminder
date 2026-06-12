# 乘車碼下車提醒

這個 Android App 會監聽 iPASS MONEY 的交易通知：

- 偵測到 `乘車碼交易` 與 `尚未出站` 後，顯示持續性的下車提醒。
- 點擊提醒通知後，開啟 iPASS MONEY App。
- 偵測到 `出站交易已完成` 後，自動移除提醒。
- Android 16 以上會要求顯示為 Live Update；舊版 Android 會顯示一般持續通知。

## 使用方式

1. 安裝並開啟 App。
2. 開啟通知存取權限，允許「乘車碼下車提醒」讀取通知。
3. Android 13 以上裝置需另外允許 App 顯示通知。
4. Android 16 以上可從首頁開啟 Live Update 設定。
5. 可先用首頁的模擬按鈕驗證提醒通知。

目前 iPASS MONEY 沒有公開乘車碼頁面的 deep link，因此點擊提醒會開啟
iPASS MONEY App 首頁。若未安裝 iPASS MONEY，會改為開啟 Google Play。

## 建置

```bash
./gradlew test assembleDebug
```
