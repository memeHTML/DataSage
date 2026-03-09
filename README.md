# DataSage — Android Client for RetailIQ

DataSage is the Android companion app for the **RetailIQ** backend platform. It provides offline-capable, real-time business intelligence and operations management tools for retail store owners and staff.

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         DataSage Android                        │
│                                                                 │
│  ┌──────────┐   ┌──────────────┐   ┌────────────────────────┐  │
│  │   Compose│──▶│  ViewModels  │──▶│      Repositories      │  │
│  │    UI    │◀──│  (StateFlow) │   │   (Network + Cache)    │  │
│  └──────────┘   └──────────────┘   └──────────┬─────────────┘  │
│                                               │                 │
│                                    ┌──────────▼─────────────┐  │
│                                    │  Retrofit API Services  │  │
│                                    │  (22 service interfaces)│  │
│                                    └──────────┬─────────────┘  │
└───────────────────────────────────────────────┼─────────────── ┘
                                                │ HTTP (Bearer JWT)
                                     ┌──────────▼──────────────┐
                                     │  RetailIQ Flask Backend  │
                                     │  api/v1/...              │
                                     └─────────────────────────┘
```

### Key Design Decisions
- **MVVM + Repository pattern**: ViewModels expose `StateFlow`, Repositories handle all network/cache logic.
- **Hilt DI**: All ViewModels, Repositories, and API Services are injected via Hilt.
- **`ApiResponse<T>` wrapper**: All Retrofit calls return `ApiResponse<T>` which wraps `success`, `data`, `error`, and `meta`. Repositories unwrap these into `Result<T>` or `NetworkResult<T>`.
- **Offline caching**: `InventoryRepository` caches product lists in memory. `OfflineApiService` handles SQLite-backed event queuing.
- **Connectivity observer**: `ConnectivityObserver` wraps `NetworkCallback` and exposes a `StateFlow<Boolean>` consumed by the `OfflineBanner` composable.

---

## Module Map

| Module | Backend Route Prefix | Frontend API Service | Repository | ViewModel(s) |
|---|---|---|---|---|
| Auth | `/api/v1/auth` | `AuthApiService` | — | — |
| Inventory | `/api/v1/inventory` | `InventoryApiService` | `InventoryRepository` | `InventoryViewModel`, `InventoryAuditViewModel`, `ProductDetailViewModel` |
| Suppliers / PO | `/api/v1/suppliers`, `/api/v1/purchase-orders` | `SupplierApiService` | `SupplierRepository` | `SupplierViewModel`, `PurchaseOrderViewModel` |
| GST | `/api/v1/gst` | `GstApiService` | `GstRepository` | `GstConfigViewModel`, `HsnSearchViewModel`, `GstReportsViewModel` |
| Analytics | `/api/v1/analytics` | `AnalyticsApiService` | — | `AnalyticsViewModel` |
| Customers | `/api/v1/customers` | `CustomerApiService` | `CustomerRepository` | `CustomersViewModel`, `CustomerProfileViewModel` |
| Transactions | `/api/v1/transactions` | `TransactionApiService` | — | `SalesViewModel` |
| Pricing | `/api/v1/pricing` | `PricingApiService` | — | `PricingViewModel` |
| Staff | `/api/v1/staff-performance` | `StaffApiService` | — | `StaffViewModel` |
| Loyalty | `/api/v1/loyalty` | `LoyaltyApiService` | — | `LoyaltyViewModel` |
| Alerts | `/api/v1/alerts` | `AlertsApiService` | — | `AlertsViewModel` |
| WhatsApp | `/api/v1/whatsapp` | `WhatsAppApiService` | — | `WhatsAppViewModel` |
| Receipts | `/api/v1/receipts` | `ReceiptsApiService` | — | `ReceiptsViewModel` |
| Chain | `/api/v1/chain` | `ChainApiService` | — | `ChainViewModel` |
| NLP Query | `/api/v1/nlp` | `NlpQueryApiService` | — | `NlpQueryViewModel` |
| Vision/OCR | `/api/v1/vision` | `VisionApiService` | — | `VisionViewModel` |
| Foreasts | `/api/v1/forecasting` | `ForecastApiService` | — | `ForecastViewModel` |
| Events | `/api/v1/events` | `EventApiService` | `EventRepository` | `EventCalendarViewModel` |
| Offline Sync | `/api/v1/sync` | `OfflineApiService` | — | `SyncStatusViewModel` |

---

## API Response Format

All backend endpoints use the `format_response` utility which returns:

```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "meta": { "page": 1, "total": 100 }
}
```

This maps directly to `ApiResponse<T>` in the frontend:

```kotlin
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: ApiError?,
    val meta: Meta?
)
```

---

## Known API Mappings (Backend → Frontend DTO)

### GST Module
| Backend JSON Key | Frontend DTO Field |
|---|---|
| `is_gst_enabled` | `GstConfigDto.isGstEnabled` |
| `registration_type` | `GstConfigDto.registrationType` |
| `state_code` | `GstConfigDto.stateCode` |
| `total_taxable` | `GstSummaryDto.totalTaxable` |
| `invoice_count` | `GstSummaryDto.invoiceCount` |
| `default_gst_rate` (HSN) | `HsnDto.default_rate` |
| `rate` (slab) | `GstSlabDto.rate` |
| `taxable_value` (slab) | `GstSlabDto.taxableValue` |
| `tax_amount` (slab) | `GstSlabDto.taxAmount` |

### Supplier / PO Module
| Backend JSON Key | Frontend DTO Field |
|---|---|
| `contact.name` | `SupplierProfileDto.contact.name` |
| `contact.phone` | `SupplierProfileDto.contact.phone` |
| `analytics.avg_lead_time_days` | `SupplierProfileDto.analytics.avgLeadTimeDays` |
| `analytics.fill_rate_90d` | `SupplierProfileDto.analytics.fillRate90d` |
| `fill_rate_90d` (list item) | `SupplierDto.fillRate90d` |
| `expected_delivery_date` | `PurchaseOrderDto.expectedDeliveryDate` |
| POST create supplier → `{ "id": "uuid" }` | `Result<String>` |
| POST create PO → `{ "id": "uuid" }` | `Result<String>` |

### Inventory Module
| Backend JSON Key | Frontend DTO Field |
|---|---|
| `product_id` | `Product.productId` |
| `sku_code` | `Product.skuCode` |
| `cost_price` | `Product.costPrice` |
| `selling_price` | `Product.sellingPrice` |
| `current_stock` | `Product.currentStock` |
| `reorder_level` | `Product.reorderLevel` |
| `is_active` | `Product.isActive` |
| `lead_time_days` | `Product.leadTimeDays` |

---

## Navigation Map

```
── Bottom Nav (5 tabs) ────────────────────────────────
home              → DashboardScreen
sales             → SalesScreen
inventory         → InventoryScreen
  inventory/add   → ProductFormScreen
  inventory/{id}  → ProductDetailScreen
  inventory/audit → InventoryAuditScreen
  inventory/ocr/{id} → OcrReviewScreen
