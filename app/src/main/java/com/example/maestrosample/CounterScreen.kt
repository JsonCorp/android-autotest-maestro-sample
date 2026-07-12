package com.example.maestrosample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

private const val MILESTONE = 10

@Composable
fun CounterScreen(
    count: Int,
    onCountChange: (Int) -> Unit,
    onNavigateToHistory: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Maestro 카운터 샘플",
            style = MaterialTheme.typography.titleMedium,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "$count",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.testTag("text_counter"),
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (count > MILESTONE) {
            Text(
                text = "🎉 $MILESTONE 달성!",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag("text_milestone"),
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { onCountChange((count - 1).coerceAtLeast(0)) },
                modifier = Modifier.testTag("btn_decrement"),
            ) { Text("-1") }

            Button(
                onClick = { onCountChange(0) },
                modifier = Modifier.testTag("btn_reset"),
            ) { Text("초기화") }

            Button(
                onClick = { onCountChange(count + 1) },
                modifier = Modifier.testTag("btn_increment"),
            ) { Text("+1") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onNavigateToHistory,
            modifier = Modifier.testTag("btn_history"),
        ) { Text("기록") }
    }
}
