# Google Play 版本狀態

本文件只記錄既有 LiveStatus App 的版本更新狀態與可驗證產物。首次上架設定、Production access、封閉測試資格及商店初始建置不在此追蹤。

## 目前狀態

| 軌道／項目 | 已確認狀態 |
| --- | --- |
| Alpha 最新上傳 | `2608060 (1.0.6)`，2026-08-06 已發布 |
| Alpha 最新通過 | `2608060 (1.0.6)`，2026-08-06 23:46 通過審查並可供測試人員使用 |
| 公開測試最新發布 | `2609060 (1.0.11)`，2026-09-07 13:18 已發布，可供不限數量的測試人員使用，供應 2 個國家／地區，68 位活躍測試人員 |
| 公開測試待審 | 無 |
| Production | `2609060 (1.0.11)`，2026-09-07 14:21 已通過審查並在 Google Play 上架，完成 100% 全面推出，供應 2 個國家／地區 |
| Production 待審 | 無 |

Google Play：<https://play.google.com/store/apps/details?id=com.github.jimmy90109.livestatus>

## 1.0.11 發版產物

| 項目 | 紀錄 |
| --- | --- |
| 版本 | `2609060 (1.0.11)`；2026-09-07 已發布至 Beta 公開測試與 Production，兩個軌道均 100% 全面推出，未上傳 Alpha |
| 來源 revision | `4b2d50d`；建置時工作目錄另含未提交的發版變更 |
| AAB | `app/build/outputs/bundle/release/app-release.aab` |
| AAB SHA-256 | `565e5f8da408f4f14073ba4253782afbcc9e0068831ff3944ee7550391a51478` |
| Native debug symbols | `app/build/outputs/native-debug-symbols/release/native-debug-symbols-play-1.0.11.zip` |
| Symbols SHA-256 | `d0590a67745d07d5853ea92d42daba3a3d090476239e3a627f99ae3176731695` |
| YouBike 索引 | `generatedAt=2026-09-07T04:44:00+00:00`，TDX 來源 9,521 筆；站點內容與服務區域皆無差異，只有產生時間更新 |
| 本機驗證 | `./gradlew verifyReleaseSigning test lintRelease bundleRelease` 成功；AAB 簽章及 release manifest 內的 package、版本名稱、版本代碼、minSdk 與 targetSdk 已核對 |
| Play 狀態 | Beta 公開測試與 Production 均已發布；Production 於 2026-09-07 14:21 上架並完成 100% 全面推出，AAB 詳細資料確認包含 native debug symbols |
| Play 裝置支援 | 1,662 台；與 1.0.10 相同，Production 供應 2 個國家／地區 |

## 1.0.10 發版產物

| 項目 | 紀錄 |
| --- | --- |
| 版本 | `2609050 (1.0.10)`；2026-09-05 已發布至 Beta 公開測試與 Production，兩個軌道均 100% 全面推出，未上傳 Alpha |
| 來源 revision | `9c13086`；建置時工作目錄另含未提交的發版變更 |
| AAB | `app/build/outputs/bundle/release/app-release.aab` |
| AAB SHA-256 | `81c2008cc5a016785ff1d21e3788f6a293d3ff2f5afb4e0b719bfd54718fbe1a` |
| Native debug symbols | `app/build/outputs/native-debug-symbols/release/native-debug-symbols-play.zip` |
| Symbols SHA-256 | `de7b6bcd18786d836dc1227d1ff1610717de8275c9374d5fabedf3f0307e1438` |
| YouBike 索引 | `generatedAt=2026-09-05T08:10:02+00:00`，TDX 來源 9,521 筆；較 `2609010` 新增 7 筆、移除 1 筆，淨增 6 筆，服務區域未減少 |
| 本機驗證 | `./gradlew verifyReleaseSigning test lintRelease bundleRelease` 成功；AAB 簽章及 release manifest 內的 package、版本名稱、版本代碼、minSdk 與 targetSdk 已核對 |
| Play 裝置支援 | 1,662 台；手機 1,096 台、平板 566 台，停止支援 0 台、新支援 0 台；Beta 公開測試與 Production 均供應 2 個國家／地區 |

## 1.0.9 發版產物

