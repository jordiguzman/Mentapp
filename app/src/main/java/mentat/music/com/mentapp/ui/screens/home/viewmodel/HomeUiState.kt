package mentat.music.com.mentapp.ui.screens.home.viewmodel

import mentat.music.com.mentapp.ui.screens.home.viewmodel.HomeViewModel.Language

/**
 * Estado unificado de la UI.
 * Agrupa persistencia (SavedStateHandle) y estado efímero (WebMenu, Páginas).
 */
data class HomeUiState(
    // Configuración Global
    val currentLanguage: Language = Language.ES,

    // Estado del Dial Principal (Persistente)
    val rotationAngle: Float = (-Math.PI / 2).toFloat(), // BANDCAMP_START_ANGLE
    val isAnimatingOut: Boolean = false,
    val clickedIconIndex: Int = -1,
    val isExpansionFinished: Boolean = false,

    // Estado del Menú Web
    val isWebMenuOpen: Boolean = false,
    val webMenuRotationAngle: Float = 0f,
    val selectedWebCategory: String? = null,
    val currentCategoryFilter: String = "Blog", // Para filtrar noticias

    // Estado de Contenido
    val currentPage: Int = 0
)