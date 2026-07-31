# Uber Android 16 Live Update 評估

## 範圍與結論

這組截圖涵蓋單筆 Uber 行程從司機接單、快抵達、已抵達、上車後到評分通知的 Live Update。第一版以英文通知文案為主，將原始通知整理成自有 Live Update。

目前觀察到「快抵達」不應用固定分鐘數判斷；更可靠的訊號是通知內文從 `Meet at ...` 上車點改成 `車牌 · 車款` 與 PIN。實作因此以通知文字結構判斷階段，而不是硬寫幾分鐘內。

## 流程證據

1. **司機前往上車點 — 健康**
   - 畫面：`01-pickup-expanded.png`
   - 標題為 `Pick up in 14 min`，內文為 `Meet at ...`；展開卡片雖也顯示車輛與 PIN，但主要 compact 內容仍是上車點。
2. **快抵達 — 健康**
   - 畫面：`02-pickup-nearby.png`
   - 標題仍為 `Pick up in 2 min`，但內文已變成 `車牌 · 車款`，PIN 也在 compact 版可見。
3. **已抵達 — 健康**
   - 畫面：`03-arrived.png`
   - 標題包含 `arrived`，內文維持 `車牌 · 車款` 與 PIN。
4. **行程中 — 健康**
   - 畫面：`04-on-trip-expanded.png`
   - 標題為 `Dropoff at ...`，內文為 `Heading to ...`；適合映射成下車時間與下車點。
5. **行程完成 — 健康**
   - 畫面：`05-rate-trip.png`
   - 標題為 `Rate your trip`；自有提醒應在此清除。

## UX 與隱私原則

- 自有通知第一階段顯示上車 ETA 與上車點。
- 快抵達與已抵達顯示車牌、車款與 PIN，協助使用者核對車輛。
- 上車後顯示下車 ETA 與下車點。
- 不複製司機照片。
- PIN 僅存在記憶體；不寫入檔案、偏好設定或正式日誌。
- 可可靠辨識時，PIN 會顯示在自有即時通知中，方便使用者核對行程。
- 第一版只追蹤一筆行程，新的行程狀態會取代上一筆狀態與 PIN。

## 尚待實機驗證

- Uber 各階段在英文環境實際寫入 title、text、big text、subtext、summary、text lines 或 custom view text 的字串。
- PIN 是否未來可能出現在 Android 16 `getShortCriticalText()`。
- 大字體、深色模式、鎖定畫面、螢幕分享及系統 Live Update 權限關閉時的呈現。

## 一般 Uber（繁體中文）

這組實機通知使用 `DecoratedCustomViewStyle`，資料同時分布於 `android.title`、
`android.text` 與 custom view text。原始截圖包含姓名、臉孔、地址、車牌與 PIN，
因此不納入 repository；以下只記錄去識別化後的通知結構。

1. **司機前往上車點**
   - 標題為「N 分鐘內上車」，custom view text 另有「在 [上車點] 碰面」。
   - 自有 Live Update 顯示分鐘 ETA 與上車點。
2. **即將抵達**
   - 標題為「[司機] 即將抵達」，車牌與車款可能合併為「[車牌] · [車款]」，
     也可能各自成行。
   - `N` 小於等於 2 且已有車輛資訊或可靠 PIN 時，也視為即將抵達。
3. **已抵達**
   - 標題為「[司機] 已抵達」，內文顯示車牌與車款。
4. **行程中**
   - 標題為「下車地點：[時間]」，內文為「正在前往：[下車點]」。
   - 車牌尾碼等四位數候選值不得當成 PIN。

PIN 只接受剛好四位數的 `shortCriticalText`，或 custom view text 中四個各自成行的數字。
空白群組摘要不會建立或更新自有 Live Update。本次未觀察到一般 Uber 的繁中取消文案，
因此不推測取消狀態。

## 優步小黃（繁體中文）

這組實機通知來自同一筆優步小黃行程。Uber 使用標準 `BigTextStyle`，資料主要位於
`android.title`、`android.text` 與 `android.bigText`，結構與英文一般 Uber 行程不同。

1. **職業駕駛正在途中**
   - 畫面：`06-taxi-en-route-notification.png`、`07-taxi-en-route-debug-summary.png`、
     `08-taxi-en-route-debug-extras.png`
   - 標題為「職業駕駛正在途中」，內文提供「將在 N 分鐘內抵達」。
   - 自有 Live Update 顯示明確分鐘 ETA，例如「4 分鐘」。
2. **職業駕駛已在附近**
   - 畫面：`09-taxi-approaching-debug.png`、`10-taxi-pickup-stages-notification-shade.png`
   - 來源標題為「職業駕駛在幾分鐘後就會抵達」，內文提醒準備碰面。
   - 自有 Live Update 使用較直接的標題「職業駕駛已在附近」。
3. **職業駕駛即將抵達**
   - 畫面：`10-taxi-pickup-stages-notification-shade.png`
   - 內文格式為「駕駛車款為 [車款] ([車牌])」；實際識別資訊已從 audit 圖片遮蔽。
   - 車牌只用於車輛核對，不得當成四位 PIN。
4. **行程完成**
   - 畫面：`11-taxi-trip-ended-debug.png`
   - 標題為「為您的行程評分」；收到後清除自有 Live Update。

目前沒有觀察到優步小黃在「駕駛已抵達」、「行程中」或「取消」時的通知，因此不從一般
Uber 英文文案推測對應狀態。截圖均經裁切與遮蔽；原始檔不納入 repository。
