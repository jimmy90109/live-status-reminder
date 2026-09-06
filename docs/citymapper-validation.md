# Citymapper 導航映射驗證

產品語言標籤與正式支援範圍目前為繁中。英文案例僅保留作為既有容錯與防回歸驗證，不構成正式支援聲明。

驗證日期：2026-08-31

## 首次映射驗證

- `./gradlew test assembleDebug lintDebug compileReleaseKotlin assembleDebugAndroidTest processReleaseMainManifest --no-daemon --max-workers=1` 成功。
- 282 項 JVM 測試通過，其中 9 項為 Citymapper：涵蓋步行、候車、搭乘樣本文字、未知語言／指示、多行與空白、操作文字清理、通知資格、來源更新／移除、空文字清除、重新連線取最新來源及 reset。
- Lint 無錯誤、11 項警告；Release Kotlin 編譯通過。
- `./gradlew connectedDebugAndroidTest --no-daemon --max-workers=1` 在 Pixel 9 Pro XL（Android 17／API 37）通過 4 項整合測試。
- 裝置測試以合成通知與替代站名驗證多行 RemoteViews 擷取、重複內容清理、通知標題／展開內文、固定膠囊、系統可提升特徵、三種來源隱私等級、無效按鈕過濾、更新後移除舊操作及缺少來源點擊意圖時的 fallback。
- PendingIntent 測試只發送測試專用的本機 broadcast，確認按鈕不會自行執行，明確呼叫後才執行；未操作真實 Citymapper 行程。
- Release 合併 Manifest 包含一筆 Citymapper 套件查詢，無新增網路或定位權限。原始 payload 記錄與查看入口仍受 Debug 條件限制。

## 尚待手動確認

實機目前鎖定，以下項目未宣稱通過：

- 導航卡片在淺色、深色與放大字級下的實際畫面。
- 真實 Citymapper 步行、候車、搭乘通知在系統介面的完整呈現。
- 點擊映射通知返回真實導航頁；Prev／Next 切換步驟並更新提醒。
- 手動 End Trip 後來源通知移除及映射提醒消失。
- 卡片開關立即清除／恢復目前導航，實際撤銷與恢復通知存取權限的流程。
- 開車、單車或其他模式是否仍使用 trip-progress 頻道，以及結束後是否確實取消 ongoing 通知。

裝置測試完成後已重新安裝本次 Debug APK；系統若已撤銷其通知權限或通知存取，須重新授權。沒有修改手機的深色模式或字級設定。

## 抵達時間文案與分享移除追測（2026-08-31）

- 已知繁中與英文抵達尾行整理為「預計抵達時間：xxx到達」，保留來源時間字樣，移除剩餘分鐘；移除中英文分享獨立行及已知分享操作。未知尾行不刪除、不推算。
- `./gradlew test assembleDebug lintDebug compileReleaseKotlin assembleDebugAndroidTest --no-daemon --max-workers=1` 通過；共 285 項 JVM 測試，其中 12 項 Citymapper 測試，lint 無錯誤、11 項警告。
- 使用 `adb install -r` 覆蓋安裝 Debug 與測試 APK，保留既有 App 資料；以 `adb shell am instrument -w -e class com.github.jimmy90109.livestatus.CitymapperNotificationTest com.github.jimmy90109.livestatus.debug.test/androidx.test.runner.AndroidJUnitRunner` 執行，5 項測試全部通過。
- 新增繁中樣本的抵達時間與分享按鈕排除驗證；此輪仍使用替代站名與合成通知，未操作真實行程。

## 台灣通勤階段樣本（2026-09-01）