| 項目 | 紀錄 |
| --- | --- |
| 版本 | `2608240 (1.0.9)`；2026-08-24 已發布至 Beta 公開測試與 Production，未上傳 Alpha |
| 來源 revision | `3267675`；建置時工作目錄另含未提交的發版變更 |
| AAB | `app/build/outputs/bundle/release/app-release.aab` |
| AAB SHA-256 | `7113fabef9e581fe35b7282f1eb0bfab5966a08b861b83bc714ec39c0521dc13` |
| Native debug symbols | `app/build/outputs/native-debug-symbols/release/native-debug-symbols-play.zip` |
| Symbols SHA-256 | `de7b6bcd18786d836dc1227d1ff1610717de8275c9374d5fabedf3f0307e1438` |
| YouBike 索引 | `generatedAt=2026-08-24T10:50:01+00:00`，TDX 來源 9,462 筆；較前版新增 17 站、移除 1 站，淨增 16 站，服務區域未減少 |
| 本機驗證 | `./gradlew verifyReleaseSigning test lintRelease bundleRelease` 成功；AAB 簽章及 release manifest 內的 package、版本名稱與版本代碼已核對，release manifest 未加入 Bolt 套件查詢 |
| Play 裝置支援 | 1,617 台；手機 1,074 台、平板 543 台，停止支援 0 台；Beta 公開測試與 Production 均供應 2 個國家／地區 |

## 1.0.8 發版產物

| 項目 | 紀錄 |
| --- | --- |
| 版本 | `2608140 (1.0.8)`；2026-08-15 已發布至 Beta 公開測試與 Production，未上傳 Alpha |
| 來源 revision | `bdcb8f9`；建置時工作目錄另含未提交的發版變更 |
| AAB | `app/build/outputs/bundle/release/app-release.aab` |
| AAB SHA-256 | `76b89a06b88aaf79fda621029c5fa0f3d5f69a372f5ae9abb5145846f57f8bb1` |
| Native debug symbols | `app/build/outputs/native-debug-symbols/release/native-debug-symbols-play.zip` |
| Symbols SHA-256 | `0c0ff274f6363f7c551d8628a484ccc1ce762f1f3be2c5069c305166176e8741` |
| YouBike 索引 | `generatedAt=2026-08-14T14:26:01+00:00`，TDX 來源 9,446 筆；相較 `2608120` 淨增 11 站、無刪除，服務區域未減少 |
| 本機驗證 | `./gradlew verifyReleaseSigning test lintRelease bundleRelease` 成功；AAB 簽章與 bundle 內的 package、版本名稱及版本代碼已核對 |
| Play 裝置支援 | 1,551 台；手機 1,036 台、平板 515 台，停止支援 0 台；Production 供應 2 個國家／地區 |

## 1.0.7 發版產物

| 項目 | 紀錄 |
| --- | --- |
| 版本 | `2608110 (1.0.7)`；2026-08-11 已發布至 Beta 公開測試與 Production，未上傳 Alpha |
| 來源 revision | `2acc5e3`；建置時工作目錄另含未提交的發版變更 |
| AAB | `app/build/outputs/bundle/release/app-release.aab` |
| AAB SHA-256 | `239a02b8c411467ae95b6d0fe0df4bda3ea2c7c106b0fbe73658ac277742f47a` |
| Native debug symbols | `app/build/outputs/native-debug-symbols/release/native-debug-symbols-play.zip` |
| Symbols SHA-256 | `576e54d7a1044ff656560b45615e99006076efe8b60ccacb76865e2871ce555a` |
| YouBike 索引 | `generatedAt=2026-08-11T03:24:01+00:00`，TDX 來源 9,435 筆；相較 `2608100` 淨增 8 站、無刪除，服務區域未減少 |
| 本機驗證 | `./gradlew verifyReleaseSigning test lintRelease bundleRelease` 成功；AAB 簽章與 release manifest 的 package、版本名稱及版本代碼已核對 |
| Play 裝置支援 | 1,551 台；手機 1,036 台、平板 515 台，停止支援 0 台 |

## 最新 Alpha 產物

