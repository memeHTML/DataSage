package com.retailiq.datasage.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuthNavHost(navController: NavHostController, onFinish: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") { SplashScreen(viewModel) { hasToken, setupComplete -> navController.navigate(if (!hasToken) "login" else if (setupComplete) "done" else "setup") } }
        composable("login") { LoginScreen(viewModel, onRegister = { navController.navigate("register") }, onForgot = { navController.navigate("forgot") }, onLoginSuccess = { setupComplete -> navController.navigate(if (setupComplete) "done" else "setup") }) }
        composable("register") { RegisterScreen(viewModel) { mobile -> navController.navigate("otp/$mobile") } }
        composable("otp/{mobile}") { backStack -> OTPVerifyScreen(viewModel, backStack.arguments?.getString("mobile").orEmpty()) { setupComplete -> navController.navigate(if (setupComplete) "done" else "setup") } }
        composable("forgot") { ForgotPasswordScreen(viewModel) { mobile -> navController.navigate("reset/$mobile") } }
        composable("reset/{mobile}") { backStack -> ResetPasswordScreen(viewModel, backStack.arguments?.getString("mobile").orEmpty()) { navController.navigate("login") } }
        composable("setup") { SetupWizardScreen(viewModel) { navController.navigate("done") } }
        composable("done") { LaunchedEffect(Unit) { onFinish() } }
    }
}

