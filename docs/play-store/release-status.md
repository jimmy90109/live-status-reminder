# Google Play 版本狀態

本文件只記錄既有 LiveStatus App 的版本更新狀態與可驗證產物。首次上架設定、Production access、封閉測試資格及商店初始建置不在此追蹤。

## 目前狀態

| 軌道／項目 | 已確認狀態 |
| --- | --- |
| Alpha 最新上傳 | `2608050 (1.0.5)`，2026-08-05 已發布 |
| Alpha 最新通過 | `2608050 (1.0.5)`，2026-08-05 通過審查並可供測試人員使用 |
| 公開測試 | `2608050 (1.0.5)`，2026-08-05 已發布 |
| Production | `2608030 (1.0.4)`，2026-08-03 01:35 通過審查並完成 100% 全面推出 |
| Production 待審 | `2608050 (1.0.5)`，2026-08-06 20:57 送審，目前審查中 |

Google Play：<https://play.google.com/store/apps/details?id=com.github.jimmy90109.livestatus>

## 最新 Alpha 產物

| 項目 | 紀錄 |
| --- | --- |
| 版本 | `2608050 (1.0.5)` |
| 來源 revision | `1115d80`；建置時工作目錄另含未提交的發版變更 |
| AAB | `app/build/outputs/bundle/release/app-release.aab` |
| AAB SHA-256 | `c8ca6a6db099d3e75b750393357047bdd5a27f936c9820ec696620fd9c9cc559` |
| Native debug symbols | `app/build/outputs/native-debug-symbols/release/native-debug-symbols-play.zip` |
| Symbols SHA-256 | `528bb0393812360b3257a6f08eb06ab981a1138676f8bb7b40b0dcfb2c1b2b0a` |
| YouBike 索引 | `generatedAt=2026-08-04T17:03:02+00:00`，TDX 來源 9,409 筆；與 `2608040` 相比站點內容無差異 |
| Play 裝置支援 | 1,515 台；相較前版減少 0 台 |

## 版本紀錄

| 日期 | 軌道 | 版本 | 狀態 |
| --- | --- | --- | --- |
| 2026-08-06 | Production | `2608050 (1.0.5)` | 20:57 送審；審查中 |
| 2026-08-05 | Alpha | `2608050 (1.0.5)` | Signed AAB 與 native debug symbols 已上傳；通過審查並已發布 |
| 2026-08-05 | 公開測試 | `2608050 (1.0.5)` | 01:32 送審；已發布 |
| 2026-08-04 | Alpha | `2608040 (1.0.5)` | 16:58 通過審查並可供測試人員使用 |
| 2026-08-03 | Production | `2608030 (1.0.4)` | 01:35 通過審查並完成 100% 全面推出 |

## 維護規則

- 只寫入 Play Console、GitHub 或本機產物已確認的事實，不預寫審查結果。
- 新版送審時更新「目前狀態」與最新產物；過審後再補版本紀錄。
- Production 上線後保留最新正式版與近期關鍵軌道紀錄；不累積首次上架待辦事項。
- AAB 輸入有變時重新建置並更新雜湊，不把不同建置的 symbols 混用。
- 不在此文件記錄 keystore、密碼、token、PIN 或完整通知內容。
