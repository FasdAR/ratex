package ru.fasdev.ratex.currency.ui.selectCurrency

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.fasdev.ratex.R
import ru.fasdev.ratex.currency.domain.entity.CurrencyDomain

const val TAG_SELECT_CURRENCY_SHEET = "select_currency_sheet"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectCurrencySheet(viewModel: SelectCurrencyViewModel, onDismiss: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onOpened() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White
    ) {
        SelectCurrencyContent(
            state = state,
            onSearchChanged = viewModel::onSearchChanged,
            onCurrencySelected = viewModel::onCurrencySelected
        )
    }
}

@Composable
fun SelectCurrencyContent(state: SelectCurrencyState, onSearchChanged: (String) -> Unit, onCurrencySelected: (CurrencyDomain) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().testTag(TAG_SELECT_CURRENCY_SHEET)) {
        Text(
            text = stringResource(R.string.select_base_currency),
            modifier = Modifier.padding(horizontal = 16.dp),
            color = Color.Black,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )

        OutlinedTextField(
            value = state.searchText,
            onValueChange = onSearchChanged,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            placeholder = { Text(stringResource(R.string.name_currency_or_index)) },
            singleLine = true,
            shape = RoundedCornerShape(20.dp)
        )

        HorizontalDivider(color = Color(0xFFEAEAEA))

        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
            items(state.currencies, key = { it.currencyCode }) { currency ->
                val isSelected = currency.currencyCode == state.baseCurrency?.currencyCode

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCurrencySelected(currency) }
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currency.displayName,
                        modifier = Modifier.weight(1f),
                        color = Color.Black,
                        fontSize = 15.sp
                    )
                    RadioButton(selected = isSelected, onClick = { onCurrencySelected(currency) })
                }
            }
        }
    }
}