@Composable
fun SplashScreen(viewModel: AuthViewModel, onRoute: (Boolean, Boolean) -> Unit) {
    LaunchedEffect(Unit) {
        delay(500)
        if (viewModel.hasToken()) {
            val valid = viewModel.validateSession()
            onRoute(valid, viewModel.isSetupComplete())
        } else {
            onRoute(false, false)
        }
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Text("Loading DataSage...")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: AuthViewModel, onRegister: () -> Unit, onForgot: () -> Unit, onLoginSuccess: (Boolean) -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    var mobile by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { p ->
        Column(Modifier.fillMaxSize().padding(p).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Login", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(mobile, { mobile = it }, label = { Text("Mobile Number") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(password, { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onForgot) { Text("Forgot Password?") } }
            Button(onClick = {
                if (!AuthValidation.isValidMobile(mobile)) {
                    scope.launch { snackbar.showSnackbar("Enter a valid 10-digit mobile number") }
                } else {
                    viewModel.login(mobile, password) { _ ->
                        onLoginSuccess(viewModel.isSetupComplete())
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Sign in") }
            TextButton(onClick = onRegister) { Text("Create account") }
            if (state is AuthUiState.Error) {
                LaunchedEffect(state) { scope.launch { snackbar.showSnackbar((state as AuthUiState.Error).message) } }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(viewModel: AuthViewModel, onOtp: (String) -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsState()

    var fullName by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var store by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { p ->
        Column(Modifier.fillMaxSize().padding(p).padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Register", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(fullName, { fullName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(mobile, { mobile = it }, label = { Text("Mobile Number") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(email, { email = it }, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(store, { store = it }, label = { Text("Store Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(password, { password = it }, label = { Text("Password (8+ chars, 1+ digit)") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                if (!AuthValidation.isValidMobile(mobile)) {
                    scope.launch { snackbar.showSnackbar("Enter a valid 10-digit mobile number") }
                } else if (!AuthValidation.isValidEmail(email)) {
                    scope.launch { snackbar.showSnackbar("Enter a valid email address") }
                } else if (!AuthValidation.isStrongPassword(password)) {
                    scope.launch { snackbar.showSnackbar("Password must be 8+ chars and contain a digit") }
                } else {
                    viewModel.register(fullName, mobile, email, store, password) { onOtp(mobile) }
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Register") }
            
            if (state is AuthUiState.Error) {
                LaunchedEffect(state) { scope.launch { snackbar.showSnackbar((state as AuthUiState.Error).message) } }
            }
        }
    }
}

@Composable
fun OTPVerifyScreen(viewModel: AuthViewModel, mobile: String, onDone: (Boolean) -> Unit) {
    var otp by remember { mutableStateOf("") }
    val seconds by viewModel.otpSecondsRemaining.collectAsState()
    val resendCount by viewModel.resendCount.collectAsState()
    LaunchedEffect(Unit) { viewModel.startOtpCountdown() }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Verify OTP")
        OutlinedTextField(otp, { if (it.length <= 6) otp = it }, label = { Text("6-digit OTP") })
        Text("Time remaining: ${seconds}s")
        Button(onClick = { viewModel.verifyOtp(mobile, otp) { _ -> onDone(viewModel.isSetupComplete()) } }, enabled = otp.length == 6) { Text("Verify") }
        TextButton(onClick = { viewModel.resendOtp(mobile) }, enabled = viewModel.canResendOtp()) { Text("Resend OTP (${3 - resendCount} left)") }
    }
}

@Composable
fun ForgotPasswordScreen(viewModel: AuthViewModel, onOtpSent: (String) -> Unit) {
    var mobile by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Forgot Password")
        OutlinedTextField(mobile, { mobile = it }, label = { Text("Mobile Number") })
        Button(onClick = { viewModel.forgotPassword(mobile); onOtpSent(mobile) }) { Text("Send OTP") }
    }
}

@Composable
fun ResetPasswordScreen(viewModel: AuthViewModel, mobile: String, onDone: () -> Unit) {
    var resetToken by remember { mutableStateOf("") }
    var pwd by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Reset Password")
        OutlinedTextField(resetToken, { resetToken = it }, label = { Text("Reset Token") })
        OutlinedTextField(pwd, { pwd = it }, label = { Text("New Password") }, visualTransformation = PasswordVisualTransformation())
        Button(onClick = { viewModel.resetPassword(resetToken, pwd) { onDone() } }) { Text("Reset") }
    }
}

@Composable
fun SetupWizardScreen(viewModel: AuthViewModel, onComplete: () -> Unit) {
    var step by remember { mutableStateOf(1) }
    
    when (step) {
        1 -> WizardStoreInfoScreen(viewModel, onNext = { step = 2 })
        2 -> WizardCategoriesScreen(viewModel, onNext = { step = 3 }, onBack = { step = 1 })
        3 -> WizardFirstProductScreen(viewModel, onNext = { step = 4 }, onSkip = { step = 4 }, onBack = { step = 2 })
        4 -> WizardSuccessScreen(viewModel, onFinish = { viewModel.completeSetup(); onComplete() })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WizardStoreInfoScreen(viewModel: AuthViewModel, onNext: () -> Unit) {
    val storeName by viewModel.wizardStoreName.collectAsState()
    val address by viewModel.wizardStoreAddress.collectAsState()
    val businessType by viewModel.wizardBusinessType.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("Grocery", "Electronics", "Apparel", "Pharmacy", "Hardware", "Other")

    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Text("Store Details", style = MaterialTheme.typography.headlineMedium)
        Text("Let's confirm your basic business information.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(storeName, { viewModel.setWizardStoreName(it) }, label = { Text("Store Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(address, { viewModel.setWizardStoreAddress(it) }, label = { Text("Store Address") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
        
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = businessType,
                onValueChange = {},
                readOnly = true,
                label = { Text("Business Type") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = { viewModel.setWizardBusinessType(option); expanded = false }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(50.dp), enabled = storeName.isNotBlank() && address.isNotBlank()) {
            Text("Next: Categories")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WizardCategoriesScreen(viewModel: AuthViewModel, onNext: () -> Unit, onBack: () -> Unit) {
    val selected by viewModel.wizardSelectedCategories.collectAsState()
    val defaultCategories = listOf("Beverages", "Snacks", "Dairy", "Produce", "Bakery", "Meat", "Personal Care", "Cleaning", "Electronics", "Stationery")

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Select Categories", style = MaterialTheme.typography.headlineMedium)
        Text("What types of products do you sell?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        FlowRow(
            modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            defaultCategories.forEach { cat ->
                val isSelected = selected.contains(cat)
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.toggleWizardCategory(cat) },
                    label = { Text(cat) }
                )
            }
        }
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("Back") }
            Button(onClick = onNext, modifier = Modifier.height(50.dp)) { Text("Next: Add Product") }
        }
    }
}

@Composable
fun WizardFirstProductScreen(viewModel: AuthViewModel, onNext: () -> Unit, onSkip: () -> Unit, onBack: () -> Unit) {
    val productName by viewModel.wizardProductName.collectAsState()
    val productPrice by viewModel.wizardProductPrice.collectAsState()
    val productStock by viewModel.wizardProductStock.collectAsState()

    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Text("Add First Product", style = MaterialTheme.typography.headlineMedium)
        Text("Kickstart your inventory by adding your first item.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = productName, 
            onValueChange = { viewModel.setWizardProductName(it) }, 
            label = { Text("Product Name") }, 
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = productPrice, 
                onValueChange = { viewModel.setWizardProductPrice(it) }, 
                label = { Text("Selling Price (₹)") }, 
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = productStock, 
                onValueChange = { viewModel.setWizardProductStock(it) }, 
                label = { Text("Initial Stock") }, 
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
        
        OutlinedTextField(
            value = "", 
            onValueChange = {}, 
            label = { Text("Barcode (Optional)") }, 
            trailingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("Back") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onSkip) { Text("Skip") }
                Button(onClick = onNext, enabled = productName.isNotBlank(), modifier = Modifier.height(50.dp)) { Text("Finish Setup") }
            }
        }
    }
}

@Composable
fun WizardSuccessScreen(viewModel: AuthViewModel, onFinish: () -> Unit) {
    val storeName by viewModel.wizardStoreName.collectAsState()
    
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.CheckCircle, contentDescription = "Success", modifier = Modifier.size(100.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(24.dp))
        Text("You're all set!", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Welcome to RetailIQ, ${storeName.ifBlank { "Store Owner" }}.\nYour dashboard is ready.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Go to Dashboard", style = MaterialTheme.typography.titleMedium)
        }
    }
}
