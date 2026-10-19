package com.sportify.app.presentation.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(vm: ProfileViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val user = ui.user
    var name by remember(user?.name) { mutableStateOf(user?.name.orEmpty()) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(vm::uploadPhoto) }

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Profile", fontWeight = FontWeight.Bold) }) }) { pad ->
        Column(Modifier.padding(pad).padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(112.dp).clip(CircleShape).clickable {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }, contentAlignment = Alignment.Center) {
                if (!user?.photoUrl.isNullOrBlank())
                    AsyncImage(user?.photoUrl, "Profile photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxSize()) {
                    Icon(Icons.Default.Person, null, Modifier.padding(28.dp))
                }
                Surface(color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f), modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(28.dp)) {
                    Icon(Icons.Default.CameraAlt, null, Modifier.padding(4.dp), tint = MaterialTheme.colorScheme.surface)
                }
            }
            if (ui.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(user?.email.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true,
                keyboardOptions = KeyboardOptions.Default, modifier = Modifier.fillMaxWidth())
            Button({ vm.saveName(name) }, Modifier.fillMaxWidth(), enabled = name.isNotBlank() && name != user?.name && !ui.busy) {
                Text("Save changes")
            }
            ui.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.weight(1f))
            OutlinedButton(vm::signOut, Modifier.fillMaxWidth()) { Text("Sign out") }
        }
    }
}
