# 即時狀態提醒

這是一個 Android 16 App，會監聽 Google 時鐘、iPASS MONEY、foodpanda、Uber、Uber Eats 與 Pikmin Bloom 的通知，將重要狀態轉成持續顯示的 Live Update。

## 功能

### Google 時鐘

- 將 `com.google.android.deskclock` 的主要倒數計時器同步成 Live Update。
- 支援運行、暫停、繼續與加一分鐘造成的時間更新；倒數結束或來源通知移除後自動清除。
- Android 17 優先讀取 `MetricStyle` timer，Android 16 則使用通知 chronometer；不解析畫面文字猜測剩餘時間。
- 點擊提醒可開啟原始 Clock 計時器，但不複製暫停或加一分鐘操作按鈕。

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

### Uber

- 司機接單後顯示預估上車時間與上車點。
- 快抵達或已抵達時顯示車牌、車款與四位數 PIN。
- 上車後顯示預估下車時間與下車點。
- 偵測到評分通知後自動移除提醒。
- 一般 Uber 行程目前支援英文通知文案。
- 優步小黃支援繁體中文的「職業駕駛正在途中」、「已在附近」與「即將抵達」三階段；
  評分通知出現後自動移除提醒。
- 尚未觀察到的優步小黃已抵達、行程中與取消通知不會自行推測狀態。

### Pikmin Bloom

- 偵測到「正在背景執行時種花」後，立即顯示種花提醒。
- 點擊提醒可開啟 Pikmin Bloom。
- 原始種花通知移除或不再符合種花狀態後，自動移除提醒。

## 系統需求

- Android 16（API 36）以上。
- 需授予通知存取權限與通知顯示權限。
- 若要顯示為系統 Live Update，裝置系統也需允許第三方 App 顯示 promoted notifications。

## 使用方式

1. 安裝並開啟 App。
2. 開啟「通知存取權限」，允許「即時狀態提醒」讀取來源 App 的通知。
3. 允許 App 顯示通知。
4. 在各 App 分頁使用模擬按鈕驗證狀態與進度。

Samsung One UI 8 若無法顯示在 Now Bar，可參考 GitHub Pages 的
[Samsung Now Bar 疑難排解](https://jimmy90109.github.io/live-status-reminder/samsung-now-bar.html)。

點擊提醒會開啟對應 App。若尚未安裝，則前往 Google Play。iPASS MONEY 目前沒有公開乘車碼頁面的 deep link，因此只能開啟 App 首頁。

## PIN 隱私

- PIN 只保留在記憶體中，不會寫入檔案、偏好設定或正式日誌。
- 完整 PIN 僅在解鎖後顯示。
- 鎖定畫面使用不含 PIN 的公開版通知。
- 第一版分別只追蹤一筆 Uber 行程與一筆 Uber Eats 訂單；新狀態會取代上一筆狀態。
- Google 時鐘只鏡像來源通知指定的主要倒數計時器。

## 建置與驗證

```bash
./gradlew test assembleDebug lintDebug
```

為避免 Documents 同步服務複製 Gradle 中間產物，建置輸出會放在 Gradle 使用者目錄：

```text
~/.gradle/project-builds/LiveStatusReminder/app/outputs/apk/debug/app-debug.apk
```

Uber 與 Uber Eats 的截圖評估及實機待驗證項目分別位於
[`docs/uber-audit/`](docs/uber-audit/README.md) 與
[`docs/ubereats-audit/`](docs/ubereats-audit/README.md)。
