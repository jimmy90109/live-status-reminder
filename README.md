# 即時狀態提醒

這是一個 Android 16 App，會監聽 iPASS MONEY、foodpanda 與 Uber Eats 的通知，將重要狀態轉成持續顯示的 Live Update。

## 功能

### iPASS MONEY

- 偵測到 `乘車碼交易` 與 `尚未出站` 後顯示下車提醒。
- 點擊提醒可開啟 iPASS MONEY。
- 偵測到 `出站交易已完成` 後自動移除提醒。

### foodpanda

- 外送夥伴出發時顯示「外送中」。
- 外送夥伴接近時更新為「即將抵達」。
- 訂單送達或取消後自動移除提醒。

### Uber Eats

- 從訂單成立到外送員即將抵達，顯示五階段進度：
  1. 訂單已收到
  2. 正在準備訂單
  3. 正在取餐
  4. 正前往您所在位置
  5. 快到了
- 只從 Android 16 `shortCriticalText` 解析剛好四位數的 PIN。
- 無法可靠辨識 PIN 時不顯示，避免誤用 ETA 或訂單編號。
- 訂單送達或取消後自動移除提醒。

## 系統需求

- Android 16（API 36）以上。
- 需授予通知存取權限與通知顯示權限。
- 若要顯示為系統 Live Update，需另外允許 promoted notifications。

## 使用方式

1. 安裝並開啟 App。
2. 開啟「通知存取權限」，允許「即時狀態提醒」讀取來源 App 的通知。
3. 允許 App 顯示通知。
4. 從首頁開啟 Live Update 設定並允許 promoted notifications。
5. 在各 App 分頁使用模擬按鈕驗證狀態與進度。

點擊提醒會開啟對應 App。若尚未安裝，則前往 Google Play。iPASS MONEY 目前沒有公開乘車碼頁面的 deep link，因此只能開啟 App 首頁。

## PIN 隱私

- PIN 只保留在記憶體中，不會寫入檔案、偏好設定或正式日誌。
- 完整 PIN 僅在解鎖後顯示。
- 鎖定畫面使用不含 PIN 的公開版通知。
- 第一版只追蹤一筆 Uber Eats 訂單；新訂單會取代上一筆狀態。

## 建置與驗證

```bash
./gradlew test assembleDebug lintDebug
```

為避免 Documents 同步服務複製 Gradle 中間產物，建置輸出會放在 Gradle 使用者目錄：

```text
~/.gradle/project-builds/LiveStatusReminder/app/outputs/apk/debug/app-debug.apk
```

Uber Eats 截圖評估與實機待驗證項目位於 [`docs/ubereats-audit/`](docs/ubereats-audit/README.md)。