- 真實 `trip-progress` payload 已觀察到「步行至公車站」與距離分鐘、數字路線公車候車與到站分鐘、「乘坐 N 站」、台鐵發車時間／車次／目的地，以及 BL 捷運候車與到站分鐘。
- 短膠囊採時間優先格式：「8分步行」、「11分公車」、「14:18發車」、「3分捷運」；搭乘階段顯示「N站下車」。步行、公車、台鐵、捷運與未知搭乘分別使用對應的單色圖示。
- 同一來源通知會將最近明確辨認的公車、台鐵或捷運模式延續到後續「乘坐 N 站」；步行、新來源、reset、來源移除與重新連線的孤立搭乘階段不沿用舊模式。
- 分類僅涵蓋上述已觀察句型。單車、YouBike、開車、渡輪、其他捷運線碼與未知候車格式維持通用導航呈現，待取得真實 payload 後再擴充。
- `./gradlew test assembleDebug lintDebug assembleDebugAndroidTest --no-daemon --max-workers=1` 通過；289 項 JVM 測試全部成功，Lint 無錯誤、11 項既有警告，Debug App 與測試 APK 均成功組裝。
- Pixel 9 Pro XL（Android 17／API 37）執行 Citymapper 專屬 `connectedDebugAndroidTest`，6 項測試全部通過，涵蓋短膠囊、small icon、正文、隱私、操作過濾與 PendingIntent 行為。

## 共用候車時間格式（2026-09-01）

- 新增真實公車時刻樣本：「等候 107…」搭配「下午2:03, 下午2:04」；膠囊取第一班、移除上午／下午後顯示「2:03發車」，沿用公車圖示，完整時刻仍保留在通知正文。
- 候車 presentation 統一使用倒數分鐘或發車時刻；公車、捷運、台鐵及未知工具共用解析。未知工具倒數顯示「3分到站」，時刻移除上午／下午或 AM／PM 後加上「發車」，並使用通用運輸圖示。輸出最長 7 個字元，符合 Android 對 `shortCriticalText` 的建議上限。
- 時刻支援上午／下午、24 小時制及英文 AM／PM，接受半形或全形逗號清單；抵達尾行、無效時間及混入非數字的分鐘清單不會誤判。
- 未知候車會清除 tracker 中上一個已知交通模式，避免後續「乘坐 N 站」沿用錯誤圖示；無合法 timing 的未知候車仍使用「導航」fallback。
- `./gradlew test assembleDebug lintDebug assembleDebugAndroidTest --no-daemon --max-workers=1` 通過；291 項 JVM 測試全部成功，Lint 無錯誤、11 項既有警告，Debug App 與測試 APK 均成功組裝。
- Pixel 9 Pro XL（Android 17／API 37）執行 Citymapper 專屬 `connectedDebugAndroidTest`，7 項測試全部通過，包含所有已知／未知模式的共用倒數與時刻膠囊驗證。
- 台北捷運線碼辨識擴充為 `BR`、`R`、`G`、`O`、`BL`、`Y`，不分大小寫；其他純字母代碼仍維持未知運輸 fallback。

## 卡片核心狀態模擬（2026-09-01）

- Citymapper 卡片依步行、公車、台鐵與捷運分組，提供步行 8 分鐘、公車候車 11 分鐘、台鐵下午 2:03 發車、捷運候車 3 分鐘及捷運搭乘 4 站五種固定模擬。
- 每個按鈕直接建立具明確 stage、mode 與 timing 的本機 update，使用同一通知 ID 覆蓋前一狀態；不依賴 tracker、不附來源 actions，也不寫入 Debug payload。
- 模擬按鈕受 Citymapper 開關與必要設定控制；清除按鈕可移除目前模擬或來源映射提醒，Debug payload 操作仍只在 Debug build 顯示。
- `./gradlew test assembleDebug lintDebug assembleDebugAndroidTest --no-daemon --max-workers=1` 通過；293 項 JVM 測試全部成功，Lint 無錯誤、11 項既有警告，Debug App 與測試 APK 均成功組裝。
- Pixel 9 Pro XL（Android 17／API 37）執行 Citymapper 專屬 `connectedDebugAndroidTest`，8 項測試全部通過；五種 fixture 均驗證膠囊、small／左側圖示、無 timer／progress／來源 actions，以及 Citymapper fallback 點擊意圖。