analytics         → AnalyticsScreen
more              → SettingsScreen

── Settings / More ────────────────────────────────────
customers         → CustomersScreen
  customers/{id}  → CustomerProfileScreen
suppliers         → SupplierListScreen
  suppliers/{id}  → SupplierProfileScreen
alerts            → AlertsScreen
staff/performance → StaffPerformanceScreen
settings/loyalty  → LoyaltySettingsScreen
pricing/suggestions → PricingSuggestionsScreen
forecast          → ForecastScreen
events            → EventCalendarScreen
nlp               → NlpQueryScreen
settings/gst      → GstConfigScreen
reports/gst       → GstReportsScreen
settings/whatsapp → WhatsAppConfigScreen
  settings/whatsapp/log → WhatsAppLogScreen
chain/dashboard   → ChainDashboardScreen (CHAIN_OWNER only)

── Purchase Orders ────────────────────────────────────
purchase-orders   → PurchaseOrderListScreen
  purchaseorders/create → CreatePurchaseOrderScreen
  purchase-orders/{id}/receive → GoodsReceiptScreen
```

---

## CI/CD — GitHub Actions

| Workflow | Trigger | Jobs |
|---|---|---|
| `ci.yml` | Push to `main`/`develop`, PRs to `main` | Lint → Build → Unit Tests (with JUnit report) |
| `release.yml` | Tag `v*` | Test → Build release APK → GitHub Release |
| `code-quality.yml` | PRs to `main` | Detekt (optional) + APK size report |
| `build-apk.yml` | Manual Workflow Dispatch | Builds either Debug or Release APK on-demand |

All workflows use JDK 17 (Temurin), Gradle caching via `gradle/actions/setup-gradle@v4`, and artifact uploads.

---

## Developer Guide

### Prerequisites
- **Android Studio** Hedgehog or later with **JDK 17 or 21** (Android Studio's bundled JBR recommended)
- **Gradle** 8.11.1 (wrapper included)
- **Android Gradle Plugin** 8.10.0
- **Kotlin** 2.1.0
- **Hilt** 2.55 (Latest stable for AGP 8.x)
- **RetailIQ backend** running locally (default: `http://10.0.2.2:5000/`)

