package com.dimowner.audiorecorder.v2.app.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dimowner.audiorecorder.R
import com.dimowner.audiorecorder.v2.app.components.DISABLED_ALPHA
import com.dimowner.audiorecorder.v2.data.model.StorageLocation

/**
 * Lets the user choose where new recordings are saved. Choosing "a folder I choose" opens the
 * system folder picker every time, so picking it again is how the folder is changed.
 */
@Composable
internal fun StorageLocationSetting(
    selected: StorageLocation,
    folderText: String,
    enabled: Boolean,
    onSelect: (StorageLocation) -> Unit,
    onFolderPicked: (Uri) -> Unit,
) {
    val expanded = remember { mutableStateOf(false) }
    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) onFolderPicked(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.TopStart)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
    ) {
        DropdownMenu(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false }
        ) {
            StorageLocation.entries.forEach { location ->
                DropdownMenuItem(
                    onClick = {
                        expanded.value = false
                        if (location == StorageLocation.CUSTOM) {
                            pickFolder.launch(null)
                        } else {
                            onSelect(location)
                        }
                    }, text = {
                        SettingsItemText(text = stringResource(location.labelRes()))
                    }
                )
            }
        }
        val label = stringResource(selected.labelRes())
        Row(
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .clickable(enabled = enabled) { expanded.value = !expanded.value },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier
                    .padding(24.dp, 16.dp, 16.dp, 16.dp)
                    .wrapContentSize(),
                painter = painterResource(id = R.drawable.ic_folder_open),
                contentDescription = label,
            )
            Column(
                modifier = Modifier
                    .padding(0.dp, 12.dp)
                    .weight(1f)
            ) {
                Text(
                    text = stringResource(id = R.string.storage_location),
                    fontFamily = FontFamily(
                        Font(
                            DeviceFontFamilyName("sans-serif"),
                            weight = FontWeight.Bold
                        )
                    ),
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    text = label,
                    fontSize = 20.sp,
                    fontFamily = FontFamily(
                        Font(
                            DeviceFontFamilyName("sans-serif"),
                            weight = FontWeight.Light
                        )
                    ),
                )
                if (folderText.isNotEmpty() && folderText != label) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        text = folderText,
                        fontSize = 14.sp,
                    )
                }
            }
            Icon(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(0.dp, 0.dp, 12.dp, 0.dp),
                painter = painterResource(id = R.drawable.ic_arrow_down),
                contentDescription = label,
            )
        }
    }
}

private fun StorageLocation.labelRes(): Int = when (this) {
    StorageLocation.PRIVATE -> R.string.storage_location_private
    StorageLocation.PUBLIC -> R.string.storage_location_public
    StorageLocation.CUSTOM -> R.string.storage_location_custom
}
