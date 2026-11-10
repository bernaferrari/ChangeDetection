package com.bernaferrari.changedetection.backup

import androidx.compose.runtime.Composable

/** Settings entry for saving and restoring a backup; platforms without a file picker render nothing. */
@Composable expect fun BackupRow()
