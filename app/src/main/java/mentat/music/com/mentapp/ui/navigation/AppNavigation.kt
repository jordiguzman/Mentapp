package mentat.music.com.mentapp.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import mentat.music.com.mentapp.ui.screens.detail.AlbumDetailScreen
import mentat.music.com.mentapp.ui.screens.home.HomeScreen
import mentat.music.com.mentapp.ui.screens.home.viewmodel.HomeViewModel
import mentat.music.com.mentapp.ui.screens.music.MusicOverviewScreen
import mentat.music.com.mentapp.ui.screens.webview.WebViewScreen
import mentat.music.com.mentapp.utils.MentatConstants.ANIM_DURATION

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    // 700ms es un tiempo muy "cinemático" para este efecto de profundidad


    SharedTransitionLayout {

        NavHost(
            navController = navController,
            // CAMBIO CLAVE: Arrancamos directo en la HOME
            // El efecto de apertura lo hace el MeliesShutter en MainActivity
            startDestination = AppScreens.HomeScreen.route
        ) {

            // --- RUTA 1: HomeScreen ---
            composable(
                route = AppScreens.HomeScreen.route,
                // Al volver a casa: Aparece fundiéndose
                enterTransition = {
                    fadeIn(animationSpec = tween(ANIM_DURATION))
                },
                // Al irnos de casa: Se oscurece y se va al fondo (escala 0.9)
                exitTransition = {
                    fadeOut(animationSpec = tween(ANIM_DURATION)) +
                            scaleOut(targetScale = 0.9f, animationSpec = tween(ANIM_DURATION))
                },
                // Al volver atrás desde otra pantalla: Aparece desde el fondo creciendo
                popEnterTransition = {
                    fadeIn(animationSpec = tween(ANIM_DURATION)) +
                            scaleIn(initialScale = 0.9f, animationSpec = tween(ANIM_DURATION))
                }
            ) {
                val homeViewModel: HomeViewModel = viewModel()
                HomeScreen(
                    navController = navController,
                    homeViewModel = homeViewModel
                )
            }

            // --- RUTA 2: MusicOverviewScreen ---
            composable(
                route = AppScreens.MusicOverviewScreen.route,
                enterTransition = {
                    fadeIn(animationSpec = tween(ANIM_DURATION)) +
                            scaleIn(initialScale = 0.95f, animationSpec = tween(ANIM_DURATION))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(ANIM_DURATION)) +
                            scaleOut(targetScale = 0.9f, animationSpec = tween(ANIM_DURATION))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(ANIM_DURATION))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(ANIM_DURATION)) +
                            scaleOut(targetScale = 0.9f, animationSpec = tween(ANIM_DURATION))
                }
            ) {
                MusicOverviewScreen(navController = navController)
            }

            // --- RUTA 3: AlbumDetailScreen ---
            composable(
                route = AppScreens.AlbumDetailScreen.route,
                enterTransition = {
                    fadeIn(animationSpec = tween(ANIM_DURATION)) +
                            scaleIn(initialScale = 0.95f, animationSpec = tween(ANIM_DURATION))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(ANIM_DURATION))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(ANIM_DURATION)) +
                            scaleOut(targetScale = 0.9f, animationSpec = tween(ANIM_DURATION))
                }
            ) { backStackEntry ->
                val albumId = backStackEntry.arguments?.getString("albumId")
                AlbumDetailScreen(navController = navController, albumId = albumId)
            }

            // --- RUTA 4: WebViewScreen ---
            composable(
                route = AppScreens.WebViewScreen.route,
                // Aceptamos argumentos por si acaso
                arguments = listOf(navArgument("url") { type = NavType.StringType }),

                // Transición: Aparece creciendo suavemente (efecto inmersivo)
                enterTransition = {
                    fadeIn(animationSpec = tween(ANIM_DURATION)) +
                            scaleIn(initialScale = 0.95f, animationSpec = tween(ANIM_DURATION))
                },
                // Al salir: Se desvanece
                exitTransition = {
                    fadeOut(animationSpec = tween(ANIM_DURATION))
                },
                // Al dar atrás: Se encoge y desvanece
                popExitTransition = {
                    fadeOut(animationSpec = tween(ANIM_DURATION)) +
                            scaleOut(targetScale = 0.9f, animationSpec = tween(ANIM_DURATION))
                }
            ) { backStackEntry ->
                val encodedUrl = backStackEntry.arguments?.getString("url")
                val url = encodedUrl?.let {
                    java.net.URLDecoder.decode(it, "UTF-8")
                }
                WebViewScreen(navController = navController, url = url)
            }
        }
    }
}