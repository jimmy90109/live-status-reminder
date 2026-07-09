# Upload keystore 排查筆記

此文件只記錄排查流程，不應寫入真實密碼、完整私人檔案路徑或 keystore 檔案。

## 目前狀態

`keystore.properties` 已存在，但目前執行：

```bash
./gradlew verifyReleaseSigning
```

會失敗並顯示：

```text
Upload keystore could not be opened. Check storeFile and storePassword in keystore.properties.
```

這代表 Gradle 找得到 `keystore.properties`，也找得到 `storeFile` 指向的檔案，但無法用目前的 `storePassword` 開啟 keystore。

## 優先排查順序

1. 確認 `storeFile` 指向的是「這個 App 的 upload key」。
   - 若沿用其他 App 的 upload key，確認該 keystore 確實是當初上傳到 Play Console 的 upload key。
   - 不要使用 debug keystore。
2. 確認 `storePassword` 是 keystore 的密碼。
   - 這是目前錯誤最可能的原因。
   - 注意前後空白、全形/半形符號、密碼管理器複製時是否帶到換行。
3. 開啟成功後，若下一個錯誤變成 alias 不存在，再修 `keyAlias`。
4. alias 正確後，若下一個錯誤變成 key 無法開啟，再修 `keyPassword`。

## 安全檢查指令

列出 keystore 裡的 alias：

```bash
keytool -list -keystore "你的-upload-key.jks"
```

輸入密碼後，如果成功列出 alias，代表 `storePassword` 正確。把列出的 alias 填到 `keystore.properties` 的 `keyAlias`。

確認 Gradle 可以使用設定：

```bash
./gradlew verifyReleaseSigning
```

重新產生正式簽署 AAB：

```bash
./gradlew test lintRelease bundleRelease
```

成功後 AAB 會在：

```text
app/build/outputs/bundle/release/app-release.aab
```

## `keystore.properties` 格式

```properties
storeFile=keystore/upload-key.jks
storePassword=REPLACE_WITH_STORE_PASSWORD
keyAlias=upload
keyPassword=REPLACE_WITH_KEY_PASSWORD
```

注意：

- `storeFile` 可用相對於 repo root 的路徑，或本機絕對路徑。
- `storePassword` 是開啟 keystore 的密碼。
- `keyAlias` 是 keystore 裡那把 upload key 的 alias。
- `keyPassword` 是該 alias/private key 的密碼；有些 keystore 會跟 `storePassword` 相同，有些不同。

## 不要提交的東西

以下都不可進 Git：

- `keystore.properties`
- `*.jks`
- `*.keystore`
- 任何包含密碼、alias 密碼或 Play App signing 憑證的截圖或文字檔
