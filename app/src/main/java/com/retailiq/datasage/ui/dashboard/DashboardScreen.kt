package com.retailiq.datasage.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.retailiq.datasage.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.retailiq.datasage.data.api.DailySummary
import com.retailiq.datasage.ui.components.CategoryBreakdown
import com.retailiq.datasage.ui.components.CategoryPieChart
import com.retailiq.datasage.ui.components.DateRevenuePair
import com.retailiq.datasage.ui.components.PaymentModeBarChart
import com.retailiq.datasage.ui.components.PaymentModeBreakdown
import com.retailiq.datasage.ui.components.RevenueLineChart
import com.retailiq.datasage.ui.worker.SyncStatusBar
import androidx.compose.ui.platform.testTag
import com.retailiq.datasage.ui.navigation.UserRole
import com.retailiq.datasage.data.local.AnalyticsSnapshot
import com.retailiq.datasage.data.local.LocalKpiEngine
import com.retailiq.datasage.data.model.SnapshotDto
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    role: UserRole = UserRole.OWNER
) {
    val uiState by viewModel.uiState.collectAsState()
    val pending by viewModel.pendingCount.collectAsState()
    val failed by viewModel.failedCount.collectAsState()
    val categoryBreakdown by viewModel.categoryBreakdown.collectAsState()
    val paymentModes by viewModel.paymentModes.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.datasage_logo),
                            contentDescription = "Logo",
                            modifier = Modifier.height(32.dp).padding(end = 12.dp)
                        )
                        Text("DataSage", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SyncStatusBar(pending, failed)

            if (role == UserRole.STAFF) {
                com.retailiq.datasage.ui.staff.StaffSessionBanner()
            }

            when (val state = uiState) {
                is DashboardUiState.Loading -> {
                    DashboardShimmer()
                }
                is DashboardUiState.Loaded -> {
                    DashboardContent(state.dashboardData, categoryBreakdown, paymentModes)
                }
                is DashboardUiState.Offline -> {
                    Column {
                        val dateText = try {
                            val formatIn = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
                            val date = formatIn.parse(state.snapshot.builtAt.substringBefore("."))
                            SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault()).format(date ?: "")
                        } catch(e: Exception) {
                            state.snapshot.builtAt
                        }
                        
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "Offline — showing data from $dateText", 
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        DashboardOfflineContent(state.snapshot, state.kpiEngine)
                    }
                }
                is DashboardUiState.Error -> {
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.message, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.refresh() }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardContent(
    dashboardData: com.retailiq.datasage.data.api.DashboardPayload,
    categoryBreakdown: List<CategoryBreakdown>,
    paymentModes: List<PaymentModeBreakdown>
) {
    val summary = dashboardData.todayKpis

    LazyColumn(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Text("Today's Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiCard("Revenue", formatCurrency(summary.revenue), Icons.Default.AttachMoney, Modifier.weight(1f))
                KpiCard("Profit", formatCurrency(summary.profit), Icons.AutoMirrored.Filled.TrendingUp, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiCard("Transactions", summary.transactions.toString(), Icons.Default.ShoppingCart, Modifier.weight(1f))
                KpiCard("Avg Basket", formatCurrency(summary.avgBasket), Icons.Default.AttachMoney, Modifier.weight(1f))
            }
        }

        item {
            Text("Revenue Trend (7 Days)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                val revenueData = dashboardData.revenue7d.map { DateRevenuePair(it.date, it.revenue) }
                RevenueLineChart(data = revenueData, modifier = Modifier.padding(16.dp).testTag("RevenueLineChart"))
            }
        }

        item {
            Text("Revenue by Payment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                PaymentModeBarChart(data = paymentModes, modifier = Modifier.padding(16.dp))
            }
        }

        item {
            Text("Category Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                CategoryPieChart(data = categoryBreakdown, modifier = Modifier.padding(16.dp).testTag("CategoryPieChart"))
            }
        }

        if (dashboardData.topProductsToday.isNotEmpty()) {
            item {
                Text("Top Products", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(dashboardData.topProductsToday) { product ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(product.name, fontWeight = FontWeight.Medium)
                                Text("Sold: ${product.unitsSold.toInt()}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        val returnsCount = dashboardData.alertsSummary["returns"] ?: 0
        if (returnsCount > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Returns today: $returnsCount",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun KpiCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Text(label, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatCurrency(amount: Double): String {
    return "₹${String.format("%,.2f", amount)}"
}

@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    Box(modifier.background(Color.Gray.copy(alpha = alpha), RoundedCornerShape(12.dp)))
}

@Composable
fun DashboardShimmer() {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ShimmerBox(Modifier.weight(1f).height(100.dp))
            ShimmerBox(Modifier.weight(1f).height(100.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ShimmerBox(Modifier.weight(1f).height(100.dp))
            ShimmerBox(Modifier.weight(1f).height(100.dp))
        }
        ShimmerBox(Modifier.fillMaxWidth().height(200.dp))
    }
}

@Composable
private fun DashboardOfflineContent(snapshot: AnalyticsSnapshot, kpiEngine: LocalKpiEngine) {
    val dto = Gson().fromJson(snapshot.snapshotJson, SnapshotDto::class.java)
    val kpis = dto.kpis
    
    LazyColumn(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Today's Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiCard("Revenue", formatCurrency(kpis?.todayRevenue ?: 0.0), Icons.Default.AttachMoney, Modifier.weight(1f))
                KpiCard("Profit", formatCurrency(kpis?.todayProfit ?: 0.0), Icons.AutoMirrored.Filled.TrendingUp, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiCard("Transactions", (kpis?.todayTransactions ?: 0).toString(), Icons.Default.ShoppingCart, Modifier.weight(1f))
            }
        }
        
        item { Text("Revenue Trend (7 Days)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                val revForChart = kpiEngine.weeklyRevenueForChart().map { 
                    com.retailiq.datasage.ui.components.DateRevenuePair(it.date, it.revenue.toDouble())
                }
                RevenueLineChart(data = revForChart, modifier = Modifier.padding(16.dp).testTag("RevenueLineChart"))
            }
        }
        
        val alerts = dto.alertsOpen ?: emptyList()
        if (alerts.isNotEmpty()) {
            item { Text("Open Alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(alerts) { alert ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(alert.message ?: "Alert", color = MaterialTheme.colorScheme.onErrorContainer)
                        Text("Priority: ${alert.priority}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}
