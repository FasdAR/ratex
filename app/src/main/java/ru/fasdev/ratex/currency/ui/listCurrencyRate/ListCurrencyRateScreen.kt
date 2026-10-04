package ru.fasdev.ratex.currency.ui.listCurrencyRate

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import java.text.DecimalFormat
import ru.fasdev.ratex.R
import ru.fasdev.ratex.currency.domain.entity.RateCurrencyDomain
import ru.fasdev.ratex.currency.ui.selectCurrency.SelectCurrencySheet
import ru.fasdev.ratex.currency.ui.selectCurrency.SelectCurrencyViewModel

const val TAG_LIST_CURRENCY_RATE = "list_currency_rate"
const val TAG_BASE_CURRENCY = "base_currency"

private val ColorSecondaryText = Color(0xFF7B7B7B)
private val ColorDivider = Color(0xFFEAEAEA)

@Composable
fun ListCurrencyRateRoute(viewModel: ListCurrencyRateViewModel, selectCurrencyViewModel: SelectCurrencyViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var isSheetVisible by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.onErrorShown()
        }
    }

    ListCurrencyRateScreen(
        state = state,
        onRefresh = viewModel::onRefresh,
        onBaseCurrencyClick = { isSheetVisible = true }
    )

    if (isSheetVisible) {
        SelectCurrencySheet(
            viewModel = selectCurrencyViewModel,
            onDismiss = {
                isSheetVisible = false
                viewModel.onBaseCurrencyChanged()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListCurrencyRateScreen(state: ListCurrencyRateState, onRefresh: () -> Unit, onBaseCurrencyClick: () -> Unit) {
    Scaffold(
        modifier = Modifier.testTag(TAG_LIST_CURRENCY_RATE),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onBaseCurrencyClick)
                    .testTag(TAG_BASE_CURRENCY)
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.relative_currency),
                    modifier = Modifier.weight(1f),
                    color = ColorSecondaryText,
                    fontSize = 16.sp
                )
                Text(text = state.baseCurrency.orEmpty(), color = ColorSecondaryText, fontSize = 16.sp)
            }
            HorizontalDivider(color = ColorDivider)

            PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.rates, key = { it.currency.currencyCode }) { rate ->
                        CurrencyRateItem(rate)
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencyRateItem(rate: RateCurrencyDomain) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(ColorDivider)
                .border(BorderStroke(1.dp, ColorDivider), CircleShape)
        ) {
            AsyncImage(
                model = rate.currency.urlImage,
                contentDescription = rate.currency.currencyCode,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().padding(1.dp).clip(CircleShape)
            )
        }

        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                text = rate.currency.displayName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.Black,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(text = rate.currency.currencyCode, color = ColorSecondaryText, fontSize = 12.sp)
        }

        Text(text = DecimalFormat("###.##").format(rate.rate), color = Color.Black, fontSize = 16.sp)
    }
}
