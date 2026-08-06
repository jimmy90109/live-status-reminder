# Google Play 版本更新清單

本清單用於已上架 App 的後續版本更新。每次發版重新檢查，不在本檔永久勾選個別版本，也不追蹤首次上架資格、Production access 或封閉測試天數。

## 發版範圍

- [ ] 確認目標 `versionCode`、`versionName`、軌道與 rollout 比例。
- [ ] 依實際 diff 撰寫繁體中文 release notes。
- [ ] 確認本次只執行使用者授權的 Alpha、Production、Git 或 GitHub Release 階段。
- [ ] 檢查工作目錄，保留任務外的未提交變更與簽署秘密。

## 資料與文件

- [ ] 執行 `python3 tools/update_youbike_station_index.py`。
- [ ] 檢查 YouBike 索引的 `generatedAt`、筆數、服務區域與 diff；異常時停止最終建置。
- [ ] 核對 App 行為、支援來源、權限、隱私政策、README 與商店文案是否一致。
- [ ] 更新 `docs/play-store/listing-zh-TW.md` 的本版發布說明。

## 建置與驗證

- [ ] 執行 `./gradlew verifyReleaseSigning test lintRelease bundleRelease`。
- [ ] 確認 AAB 內的 package、`versionCode` 與 `versionName` 正確。
- [ ] 找到 signed AAB 與同一建置的 Play native debug symbols。
- [ ] 計算並記錄 AAB 與 symbols 的 SHA-256。
- [ ] 確認沒有把 `.gradle/`、`build/`、`app/build/` 或簽署材料納入 Git。

## Alpha

- [ ] 核對 Alpha 目前版本，避免重複上傳相同 version code。
- [ ] 上傳 signed AAB 與同一建置的 native debug symbols。
- [ ] 填入本版 release notes。
- [ ] 檢查 Play 警告、錯誤、裝置支援損失、國家／地區及測試者設定。
- [ ] 使用者要求送審時才提交；送審後記錄狀態並停止主動輪詢。
- [ ] 過審後依需要執行 `alpha-testing-checklist-zh-TW.md` 的 App 回歸驗證。

## Production

- [ ] 取得本次 Production 建立、送審或 rollout 的明確授權。
- [ ] 確認目標版本已符合指定的 Alpha 測試或審查條件。
- [ ] 核對 Production 使用的 AAB、symbols、版本與已驗證 Alpha 產物完全一致。
- [ ] 預覽裝置支援、國家／地區、警告與 rollout 比例。
- [ ] 只使用指定 rollout 比例；送審後記錄狀態並停止主動輪詢。
- [ ] Play Console 明確顯示上線後，才記錄為通過審查或已公開。

## 紀錄與 Git

- [ ] 更新 `docs/play-store/release-status.md` 的版本、日期、軌道、狀態、revision、產物路徑與雜湊。
- [ ] 執行 `git diff --check` 並檢查最終 diff。
- [ ] 只有使用者要求時才 commit、push、建立 PR、轉 Ready 或 merge。
- [ ] 只有發版內容已在 `main` 且使用者要求時，才建立 annotated tag 與 GitHub Release。
- [ ] GitHub Release 內容取自該 tag 的實際變更與商店發布說明，不使用自動產生的 notes。
