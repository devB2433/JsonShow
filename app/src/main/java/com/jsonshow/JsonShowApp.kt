package com.jsonshow

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.jsonshow.auth.GoogleAuthManager
import com.jsonshow.ui.import.ImportScreen
import com.jsonshow.ui.privacy.PrivacyPolicyDialog
import com.jsonshow.ui.viewer.ViewerScreen
import com.jsonshow.viewmodel.JsonViewModel
import kotlinx.coroutines.launch

@Composable
fun JsonShowApp(viewModel: JsonViewModel = viewModel()) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Auth manager
    val authManager = remember { GoogleAuthManager(context) }
    val scope = rememberCoroutineScope()

    // Privacy dialog state
    var showPrivacyDialog by remember { mutableStateOf(false) }

    // Check for existing sign-in on launch
    LaunchedEffect(Unit) {
        viewModel.loadPrivacyState(context)
        authManager.getSignedInAccount(context)?.let {
            viewModel.onSignedIn(it)
        }
    }

    // Google Sign-In launcher
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)
                viewModel.onSignedIn(account)
            } catch (_: ApiException) { /* sign-in failed, ignore */ }
        }
    }

    // Privacy dialog
    if (showPrivacyDialog) {
        PrivacyPolicyDialog(
            onAccept = {
                viewModel.acceptPrivacy(context)
                showPrivacyDialog = false
                signInLauncher.launch(authManager.signInIntent)
            },
            onDecline = { showPrivacyDialog = false }
        )
    }

    NavHost(navController, startDestination = "import") {
        composable("import") {
            LaunchedEffect(Unit) { viewModel.refreshSavedFiles(context) }

            ImportScreen(
                savedFiles = viewModel.savedFiles,
                onJsonLoaded = {
                    viewModel.currentFileName = null
                    viewModel.loadJson(it)
                    navController.navigate("viewer")
                },
                onOpenSaved = { name ->
                    viewModel.loadSaved(context, name)
                    navController.navigate("viewer")
                },
                onDeleteSaved = { name ->
                    viewModel.deleteSaved(context, name)
                },
                isSignedIn = viewModel.googleAccount != null,
                isSyncing = viewModel.isSyncing,
                syncResultText = viewModel.syncResult?.summary,
                onSignInClick = {
                    if (viewModel.privacyAccepted) {
                        signInLauncher.launch(authManager.signInIntent)
                    } else {
                        showPrivacyDialog = true
                    }
                },
                onSyncClick = { viewModel.syncWithDrive(context) },
                onSignOutClick = {
                    viewModel.onSignedOut()
                    scope.launch { authManager.signOut() }
                }
            )
        }
        composable("viewer") {
            ViewerScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSave = { name -> viewModel.saveJson(context, name) }
            )
        }
    }
}
