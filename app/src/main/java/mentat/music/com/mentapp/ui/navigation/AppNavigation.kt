package mentat.music.com.mentapp.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalSharedTransitionApi // <--- NUEVO
import androidx.compose.animation.SharedTransitionLayout // <--- NUEVO
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mentat.music.com.mentapp.ui.screens.detail.AlbumDetailScreen
import mentat.music.com.mentapp.ui.screens.home.HomeScreen
import mentat.music.com.mentapp.ui.screens.home.viewmodel.HomeViewModel
import mentat.music.com.mentapp.ui.screens.music.MusicOverviewScreen
import mentat.music.com.mentapp.ui.screens.splash.SplashScreen
import mentat.music.com.mentapp.ui.screens.webview.WebViewScreen

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalSharedTransitionApi::class) // <--- NECESARIO PARA USAR LA API NUEVA
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val animDuration = 1000

    // --- CAMBIO FASE 1: LA BURBUJA MÁGICA ---
    // Envolvemos todo el sistema de navegación en este Layout.
    // Esto permite que HomeScreen y AlbumDetailScreen "se vean" entre sí.
    SharedTransitionLayout {

        NavHost(
            navController = navController,
            startDestination = AppScreens.SplashScreen.route
        ) {

            composable(AppScreens.SplashScreen.route) {
                SplashScreen(navController = navController)
            }

            // --- RUTA 1: HomeScreen ---
            composable(
                route = AppScreens.HomeScreen.route,
                enterTransition = { fadeIn(animationSpec = tween(animDuration)) },
                exitTransition = { fadeOut(animationSpec = tween(0)) }
            ) {
                val homeViewModel: HomeViewModel = viewModel()

                // NOTA: En el siguiente paso tendremos que pasarle
                // el "scope" de animación a esta pantalla.
                HomeScreen(
                    navController = navController,
                    homeViewModel = homeViewModel
                )
            }

            // --- RUTA 2: MusicOverviewScreen ---
            composable(
                route = AppScreens.MusicOverviewScreen.route,
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(animDuration)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(animDuration)
                    )
                }
            ) {
                MusicOverviewScreen(navController = navController)
            }

            // --- RUTA 3: AlbumDetailScreen ---
            composable(
                route = AppScreens.AlbumDetailScreen.route,
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(animDuration)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(animDuration)
                    )
                }
            ) { backStackEntry ->
                val albumId = backStackEntry.arguments?.getString("albumId")

                // NOTA: Aquí también tendremos que pasar el "scope" más adelante.
                AlbumDetailScreen(navController = navController, albumId = albumId)
            }

            // --- RUTA 4: WebViewScreen ---
            composable(
                route = AppScreens.WebViewScreen.route,
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = tween(animDuration)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(animDuration)
                    )
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