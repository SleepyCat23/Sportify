package com.sportify.app.presentation.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.sportify.app.R
import com.sportify.app.presentation.common.ErrorText
import com.sportify.app.presentation.common.LinkText
import com.sportify.app.presentation.common.OutlineButton
import com.sportify.app.presentation.common.PrimaryButton
import com.sportify.app.presentation.theme.Green

/** Green hero panel (from the web design's left column) stacked above the form. */
@Composable
private fun AuthScaffold(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()).imePadding()
    ) {
        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(Green).statusBarsPadding().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.SportsTennis, null, tint = Color.White)
                Text("Sportify", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(20.dp))
            Text("Book courts and pitches in seconds.", color = Color.White,
                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Tennis, padel, basketball, football and more, at venues near you.",
                color = Color.White.copy(alpha = 0.92f), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text("Slots are held for 10 minutes while you pay.",
                color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium)
        }
        Column(Modifier.padding(24.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun LabeledField(
    label: String, value: String, onChange: (String) -> Unit,
    keyboard: KeyboardType = KeyboardType.Text, password: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value = value, onValueChange = onChange, singleLine = true,
            shape = RoundedCornerShape(12.dp),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White, unfocusedContainerColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun LoginScreen(onRegister: () -> Unit, vm: AuthViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var email by remember { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }
    val context = LocalContext.current

    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        try {
            val acc = GoogleSignIn.getSignedInAccountFromIntent(res.data).getResult(ApiException::class.java)
            acc.idToken?.let(vm::google) ?: vm.setError("Google sign-in returned no token")
        } catch (e: ApiException) {
            if (e.statusCode != 12501) vm.setError("Google sign-in failed (${e.statusCode})") // 12501 = cancelled
        }
    }

    AuthScaffold("Welcome to Sportify", "Sign in to book your next game.") {
        LabeledField("Email", email, { email = it }, KeyboardType.Email)
        LabeledField("Password", pw, { pw = it }, KeyboardType.Password, password = true)
        LinkText("Forgot password?", { vm.resetPassword(email) }, Modifier.align(Alignment.End))
        ErrorText(ui.error)
        ui.info?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        PrimaryButton("Sign in", { vm.signIn(email, pw) }, Modifier.fillMaxWidth(), loading = ui.loading)
        OutlineButton(
            "Continue with Google",
            onClick = {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(R.string.default_web_client_id))
                    .requestEmail().build()
                val client = GoogleSignIn.getClient(context, gso)
                client.signOut().addOnCompleteListener { googleLauncher.launch(client.signInIntent) }
            },
            modifier = Modifier.fillMaxWidth(), enabled = !ui.loading
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("New here? ", color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinkText("Create an account", onRegister)
        }
    }
}

@Composable
fun RegisterScreen(onBack: () -> Unit, vm: AuthViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }

    AuthScaffold("Create account", "Join Sportify and start playing.") {
        LabeledField("Full name", name, { name = it })
        LabeledField("Email", email, { email = it }, KeyboardType.Email)
        LabeledField("Password (min 6 characters)", pw, { pw = it }, KeyboardType.Password, password = true)
        ErrorText(ui.error)
        PrimaryButton("Sign up", { vm.signUp(name, email, pw) }, Modifier.fillMaxWidth(), loading = ui.loading)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("Already have an account? ", color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinkText("Sign in", onBack)
        }
    }
}