> [!NOTE]
> Hilt 2.59+ requires AGP 9.0.0. To maintain compatibility with AGP 8.10.0, this project uses Hilt 2.55.
- **RetailIQ backend** running locally (default: `http://10.0.2.2:5000/`)

### Configuration

`gradle.properties`:
```properties
org.gradle.java.home=C:\\Program Files\\Android\\Android Studio\\jbr
API_BASE_URL=http://10.1.0.2:5000/
```

> **Note:** JDK 25+ is NOT compatible with Android Gradle Plugin. Use JDK 17–21 only.

### Building

```bash
./gradlew compileDebugKotlin   # Verify all sources compile
./gradlew assembleDebug        # Build debug APK
./gradlew assembleRelease      # Build signed release APK
```

### Release APK Signing

The project is configured to sign release APKs using a keystore.

#### Local Development
1. Create a `keystore.properties` file in the project root:
   ```properties
   storePassword=your_password
   keyPassword=your_password
   keyAlias=your_alias
   storeFile=your_keystore.jks
   ```
2. Place your `.jks` file in the `app/` directory (or update the path in `storeFile`).
3. These files are gitignored for security.

#### CI/CD (GitHub Actions)
Add the following as **GitHub Repository Secrets**:
- `RELEASE_STORE_PASSWORD` or `KEYSTORE_PASSWORD`
- `RELEASE_KEY_ALIAS` or `KEY_ALIAS`
- `RELEASE_KEY_PASSWORD` or `KEY_PASSWORD`
- `RELEASE_SIGNING_KEY` (Base64 encoded keystore file)
The keystore file is currently stored in `app/release-keystore.jks` (placeholder generated for initial setup) and used by `.github/workflows/release.yml`. The exact values for these credentials can be found tracked in the `keystore.properties` file at the root. For on-demand release builds, `.github/workflows/build-apk.yml` can be dispatched with the environment variables. For production, you should securely upload your production keystore.

> [!NOTE]
> The release signing configuration in `build.gradle.kts` evaluates whether `release-keystore.jks` exists and is a valid file (>0 bytes). If the Keystore is not found (like when Secrets are missing in CI), the signing configuration gracefully falls back to building an `app-release-unsigned.apk` rather than failing the build.

### Running Unit Tests

```bash
./gradlew testDebugUnitTest --no-configuration-cache
```

Test results: `app/build/test-results/testDebugUnitTest/`

### Project Structure

```
app/src/main/java/com/retailiq/datasage/
├── data/
│   ├── api/          # 22 Retrofit service interfaces + ApiResponse<T> wrapper
│   ├── model/        # DTO data classes (mapped from backend JSON)
│   └── repository/   # Business logic + caching + error handling
├── ui/
│   ├── navigation/   # AppNav.kt — NavHost with all composable routes
│   ├── viewmodel/    # HiltViewModels — one per feature area
│   ├── dashboard/    # Home screen
│   ├── inventory/    # Product list, detail, audit, add form, HSN search
│   ├── supplier/     # Supplier list + profile
│   ├── purchaseorder/ # PO list, create, goods receipt
│   ├── settings/     # GST config, WhatsApp, Loyalty, general settings
│   ├── reports/      # GST reports screen
│   ├── staff/        # Staff performance
│   ├── customers/    # Customer list + profile
│   ├── analytics/    # Analytics dashboard
│   ├── sales/        # Sales / POS screen
│   ├── alerts/       # Inventory alerts
│   ├── events/       # Events placeholder screen
│   └── common/       # Shared composables (OfflineBanner, etc.)
└── core/             # ConnectivityObserver, BaseViewModel helpers
```

### Adding a New API Endpoint

1. Add the method to the relevant `*ApiService.kt` interface (use `ApiResponse<YourDto>` as return type)
2. Add the DTO to `data/model/YourModels.kt` matching backend JSON keys via `@SerializedName`
3. Add a repository method in `*Repository.kt` that unwraps `ApiResponse` → `Result<T>` / `NetworkResult<T>`
4. Expose it from the relevant ViewModel via `StateFlow`
5. Consume in the Compose screen

