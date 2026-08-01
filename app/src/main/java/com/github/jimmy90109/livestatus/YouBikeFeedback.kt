package com.github.jimmy90109.livestatus

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

data class YouBikeFeedbackReport(
    val stationName: String,
    val normalizedStationName: String,
    val selectedRegion: YouBikeRegion,
    val issue: YouBikeStationResolutionIssue,
    val candidateRegions: Set<YouBikeRegion>,
    val vehicleType: YouBikeVehicleType,
    val appVersionName: String,
    val appVersionCode: Long,
    val stationIndexVersion: String,
)

object YouBikeFeedbackPolicy {
    fun createReport(
        session: YouBikeRideSession,
        appVersionName: String,
        appVersionCode: Long,
        stationIndexVersion: String,
    ): YouBikeFeedbackReport? {
        val issue = session.originalResolutionIssue ?: return null
        val selectedRegion = session.manuallySelectedRegion?.takeIf { it.isFareSupported } ?: return null
        return YouBikeFeedbackReport(
            stationName = session.stationName,
            normalizedStationName = YouBikeStationResolver.normalizeStationName(session.stationName),
            selectedRegion = selectedRegion,
            issue = issue,
            candidateRegions = session.candidateRegions.filterTo(linkedSetOf()) { it.isFareSupported },
            vehicleType = session.vehicleType,
            appVersionName = appVersionName,
            appVersionCode = appVersionCode,
            stationIndexVersion = stationIndexVersion,
        )
    }
}

object YouBikeStationIndexMetadata {
    private const val GENERATED_AT_PREFIX = "# generatedAt="

    fun parseVersion(lines: Sequence<String>): String? = lines
        .take(4)
        .firstOrNull { it.startsWith(GENERATED_AT_PREFIX) }
        ?.removePrefix(GENERATED_AT_PREFIX)
        ?.substringBefore(' ')
        ?.takeIf(String::isNotBlank)
}

data class YouBikeFeedbackPromptState(
    val stationIndexVersion: String?,
    val promptedStationHashes: Set<String>,
)

data class YouBikeFeedbackPromptDecision(
    val shouldPrompt: Boolean,
    val updatedState: YouBikeFeedbackPromptState,
)

object YouBikeFeedbackDedupPolicy {
    fun markPrompted(
        state: YouBikeFeedbackPromptState,
        stationIndexVersion: String,
        normalizedStationName: String,
    ): YouBikeFeedbackPromptDecision {
        val currentHashes = if (state.stationIndexVersion == stationIndexVersion) {
            state.promptedStationHashes
        } else {
            emptySet()
        }
        val stationHash = hashStationName(normalizedStationName)
        val shouldPrompt = stationHash !in currentHashes
        return YouBikeFeedbackPromptDecision(
            shouldPrompt = shouldPrompt,
            updatedState = YouBikeFeedbackPromptState(
                stationIndexVersion = stationIndexVersion,
                promptedStationHashes = if (shouldPrompt) currentHashes + stationHash else currentHashes,
            ),
        )
    }

    fun hashStationName(normalizedStationName: String): String = MessageDigest
        .getInstance("SHA-256")
        .digest(normalizedStationName.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
}

object YouBikeFeedbackEmail {
    const val RECIPIENT = "jimmy.huang.dev@gmail.com"
    private const val SUBJECT = "[LiveStatus] YouBike 站點辨識回報"

    fun mailtoUri(report: YouBikeFeedbackReport): String = buildString {
        append("mailto:")
        append(RECIPIENT)
        append("?subject=")
        append(encode(SUBJECT))
        append("&body=")
        append(encode(report.emailBody()))
    }

    private fun YouBikeFeedbackReport.emailBody(): String = buildString {
        appendLine("您好，我想回報 YouBike 站點辨識問題：")
        appendLine()
        appendLine("站點名稱：$stationName")
        appendLine("正規化站名：$normalizedStationName")
        appendLine("選擇地區：${selectedRegion.feedbackDisplayName()}")
        appendLine("問題類型：${issue.feedbackDisplayName()}")
        if (candidateRegions.isNotEmpty()) {
            appendLine(
                "同名候選：" + candidateRegions
                    .sortedBy(YouBikeRegion::ordinal)
                    .joinToString("、") { it.feedbackDisplayName() },
            )
        }
        appendLine("車種：${vehicleType.displayName}")
        appendLine("App 版本：$appVersionName ($appVersionCode)")
        appendLine("站點索引：$stationIndexVersion")
        appendLine()
        append("以上內容由 App 自動帶入，請確認後再寄送。")
    }

    private fun encode(value: String): String = URLEncoder
        .encode(value, StandardCharsets.UTF_8.name())
        .replace("+", "%20")
}

internal fun YouBikeRegion.feedbackDisplayName(): String = when (this) {
    YouBikeRegion.TAIPEI -> "臺北市"
    YouBikeRegion.NEW_TAIPEI -> "新北市"
    YouBikeRegion.TAOYUAN -> "桃園市"
    YouBikeRegion.HSINCHU_COUNTY -> "新竹縣"
    YouBikeRegion.HSINCHU_CITY -> "新竹市"
    YouBikeRegion.HSINCHU_SCIENCE_PARK -> "新竹科學園區"
    YouBikeRegion.MIAOLI -> "苗栗縣"
    YouBikeRegion.TAICHUNG -> "臺中市"
    YouBikeRegion.CHIAYI_CITY -> "嘉義市"
    YouBikeRegion.CHIAYI_COUNTY -> "嘉義縣"
    YouBikeRegion.TAINAN -> "臺南市"
    YouBikeRegion.KAOHSIUNG -> "高雄市"
    YouBikeRegion.PINGTUNG -> "屏東縣"
    YouBikeRegion.TAITUNG -> "臺東縣"
    YouBikeRegion.UNSUPPORTED -> "其他地區"
    YouBikeRegion.UNRESOLVED -> "未解析"
}

private fun YouBikeStationResolutionIssue.feedbackDisplayName(): String = when (this) {
    YouBikeStationResolutionIssue.UNKNOWN -> "找不到站點"
    YouBikeStationResolutionIssue.AMBIGUOUS -> "同名站點"
}
