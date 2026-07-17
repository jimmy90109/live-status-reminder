# AGENTS.md

本文件適用於整個 repository。請以繁體中文溝通與撰寫使用者可見文件；程式碼識別字維持英文。

## 專案概覽

- 這是單一 `app` 模組的 Android 專案，使用 Kotlin、Jetpack Compose 與 Gradle Groovy DSL。
- 套件與 namespace：`com.github.jimmy90109.livestatus`。
- 最低支援 Android 16（API 36），`compileSdk`／`targetSdk` 目前為 37。
- 使用 AGP 9、JDK 21；版本與相依套件以根目錄及 `app/build.gradle` 為準，不要依此文件猜測版本。
- App 監聽 iPASS MONEY、foodpanda、Uber、Uber Eats、Pikmin Bloom 通知，轉換成 Android Live Update；首頁也提供各來源 App 的設定與模擬介面。

## 重要位置

- `app/src/main/java/com/github/jimmy90109/livestatus/LiveStatusNotificationListenerService.kt`：接收來源通知並協調狀態。
- `app/src/main/java/com/github/jimmy90109/livestatus/LiveStatusNotificationParser.kt`：純 Kotlin 通知文字解析邏輯。
- `app/src/main/java/com/github/jimmy90109/livestatus/LiveStatusReminder.kt`：建立、更新及取消 Live Update。
- `app/src/main/java/com/github/jimmy90109/livestatus/AppReminderPreferences.kt`：各 App 開關與本機偏好。
- `app/src/main/java/com/github/jimmy90109/livestatus/ui/home/`：Compose UI 與 Activity。
- `app/src/test/`：JVM 單元測試，尤其是通知解析器的實際文案案例。
- `docs/play-store/`：商店上架、隱私政策與發佈檢查文件。

## 工作原則

- 修改前先閱讀相關實作與測試，保持既有架構、命名、格式和 Compose 元件風格；不要為局部需求進行無關重構。
- 保留使用者既有的未提交變更。不要還原、覆寫或格式化任務範圍外的檔案。
- 優先讓通知解析保持純函式且可由 JVM 測試；新增或調整來源通知文案時，同步加入正例、結束狀態、無關文案及易誤判案例。
- 比對通知文字時要處理 `null`、空白、大小寫與多行內容；事件判斷應先匹配「完成／取消」等終止狀態，再匹配進行中狀態，避免較寬鬆條件遮蔽精確條件。
- 不要擴大權限、可查詢套件、資料保存、外部連線或分析追蹤，除非需求明確要求，並同步檢查 Manifest、隱私政策及 Play 商店文件。
- UI 使用 edge-to-edge；新增畫面或元件時需處理 system bars、顯示切口、IME、字級放大、深色模式及基本無障礙語意。
- 使用者可見字串優先放入 `res/values/strings.xml`。若觸及現有硬編碼文案，可在相同範圍內一併資源化，但不要擴張成全專案重寫。

## 隱私與通知限制

- PIN 與完整通知內容視為敏感資料。正式流程不可將 PIN 寫入檔案、SharedPreferences、分析服務或正式日誌。
- Uber／Uber Eats PIN 只接受目前明確支援的來源：剛好四位數的 `shortCriticalText`，或通知 view text 中四個各自成行的數字。不可把 ETA、年份、訂單編號或車牌尾碼當成 PIN。
- 完整 PIN 只可在裝置解鎖後顯示；鎖定畫面的公開版通知不可包含 PIN。
- Debug payload 功能若需變更，必須維持僅限 debug build，並避免讓敏感資料進入 release 行為或一般日誌。
- 來源通知移除、訂單完成／取消或行程結束時，應確實清除對應提醒；新增狀態時也要驗證終止路徑。

## 建置與驗證

依變更範圍執行最小充分驗證；交付前回報實際執行的命令與結果。

```bash
# 通知解析或純邏輯變更
./gradlew test

# Compose、資源、Manifest 或一般 App 變更
./gradlew test assembleDebug lintDebug
```

- 若只需跑解析器測試，可使用：

```bash
./gradlew testDebugUnitTest --tests '*LiveStatusNotificationParserTest'
```

- Release／簽署相關變更另執行適當的 release task；不要建立、修改或提交真實 `keystore.properties`、keystore、密碼或金鑰。範例設定只放在 `keystore.properties.example`。
- 此專案可能把建置產物導向 Gradle 使用者目錄；不要假設 APK 一定位於 repository 內的 `app/build/`，以 Gradle 輸出與 README 為準。
- 無法執行的檢查要明確說明原因，不可宣稱未執行的測試已通過。

## 文件與交付

- 行為、支援 App、權限、系統需求或使用流程改變時，更新 `README.md`。
- 影響資料處理、隱私聲明或上架資訊時，同步檢查 `docs/play-store/` 下的對應文件。
- 提交內容保持小而聚焦；不要提交 `.gradle/`、`build/`、`app/build/`、簽署材料或其他產生檔。
