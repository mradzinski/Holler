package io.github.mradzinski.holler.sample.ui.inbox.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.mradzinski.holler.sample.resources.Res
import io.github.mradzinski.holler.sample.resources.email_delete
import org.jetbrains.compose.resources.stringResource

/**
 * Lets a row be swiped away, to either side, to delete what it shows. While the row moves, the space it
 * leaves shows a delete icon on the theme's error color. It is Material's `SwipeToDismissBox`.
 *
 * @param onDelete called once the row has been swiped away.
 * @param modifier applied to the swipeable area.
 * @param content the row.
 */
@Composable
internal fun SwipeToDelete(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val positionalThreshold = SwipeToDismissBoxDefaults.positionalThreshold

    // The state is remembered, not saved. Material's own `rememberSwipeToDismissBoxState` saves it, and a
    // list keeps the saved state of a row by its key: a row put back by Undo would then come back already
    // swiped away, and delete itself again.
    val state = remember { SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, positionalThreshold) }

    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            DeleteBackground(
                startSide = state.dismissDirection == SwipeToDismissBoxValue.StartToEnd,
            )
        },
        onDismiss = { onDelete() },
        modifier = modifier,
    ) {
        content()
    }
}

/**
 * What shows behind a row while it is swiped: a delete icon on the side the row is leaving.
 *
 * @param startSide whether the row is moving toward the end, which uncovers the start side.
 */
@Composable
private fun DeleteBackground(startSide: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (startSide) {
            Arrangement.Start
        } else {
            Arrangement.End
        },
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 24.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = stringResource(Res.string.email_delete),
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}