| 項目 | 紀錄 |
| --- | --- |
| 版本 | `2608060 (1.0.6)` |
| 來源 revision | `77d06b1`；建置時工作目錄另含未提交的發版變更 |
| AAB | `app/build/outputs/bundle/release/app-release.aab` |
| AAB SHA-256 | `133c16df7642c23bc5ee506bbb41566739277450d4f6aab4598ad387e0594731` |
| Native debug symbols | `app/build/outputs/native-debug-symbols/release/native-debug-symbols-play.zip` |
| Symbols SHA-256 | `528bb0393812360b3257a6f08eb06ab981a1138676f8bb7b40b0dcfb2c1b2b0a` |
| YouBike 索引 | `generatedAt=2026-08-06T15:24:00+00:00`，TDX 來源 9,416 筆；相較 `2608050` 淨增 7 站、無刪除，服務區域未減少 |
| Play 裝置支援 | 1,543 台；相較 Alpha 預覽增加 8 台，同一 AAB 未造成裝置支援損失 |

## 版本紀錄

| 日期 | 軌道 | 版本 | 狀態 |
| --- | --- | --- | --- |
| 2026-09-07 | Production | `2609060 (1.0.11)` | 14:21 已通過審查並在 Google Play 上架；完成 100% 全面推出，供應 2 個國家／地區，支援 1,662 台裝置 |
| 2026-09-07 | 公開測試 | `2609060 (1.0.11)` | Signed AAB 與 native debug symbols 已上傳，軌道內 100% 全面推出；13:18 已發布，可供不限數量的測試人員使用，供應 2 個國家／地區，68 位活躍測試人員 |
| 2026-09-05 | Production | `2609050 (1.0.10)` | 16:33 已通過審查並在 Google Play 上架；Play Console 顯示為最新正式版，於台灣與香港完成 100% 全面推出，支援 1,662 台裝置 |
| 2026-09-05 | 公開測試 | `2609050 (1.0.10)` | Signed AAB 與 native debug symbols 已上傳，軌道內 100% 全面推出並送審；16:33 已發布，可供不限數量的測試人員使用，供應 2 個國家／地區 |
| 2026-09-04 | 公開測試 | `2609010 (1.0.10)` | 9 月 1 日 22:07 Signed AAB 與 native debug symbols 已上傳並以軌道內 100% 全面推出送審；9 月 4 日 22:31 已發布 |
| 2026-08-24 | Production | `2608240 (1.0.9)` | 20:12 已通過審查並在 Google Play 上架；Play Console 顯示為最新正式版，供應 2 個國家／地區，支援 1,617 台裝置 |
| 2026-08-24 | 公開測試 | `2608240 (1.0.9)` | 18:59 Signed AAB 與 native debug symbols 已上傳，軌道內 100% 全面推出並送審；19:29 已發布，可供不限數量的測試人員使用 |
| 2026-08-15 | Production | `2608140 (1.0.8)` | 02:56 已通過審查並在 Google Play 上架；Play Console 顯示為最新正式版，供應 2 個國家／地區 |
| 2026-08-15 | 公開測試 | `2608140 (1.0.8)` | 02:56 已發布，可供不限數量的測試人員使用；Play Console 顯示 56 位活躍測試人員 |
| 2026-08-12 | 公開測試 | `2608120 (1.0.8)` | 22:46 Signed AAB 與 native debug symbols 已上傳，軌道內 100% 全面推出並送審；23:20 通過審查並已發布 |
| 2026-08-11 | Production | `2608110 (1.0.7)` | 已通過審查並發布；Play Console 顯示為最新正式版，供應 2 個國家／地區 |
| 2026-08-11 | 公開測試 | `2608110 (1.0.7)` | 11:29 Signed AAB 與 native debug symbols 已上傳，軌道內 100% 全面推出並送審；同日通過審查並已發布 |
| 2026-08-10 | 公開測試 | `2608100 (1.0.7)` | 21:32 Signed AAB 與 native debug symbols 已上傳並送審；21:47 通過審查並已發布 |
| 2026-08-08 | Production | `2608060 (1.0.6)` | 17:58 送審；18:05 通過審查並於台灣與香港完成 100% 全面推出 |
| 2026-08-06 | Alpha | `2608060 (1.0.6)` | 23:32 Signed AAB 與 native debug symbols 已上傳並送審；23:46 通過審查並已發布 |
| 2026-08-06 | Production | `2608050 (1.0.5)` | 20:57 送審；同日通過審查並完成 100% 全面推出 |
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
