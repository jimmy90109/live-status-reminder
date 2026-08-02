package com.github.jimmy90109.livestatus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YouBikeStationResolverTest {
    private val rows = sequenceOf(
        "YouBike2.0_龍江錦州街口\tTaipei",
        "同名站\tTaipei",
        "同名站\tNewTaipei",
        "桃園站\tTaoyuan",
        "新竹縣站\tHsinchuCounty",
        "新竹市站\tHsinchuCity",
        "YouBike2.0_(竹科)竹科管理局\tHsinchuSciencePark",
        "YouBike2.0_嘉義市站\tChiayiCity",
        "YouBike2.0_嘉義縣站\tChiayiCounty",
        "YouBike2.0_臺南站\tTainan",
        "YouBike2.0_高雄站\tKaohsiung",
        "YouBike2.0_屏東站\tPingtung",
        "YouBike2.0_臺東站\tTaitung",
        "苗栗站\tMiaoli",
        "臺中站\tTaichung",
        "外縣市站\tUnsupported",
    )

    @Test
    fun normalizesPrefixUnicodeAndWhitespace() {
        assertEquals("龍江錦州街口", YouBikeStationResolver.normalizeStationName(" YouBike2.0_龍江錦州街口 "))
        val result = YouBikeStationResolver.resolve("龍江錦州街口", rows)
        assertEquals(
            YouBikeStationResolution.Supported(YouBikeRegion.TAIPEI),
            result,
        )
    }

    @Test
    fun distinguishesAmbiguousUnsupportedAndUnknownStations() {
        val ambiguous = YouBikeStationResolver.resolve("同名站", rows)
        assertTrue(ambiguous is YouBikeStationResolution.Ambiguous)
        assertEquals(YouBikeStationResolution.Unsupported, YouBikeStationResolver.resolve("外縣市站", rows))
        assertEquals(YouBikeStationResolution.Unknown, YouBikeStationResolver.resolve("不存在", rows))
    }

    @Test
    fun resolvesEveryNewSupportedRegion() {
        mapOf(
            "桃園站" to YouBikeRegion.TAOYUAN,
            "新竹縣站" to YouBikeRegion.HSINCHU_COUNTY,
            "新竹市站" to YouBikeRegion.HSINCHU_CITY,
            "(竹科)竹科管理局" to YouBikeRegion.HSINCHU_SCIENCE_PARK,
            "苗栗站" to YouBikeRegion.MIAOLI,
            "臺中站" to YouBikeRegion.TAICHUNG,
            "嘉義市站" to YouBikeRegion.CHIAYI_CITY,
            "嘉義縣站" to YouBikeRegion.CHIAYI_COUNTY,
            "臺南站" to YouBikeRegion.TAINAN,
            "高雄站" to YouBikeRegion.KAOHSIUNG,
            "屏東站" to YouBikeRegion.PINGTUNG,
            "臺東站" to YouBikeRegion.TAITUNG,
        ).forEach { (station, region) ->
            assertEquals(
                YouBikeStationResolution.Supported(region),
                YouBikeStationResolver.resolve(station, rows),
            )
        }
    }
}
