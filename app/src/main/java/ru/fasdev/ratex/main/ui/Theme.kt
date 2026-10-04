package ru.fasdev.ratex.main.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun RatexTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(), content = content)
}
