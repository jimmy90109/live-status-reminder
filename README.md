# 即時狀態提醒

這是一個 Android 16 App，會監聽 Google 時鐘、iPASS MONEY、台灣 Pay、YouBike、foodpanda、Uber、Uber Eats 與 Pikmin Bloom 的通知，將重要狀態轉成持續顯示的 Live Update。

## 功能

### Google 時鐘

- 當 Google 時鐘原生 Live Update 未生效時，將 `com.google.android.deskclock` 的主要倒數計時器同步成備援 Live Update。
- 若來源通知已被系統提升為 Live Update，則不建立第二則提醒；若先前已有鏡像提醒也會自動清除。
- 支援運行、暫停、繼續與加一分鐘造成的時間更新；倒數結束或來源通知移除後自動清除。
- Android 17 優先讀取 `MetricStyle` timer，Android 16 則使用通知 chronometer；不解析畫面文字猜測剩餘時間。
- 點擊提醒可開啟原始 Clock 計時器，但不複製暫停或加一分鐘操作按鈕。

### iPASS MONEY

- 偵測到 `乘車碼交易` 與 `尚未出站` 後顯示下車提醒。
- 點擊提醒可開啟 iPASS MONEY。
- 偵測到 `出站交易已完成` 後自動移除提醒。

### 台灣 Pay

- 偵測到「[站名] 上車通知」後顯示乘車提醒，文案與 iPASS MONEY 的乘車碼捷徑一致。
- 點擊提醒可開啟台灣行動支付，準備出示乘車碼。
- 偵測到「[站名] 下車通知」後自動移除提醒。
- Debug build 另會在程序記憶體保留最近 30 筆原始通知 payload，供後續文案校正。

### YouBike 2.0／2.0E

- 偵測官方「借車成功」通知後，顯示騎乘時間、目前預估費用與下一次費用變更時間；收到相同車號的「還車扣款成功」通知後自動移除。
- 支援目前全臺 YouBike 服務區域的 YouBike 2.0／2.0E 一般會員費率，包含嘉義縣市、臺南、高雄、屏東與臺東的地區費率及地方政府騎乘補助。
- 臺東採 2.0 `12/24/48`、2.0E `25/50` 費率；嘉義縣市補助依借車日期套用至 2026-12-31，之後自動恢復原價。
- 車種由官方通知中的七位數車號在本機判斷；第三碼為 `6` 或 `9` 時視為 2.0E，無法符合此規則時以一般 2.0 估算。
- 不包含 TPASS、敬老／愛心／學生、臺南市民卡等特殊身分或票卡、轉乘優惠及跨區調度費，實際金額以 YouBike 官方結果為準。
- 費率最後查核日期為 2026-08-01；地方政府補助異動後需隨 App 更新。
- 站點服務區域優先使用開發時由 TDX 產生的內建索引判斷；無法判斷時才會在提醒提供地區選擇。
- App 不連線查詢站點。只在本機保存目前騎乘所需的時間、站名、車柱、車號與服務區域，最長 24 小時；不保存原始通知或付款識別碼。
- 若使用者從 YouBike 卡片允許「鬧鐘與提醒」，App 會使用 exact alarm 在下一個費用變更邊界盡可能準時更新；未授權時仍可使用，但 Doze 期間金額可能延後更新。
- 滑除 YouBike Live Update 只會隱藏本次追蹤並停止後續費用更新，不會視為還車；可在 90 秒內點擊靜音通知恢復追蹤，未恢復時仍會在收到相同車號的還車通知後結束騎乘。下一趟借車會自動恢復正常顯示。
- Exact alarm 只重新計算本機通知，不連網、不播放聲音、不顯示鬧鐘，也不用於分析或背景同步。裝置強制停止 App 或廠牌極端省電時仍可能延遲。
- 若站點原本無法辨識或有同名候選，使用者手動選擇支援地區並正常還車後，App 會顯示一則可忽略的靜音回報通知。只有點擊「寄送回報」並在 Email App 確認寄出後，預填的站名、選擇地區、辨識類型、車種、App 版本與站點索引版本才會離開裝置；不包含車號、車柱、借還時間、通知全文或付款資料。
- 為避免同一問題重複提醒，App 只在本機保存目前站點索引版本與正規化站名的 SHA-256 雜湊，不保存回報站名明文。索引版本更新後會自動重置去重紀錄。
- Debug build 會在程序記憶體保留最近 30 筆 YouBike 原始 payload，正式版不提供此入口。

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
- 一般 Uber 行程支援英文與繁體中文通知文案；繁中可辨識「N 分鐘內上車」、
  「即將抵達」、「已抵達」及「下車地點／正在前往」。
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
- YouBike 的「鬧鐘與提醒」特殊存取為選用；用於提高計費邊界更新準確度，不影響其他功能。
- 若要顯示為系統 Live Update，裝置系統也需允許第三方 App 顯示 promoted notifications。

## 使用方式

1. 安裝並開啟 App。
2. 開啟「通知存取權限」，允許「即時狀態提醒」讀取來源 App 的通知。
3. 允許 App 顯示通知。
4. 在各 App 分頁使用模擬按鈕驗證狀態與進度。

Samsung One UI 8 若無法顯示在 Now Bar，可參考 GitHub Pages 的
[Samsung Now Bar 疑難排解](https://jimmy90109.github.io/live-status-reminder/samsung-now-bar.html)。

點擊提醒會開啟對應 App。若尚未安裝，則前往 Google Play。iPASS MONEY 與台灣 Pay 目前沒有公開乘車碼頁面的 deep link，因此只能開啟 App 首頁。

## PIN 隱私

- PIN 只保留在記憶體中，不會寫入檔案、偏好設定或正式日誌。
- 可可靠辨識時，PIN 會顯示在即時通知／Live Update 中，方便核對行程或外送。
- 第一版分別只追蹤一筆 Uber 行程與一筆 Uber Eats 訂單；新狀態會取代上一筆狀態。
- Google 時鐘只在原生 Live Update 未生效時鏡像來源通知指定的主要倒數計時器，不處理碼表。

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

更新 YouBike 站點索引前，先在環境或未追蹤的 `local.properties` 中設定
`TDX_CLIENT_ID`、`TDX_CLIENT_SECRET`（可複製 `local.properties.example`），再執行：

```bash
python3 tools/update_youbike_station_index.py
```

憑證與 TDX 原始回應不會寫入 repository；產生器遇到流量限制時會退避重試，產出的 YouBike 2.0 精簡索引位於 `app/src/main/res/raw/youbike_stations.tsv`。