### Error Handling Pattern

```kotlin
// Repository layer
return try {
    val response = api.someCall()
    if (response.success && response.data != null) {
        NetworkResult.Success(response.data)
    } else {
        NetworkResult.Error(0, response.error?.message ?: "Unknown error")
    }
} catch (e: SocketTimeoutException) {
    NetworkResult.Error(408, "Request timed out. Please try again.")
} catch (e: Exception) {
    Timber.e(e, "API error")
    NetworkResult.Error(500, e.message ?: "Unexpected error")
}
```

---

## Recent Changes (2026-03-08)

### Android CI/CD Keystore Fix (2026-03-08)
- **PR Build Failure Fixed**: Resolved `kspReleaseKotlin` and `validateSigningRelease` execution failures occurring on Pull Request workflow triggers.
  - Modified `.github/workflows/android.yml` to skip decoding `RELEASE_SIGNING_KEY` and running `assembleRelease` when `github.event_name == 'pull_request'` since PRs from forks do not have pipeline access to repository secrets. 
  - Changed `./gradlew lint` to `./gradlew lintDebug` to prevent the Gradle linting task from evaluating the release signing config and failing on a missing or empty `release-keystore.jks`.

### Complete Production Pass (2026-03-08)
- **Events Feature Complete**: Wired the functional `EventCalendarScreen` into `AppNav.kt`, removing the dead "Coming soon" placeholder screen.
- **Customers Feature Complete**: Created `CustomerRepository` and `CustomersViewModel` to fetch the real customer list from the backend. Replaced the "Coming in next release" placeholder `CustomersScreen` with a fully functional `LazyColumn` featuring pull-to-refresh. Unit tested the ViewModel state management.
- **WhatsApp Logs Polish**: Implemented the Material 3 `PullToRefreshBox` API for the `WhatsAppLogScreen` based on `ExperimentalMaterial3Api`, resolving a pending TODO.
- **Rules Polish**: Removed dormant XML comments and stubs scattered throughout the codebase.

### Major AGP Stable Migration (2026-03-08)
- **AGP Downgrade (9.0.0 → 8.10.0)**: Migrated to the latest stable 8.x release to resolve fundamental incompatibilities between KSP, legacy plugins, and AGP 9.0's new flags.
  - Eliminated `android.builtInKotlin=false` and `android.newDsl=false` workarounds.
  - Aligned Kotlin to 2.2.0 and Gradle to 8.11.1 for a supported stability matrix.
- **Release APK Signing**: Corrected building configuration for signed release APKs.
  - Implemented secure property loading via `keystore.properties` or environment variables.
  - Updated GitHub Actions CI/CD to produce signed release artifacts using GitHub Secrets.
  - Automated JDK 21 (JBR) path resolution via `gradle.properties`.
- **CI/CD Pipeline Setup**: Created `.github/workflows/android.yml` to automate Gradle builds, Lint checks, and Unit Tests via GitHub Actions on push/PR.
- **Setup Wizard Overhaul**: Implemented an elegant 4-step Material 3 onboarding flow for new users.
- **Email OTP Authentication**: Added email validation and registration fields aligned with the latest RetailIQ standards.

### Frontend Polish (2026-03-05)
- **Bottom nav reduced to 5 tabs**: Home, Sales, Inventory, Analytics, More. Suppliers and Pricing moved to Settings/More.
- **SettingsScreen reorganized**: Grouped into sections (Quick Actions, Business, Reports, Integrations, System) with distinct per-feature icons and subtitles.
- **NLP Query screen**: New chat-style AI query screen with `NlpQueryViewModel` consuming `/api/v1/query/` endpoint.
- **Forecast screen wired**: Added navigation route and back button to `ForecastScreen`.
- **OCR review route fixed**: `inventory/ocr/{jobId}` now renders `OcrReviewScreen` instead of `InventoryScreen`.
- **Alert dismiss**: Backend `DELETE /api/v1/inventory/alerts/<id>` + optimistic UI removal with close button.
- **Loyalty 404 handled**: Returns default empty settings instead of error when no program configured.
- **Staff performance deserialization fixed**: `StaffApiService.getDailyPerformance` return type corrected to `ApiResponse<List<...>>`.
- **Sales screen refresh fixed**: `DisposableEffect` + `LifecycleEventObserver` ensures product reload on every `ON_RESUME`.

