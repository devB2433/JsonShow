package com.jsonshow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jsonshow.ui.import.ImportScreen
import com.jsonshow.ui.viewer.ViewerScreen
import com.jsonshow.viewmodel.JsonViewModel

@Composable
fun JsonShowApp(viewModel: JsonViewModel = viewModel()) {
    val navController = rememberNavController()
    val context = LocalContext.current

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
