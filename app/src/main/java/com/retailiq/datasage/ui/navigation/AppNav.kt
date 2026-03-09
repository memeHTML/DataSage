package com.retailiq.datasage.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.retailiq.datasage.core.ConnectivityObserver
import com.retailiq.datasage.ui.alerts.AlertsScreen
import com.retailiq.datasage.ui.analytics.AnalyticsScreen
import com.retailiq.datasage.ui.common.OfflineBanner
import com.retailiq.datasage.ui.customers.CustomersScreen
import com.retailiq.datasage.ui.dashboard.DashboardScreen
import com.retailiq.datasage.ui.inventory.InventoryScreen
import com.retailiq.datasage.ui.inventory.ProductDetailScreen
import com.retailiq.datasage.ui.inventory.InventoryAuditScreen
import com.retailiq.datasage.ui.pricing.PricingSuggestionsScreen
import com.retailiq.datasage.ui.sales.SalesScreen
import com.retailiq.datasage.ui.settings.SettingsScreen

@Composable
fun MainNavigation(
    role: UserRole,
    connectivityObserver: ConnectivityObserver,
    isChainOwner: Boolean = false,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val tabs = tabsForRole(role, isChainOwner)
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route
    val isOnline by connectivityObserver.isOnline.collectAsState(initial = true)

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            OfflineBanner(isOffline = !isOnline)
            NavHost(navController, startDestination = "home") {
                composable("home") { DashboardScreen(role = role) }
                composable("sales") { SalesScreen() }
                composable("inventory") {
                    InventoryScreen(
                        onNavigateToAddProduct = { navController.navigate("inventory/add") },
                        onNavigateToProduct = { id -> navController.navigate("inventory/$id") },
                        onNavigateToOcrReview = { jobId -> navController.navigate("inventory/ocr/$jobId") },
                        onNavigateToAudit = { navController.navigate("inventory/audit") }
                    )
                }
                composable("inventory/audit") {
                    InventoryAuditScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("inventory/{productId}") { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("productId")?.toIntOrNull() ?: return@composable
                    ProductDetailScreen(
                        productId = id,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("inventory/add") {
                    val inventoryVm: com.retailiq.datasage.ui.inventory.InventoryViewModel = hiltViewModel()
                    com.retailiq.datasage.ui.inventory.ProductFormScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onSaveProduct = { name, costPrice, sellingPrice, hsnCode, gstRate ->
                            inventoryVm.createProduct(
                                name = name,
                                costPrice = costPrice,
                                sellingPrice = sellingPrice,
                                hsnCode = hsnCode,
                                gstRate = gstRate
                            )
                            navController.popBackStack()
                        }
                    )
                }
                composable("analytics") {
                    AnalyticsScreen(
                        onNavigateToGstReports = { navController.navigate("reports/gst") }
                    )
                }

                // ── Suppliers & Purchase Orders ─────────────────────────
                composable("suppliers") {
                    com.retailiq.datasage.ui.supplier.SupplierListScreen(
                        onNavigateToSupplier = { id -> navController.navigate("suppliers/$id") }
                    )
                }
                composable("suppliers/{supplierId}") { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("supplierId") ?: return@composable
                    com.retailiq.datasage.ui.supplier.SupplierProfileScreen(
                        supplierId = id,
                        onNavigateBack = { navController.popBackStack() },
                        onCreatePo = { supId -> navController.navigate("purchaseorders/create?supplierId=$supId") },
                        onViewAllPos = { supId -> navController.navigate("purchase-orders?supplierId=$supId") }
                    )
                }
                composable("purchase-orders?supplierId={supplierId}") { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("supplierId")
                    com.retailiq.datasage.ui.purchaseorder.PurchaseOrderListScreen(
                        supplierId = id,
                        onNavigateBack = { navController.popBackStack() },
                        onCreatePo = { supId ->
                            val query = if (supId != null) "?supplierId=$supId" else ""
                            navController.navigate("purchaseorders/create$query")
                        },
                        onNavigateToReceive = { poId -> navController.navigate("purchase-orders/$poId/receive") }
                    )
                }
                composable("purchaseorders/create?supplierId={supplierId}&prefillProductId={prefillProductId}") { backStackEntry ->
                    val supId = backStackEntry.arguments?.getString("supplierId")
                    val prodId = backStackEntry.arguments?.getString("prefillProductId")?.toIntOrNull()
                    com.retailiq.datasage.ui.purchaseorder.CreatePurchaseOrderScreen(
                        prefillSupplierId = supId,
                        prefillProductId = prodId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("purchase-orders/{poId}/receive") { backStackEntry ->
                    val poId = backStackEntry.arguments?.getString("poId") ?: return@composable
                    com.retailiq.datasage.ui.purchaseorder.GoodsReceiptScreen(
                        poId = poId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // ── Settings / More ─────────────────────────────────────
                composable("more") {
                    SettingsScreen(
                        userRole = role.name,
                        onNavigateToStaffPerformance = { navController.navigate("staff/performance") },
                        onNavigateToLoyaltySettings = { navController.navigate("settings/loyalty") },
                        onNavigateToGstSettings = { navController.navigate("settings/gst") },
                        onNavigateToWhatsAppSettings = { navController.navigate("settings/whatsapp") },
                        onNavigateToPricing = { navController.navigate("pricing/suggestions") },
                        onNavigateToEvents = { navController.navigate("events") },
                        onNavigateToAlerts = { navController.navigate("alerts") },
                        onNavigateToCustomers = { navController.navigate("customers") },
                        onNavigateToSuppliers = { navController.navigate("suppliers") },
                        onNavigateToForecast = { navController.navigate("forecast") },
                        onNavigateToNlpQuery = { navController.navigate("nlp") },
                        onNavigateToGstReports = { navController.navigate("reports/gst") },
                        onLogout = onLogout
                    )
                }
                composable("settings/loyalty") {
                    com.retailiq.datasage.ui.settings.LoyaltySettingsScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("settings/gst") {
                    com.retailiq.datasage.ui.settings.GstConfigScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("reports/gst") {
                    com.retailiq.datasage.ui.reports.GstReportsScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("settings/whatsapp") {
                    com.retailiq.datasage.ui.settings.WhatsAppConfigScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToLog = { navController.navigate("settings/whatsapp/log") }
                    )
                }
                composable("settings/whatsapp/log") {
                    com.retailiq.datasage.ui.settings.WhatsAppLogScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("staff/performance") {
                    com.retailiq.datasage.ui.staff.StaffPerformanceScreen(
                        userRole = role.name,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // ── Customers ───────────────────────────────────────────
                composable("customers") {
                    CustomersScreen(
                        onNavigateToCustomer = { id -> navController.navigate("customers/$id") }
                    )
                }
                composable("customers/{customerId}") { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("customerId")?.toIntOrNull() ?: return@composable
                    com.retailiq.datasage.ui.customers.CustomerProfileScreen(
                        customerId = id,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // ── Alerts ──────────────────────────────────────────────
                composable("alerts") {
                    AlertsScreen(
                        onNavigateToCreatePo = { prodId -> navController.navigate("purchaseorders/create?prefillProductId=$prodId") }
                    )
                }

                // ── Chain (franchise owner) ─────────────────────────────
                composable("chain/dashboard") {
                    com.retailiq.datasage.ui.chain.ChainDashboardScreen(
                        onNavigateToCompare = { navController.navigate("chain/compare") },
                        onNavigateToTransfers = { navController.navigate("chain/transfers") }
                    )
                }
                composable("chain/compare") {
                    com.retailiq.datasage.ui.chain.StoreCompareScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("chain/transfers") {
                    com.retailiq.datasage.ui.chain.ChainTransfersScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // ── Pricing ─────────────────────────────────────────────
                composable("pricing/suggestions") {
                    PricingSuggestionsScreen()
                }

                // ── Events ──────────────────────────────────────────────
                composable("events") {
                    com.retailiq.datasage.ui.events.EventCalendarScreen()
                }

                // ── Forecast ────────────────────────────────────────────
                composable("forecast") {
                    com.retailiq.datasage.ui.forecast.ForecastScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // ── NLP Query ───────────────────────────────────────────
                composable("nlp") {
                    com.retailiq.datasage.ui.nlp.NlpQueryScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // ── OCR Review ──────────────────────────────────────────
                composable("inventory/ocr/{jobId}") { backStackEntry ->
                    val jobId = backStackEntry.arguments?.getString("jobId") ?: return@composable
                    com.retailiq.datasage.ui.inventory.ocr.OcrReviewScreen(
                        jobId = jobId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