### Bug Fixes (2026-03-02)
- **More button crash**: Added missing `composable("events")` route in `AppNav.kt`. Navigation to `EventsScreen` now works.
- **Add product does nothing**: Wired `ProductFormScreen`'s `onSaveProduct` callback through `InventoryViewModel.createProduct()` → `InventoryRepository.createProduct()` → `InventoryApiService.createProduct()`. Product is now persisted to backend.
- **GST HSN search network error**: Fixed `GstApiService.searchHsn` return type from `ApiResponse<HsnSearchResponse>` (wrapper object) to `ApiResponse<List<HsnDto>>` to match the backend's flat list response. Fixed `GstRepository.searchHsn` to use `response.data` directly instead of `response.data.results`.
- **GST liability slabs network error**: Same fix — `getLiabilitySlabs` now returns `ApiResponse<List<GstSlabDto>>` (no wrapper).
- **GST model field name mismatches**: All `GstConfigDto`, `GstSummaryDto`, and `GstSlabDto` fields renamed to camelCase to match backend JSON (`is_gst_enabled` → `isGstEnabled`, `total_taxable` → `totalTaxable`, etc.).
- **Supplier crash on click**: Fixed nested DTO access in `SupplierProfileScreen` (`profile.contact.name`, `profile.analytics.fillRate90d`) and `PurchaseOrderViewModel` (`contact?.phone`).
- **PO goods receipt crash**: Fixed `GoodsReceiptItemRequest` field: `poItemId` → `productId`, `orderedQty` as `Double`.
- **Settings Screen crash**: Fixed receipt template DTO deserialization mismatch (`header_text`, `footer_text`, `paper_width_mm`) by updating `ReceiptModels.kt` annotations and wrapping `ReceiptsApiService.kt` returns in `ApiResponse<T>`. Fixed `SettingsScreen.kt` `NullPointerException`s via `.orEmpty()`.
- **Newly added products missing**: Fixed by implementing `LifecycleResumeEffect(Lifecycle.State.RESUMED)` in both `InventoryScreen.kt` and `SalesScreen.kt` to auto-trigger `loadProducts()` whenever navigating back to them. Made `SalesViewModel.loadProducts()` public.
- **Sales sync failing and stock not updating**: Fixed backend schema validation rejection by stripping unknown `loyalty_` map keys from `SalesViewModel.submitSale()` payload map. Added local-first deduction inside `InventoryRepository.deductStockLocally(soldQuantities)` to explicitly override `cachedProducts` and immediately reflect fresh stock values in the UI.
- **Infinite pending sync & missing inventory additions**: Added data-sanitization middleware in `SyncTransactionsWorker.doWork()` to strip invalid `loyalty_points_redeemed` and `loyalty_discount_amount` fields from SQLite JSON payloads before batch sync. Added `resetAllFailedToPending()` DAO method called at start of each sync run to rescue transactions that previously exceeded `MAX_RETRIES`. Wired `InventoryRepository.updateStock()` to update `cachedProducts` with the new stock count from the API response.
- **Sales sync unreachable backend**: Fixed `API_BASE_URL` in `gradle.properties` from `http://10.1.0.2:5000/` (wrong) to `http://10.0.2.2:5000/` (correct Android emulator host loopback alias). This was the root cause of all sync failures — no HTTP requests could reach the backend.
- **Token expires after 2 hours**: Increased access token TTL from `timedelta(hours=2)` to `timedelta(hours=24)` in backend `generate_access_token()`. Refresh token (30 days) handles long-term persistence.
- **BatchTransactionResponse DTO mismatch**: Fixed field names from `succeeded`/`failed`/`total` to `accepted`/`rejected` to match backend `process_batch_transactions()` response.
- **Payment mode case mismatch (400 VALIDATION_ERROR)**: `SalesScreen.kt` initialized `selectedPaymentMode = "cash"` (lowercase). Backend `TransactionCreateSchema` validates `OneOf(['CASH', 'UPI', 'CARD', 'CREDIT'])`. Fixed with `.uppercase()` in `SalesViewModel.submitSale()` and normalization in both `TransactionRepository.trySyncNow()` and `SyncTransactionsWorker` for existing stuck records.
- **Direct sync bypass**: Added `trySyncNow()` in `TransactionRepository` that directly calls `createTransactionBatch()` via Retrofit immediately after each sale, bypassing WorkManager. This ensures instant sync when online, with WorkManager as a background fallback.
