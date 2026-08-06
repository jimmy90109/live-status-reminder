package com.github.jimmy90109.livestatus.ui.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.github.jimmy90109.livestatus.R
import com.github.jimmy90109.livestatus.YouBikeRegion
import com.github.jimmy90109.livestatus.YouBikeRideManager
import com.github.jimmy90109.livestatus.YouBikeRideSessionStore
import com.github.jimmy90109.livestatus.isExpired
import com.github.jimmy90109.livestatus.isFareSupported

class YouBikeRegionPickerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
        val session = YouBikeRideSessionStore.load(this)
        if (sessionId == null || session?.id != sessionId || session.isExpired(System.currentTimeMillis())) {
            closeDialogTask()
            return
        }
        val choices = youBikeRegionChoices(session.candidateRegions)
        setContent {
            SystemDynamicColorTheme {
                RegionPickerDialog(
                    choices = choices,
                    onSelect = { region ->
                        YouBikeRideManager.selectRegion(this, sessionId, region)
                        closeDialogTask()
                    },
                    onDismiss = ::closeDialogTask,
                )
            }
        }
    }

    private fun closeDialogTask() {
        finishAndRemoveTask()
    }

    companion object {
        private const val EXTRA_SESSION_ID = "you_bike_session_id"

        fun createIntent(context: Context, sessionId: String): Intent =
            Intent(context, YouBikeRegionPickerActivity::class.java)
                .putExtra(EXTRA_SESSION_ID, sessionId)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                        Intent.FLAG_ACTIVITY_NO_HISTORY or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS,
                )
    }
}

@Composable
private fun SystemDynamicColorTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colorScheme = if (isSystemInDarkTheme()) {
        dynamicDarkColorScheme(context)
    } else {
        dynamicLightColorScheme(context)
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

@Composable
private fun RegionPickerDialog(
    choices: List<YouBikeRegion>,
    onSelect: (YouBikeRegion) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.you_bike_region_picker_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.you_bike_region_picker_description))
                choices.forEachIndexed { index, region ->
                    if (index > 0) HorizontalDivider()
                    RegionChoice(stringResource(region.labelRes()), region) { onSelect(region) }
                }
                HorizontalDivider()
                RegionChoice(
                    stringResource(R.string.you_bike_region_other),
                    YouBikeRegion.UNSUPPORTED,
                ) { onSelect(it) }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = rememberHapticAction(action = onDismiss)) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun RegionChoice(
    label: String,
    region: YouBikeRegion,
    onSelect: (YouBikeRegion) -> Unit,
) {
    Text(
        text = label,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .hapticClickable(role = Role.RadioButton, effect = HapticEffect.SELECTION) {
                onSelect(region)
            }
            .semantics { stateDescription = "未選取" }
            .padding(horizontal = 8.dp, vertical = 14.dp),
    )
}

private val supportedRegions = listOf(
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
)

internal fun youBikeRegionChoices(candidateRegions: Set<YouBikeRegion>): List<YouBikeRegion> {
    val supportedCandidates = candidateRegions.filterTo(mutableSetOf()) { it.isFareSupported }
    return supportedCandidates.takeIf { it.isNotEmpty() }
        ?.let { candidates -> supportedRegions.filter(candidates::contains) }
        ?: supportedRegions
}

@StringRes
private fun YouBikeRegion.labelRes(): Int = when (this) {
    YouBikeRegion.TAIPEI -> R.string.you_bike_region_taipei
    YouBikeRegion.NEW_TAIPEI -> R.string.you_bike_region_new_taipei
    YouBikeRegion.TAOYUAN -> R.string.you_bike_region_taoyuan
    YouBikeRegion.HSINCHU_COUNTY -> R.string.you_bike_region_hsinchu_county
    YouBikeRegion.HSINCHU_CITY -> R.string.you_bike_region_hsinchu_city
    YouBikeRegion.HSINCHU_SCIENCE_PARK -> R.string.you_bike_region_hsinchu_science_park
    YouBikeRegion.MIAOLI -> R.string.you_bike_region_miaoli
    YouBikeRegion.TAICHUNG -> R.string.you_bike_region_taichung
    YouBikeRegion.CHIAYI_CITY -> R.string.you_bike_region_chiayi_city
    YouBikeRegion.CHIAYI_COUNTY -> R.string.you_bike_region_chiayi_county
    YouBikeRegion.TAINAN -> R.string.you_bike_region_tainan
    YouBikeRegion.KAOHSIUNG -> R.string.you_bike_region_kaohsiung
    YouBikeRegion.PINGTUNG -> R.string.you_bike_region_pingtung
    YouBikeRegion.TAITUNG -> R.string.you_bike_region_taitung
    YouBikeRegion.UNSUPPORTED,
    YouBikeRegion.UNRESOLVED,
    -> R.string.you_bike_region_other
}
