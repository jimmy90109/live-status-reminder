package com.github.jimmy90109.livestatus.ui.home

import com.github.jimmy90109.livestatus.YouBikeRegion
import org.junit.Assert.assertEquals
import org.junit.Test

class YouBikeRegionPickerTest {
    @Test
    fun unknownStationListsEverySupportedRegionInGeographicOrder() {
        assertEquals(
            listOf(
                YouBikeRegion.TAIPEI,
                YouBikeRegion.NEW_TAIPEI,
                YouBikeRegion.TAOYUAN,
                YouBikeRegion.HSINCHU_COUNTY,
                YouBikeRegion.HSINCHU_CITY,
                YouBikeRegion.HSINCHU_SCIENCE_PARK,
                YouBikeRegion.MIAOLI,
                YouBikeRegion.TAICHUNG,
                YouBikeRegion.CHIAYI_CITY,
                YouBikeRegion.CHIAYI_COUNTY,
                YouBikeRegion.TAINAN,
                YouBikeRegion.KAOHSIUNG,
                YouBikeRegion.PINGTUNG,
                YouBikeRegion.TAITUNG,
            ),
            youBikeRegionChoices(emptySet()),
        )
    }

    @Test
    fun ambiguousStationListsOnlySupportedCandidatesInGeographicOrder() {
        assertEquals(
            listOf(YouBikeRegion.TAOYUAN, YouBikeRegion.TAICHUNG),
            youBikeRegionChoices(
                setOf(
                    YouBikeRegion.TAICHUNG,
                    YouBikeRegion.UNSUPPORTED,
                    YouBikeRegion.TAOYUAN,
                ),
            ),
        )
    }
}
