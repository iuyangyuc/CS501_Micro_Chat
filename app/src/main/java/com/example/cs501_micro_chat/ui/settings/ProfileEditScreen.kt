package com.example.cs501_micro_chat.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.cs501_micro_chat.R
import com.example.cs501_micro_chat.ui.settings.composables.ImageCropDialog
import java.io.File
import kotlinx.coroutines.launch

@Composable
fun ProfileEditScreen(
    onBack: () -> Unit,
    onProfileSaved: () -> Unit = onBack,
    viewModel: ProfileEditViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is ProfileEditEvent.ProfileSaved) {
                onProfileSaved()
            }
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    val context = LocalContext.current
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var showCropDialog by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            showCropDialog = true
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraUri?.let {
                selectedImageUri = it
                showCropDialog = true
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraUri = createImageUri(context)
            cameraUri?.let { cameraLauncher.launch(it) }
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.profile_camera_permission_denied))
            }
        }
    }

    fun startCameraFlow() {
        val permissionStatus = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
            cameraUri = createImageUri(context)
            cameraUri?.let { cameraLauncher.launch(it) }
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            AvatarPreview(
                imageUrl = state.avatarUrl,
                newAvatarUri = state.newAvatarUri,
                onClear = viewModel::clearAvatarSelection
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextButtonWithIcon(
                    label = stringResource(R.string.profile_edit_choose_photo),
                    icon = Icons.Filled.Image,
                    onClick = {
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                )
                OutlinedTextButtonWithIcon(
                    label = stringResource(R.string.profile_edit_take_photo),
                    icon = Icons.Filled.CameraAlt,
                    onClick = { startCameraFlow() }
                )
            }

            OutlinedTextField(
                value = state.displayName,
                onValueChange = viewModel::onDisplayNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.profile_edit_name_label)) },
                singleLine = true
            )

            OutlinedTextField(
                value = state.bio,
                onValueChange = viewModel::onBioChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                label = { Text(stringResource(R.string.profile_edit_bio_label)) },
                supportingText = { Text(stringResource(R.string.profile_edit_bio_helper)) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.saveProfile() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving && state.displayName.isNotBlank()
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(text = stringResource(R.string.profile_edit_save))
            }
        }

        // Show crop dialog when image is selected
        if (showCropDialog && selectedImageUri != null) {
            ImageCropDialog(
                imageUri = selectedImageUri!!,
                onDismiss = {
                    showCropDialog = false
                    selectedImageUri = null
                },
                onCropComplete = { croppedUri ->
                    viewModel.onAvatarSelected(croppedUri)
                    showCropDialog = false
                    selectedImageUri = null
                }
            )
        }
    }
}

@Composable
private fun AvatarPreview(
    imageUrl: String,
    newAvatarUri: Uri?,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    val model = remember(imageUrl, newAvatarUri) {
        newAvatarUri ?: imageUrl.takeIf { it.isNotBlank() }
    }
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surface
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (model == null) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(60.dp)
            )
        } else {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(model)
                    .crossfade(true)
                    .build(),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                contentDescription = stringResource(R.string.profile_edit_avatar_content_description)
            )
            if (newAvatarUri != null) {
                TextButton(
                    onClick = onClear,
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Text(text = stringResource(R.string.profile_edit_remove_avatar))
                }
            }
        }
    }
}

@Composable
private fun OutlinedTextButtonWithIcon(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    OutlinedButton(onClick = onClick) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(modifier = Modifier.size(8.dp))
        Text(text = label)
    }
}

private fun createImageUri(context: android.content.Context): Uri {
    val image = File.createTempFile("avatar_", ".jpg", context.cacheDir)
    val authority = "${context.packageName}.fileprovider"
    return FileProvider.getUriForFile(context, authority, image)
}

