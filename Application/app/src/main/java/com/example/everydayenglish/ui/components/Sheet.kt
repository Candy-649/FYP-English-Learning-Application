package com.example.everydayenglish.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.example.everydayenglish.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> BottomSheet(
    title: String,
    initialValue: T,
    content: @Composable () -> Unit,
    onConfirmClicked: (T) -> Unit
){
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val value = remember { mutableStateOf(initialValue) }
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = {
            scope.launch { sheetState.hide() }
        }
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(dimensionResource(R.dimen.padding_medium)),
            style = MaterialTheme.typography.titleLarge
        )

        content()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.padding_medium)),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { scope.launch { sheetState.hide() }}) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalButton(onClick = {
                onConfirmClicked(value.value)
                scope.launch { sheetState.hide() }
            }) {
                Text("Confirm")
            }
        }
    }
}