package mentat.music.com.mentapp.ui.screens.home.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mentat.music.com.mentapp.data.local.AppDatabase
import mentat.music.com.mentapp.data.local.entity.MediaEntity
import mentat.music.com.mentapp.data.model.AppData
import mentat.music.com.mentapp.data.model.CarouselItem
import mentat.music.com.mentapp.data.repository.MediaRepository

// --- CONSTANTES DE UI ---
private val angleStep = (2 * Math.PI.toFloat() / 6)
private val targetAngleRad = (Math.PI.toFloat() / 2.0f)
private val BANDCAMP_START_ANGLE = (-Math.PI / 2).toFloat()

private const val ROTATION_KEY = "rotationAngle"
private const val ANIMATING_OUT_KEY = "isAnimatingOut"
private const val CLICKED_INDEX_KEY = "clickedIconIndex"
private const val EXPANSION_FINISHED_KEY = "isExpansionFinished"

// --- ESTADOS DE LA APP ---
sealed class AppState {
    object Loading : AppState()
    data class Success(val data: AppData) : AppState()
    data class Error(val message: String) : AppState()
}

class HomeViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    // --- 1. INICIALIZACIÓN ---
    private val database = AppDatabase.getDatabase(application)
    private val mediaRepository = MediaRepository(database.mediaDao())

    // --- 2. GESTIÓN DE IDIOMA ---
    enum class Language { ES, EN }
    private val _currentLanguage = MutableStateFlow(Language.ES)
    val currentLanguage: StateFlow<Language> = _currentLanguage.asStateFlow()

    fun toggleLanguage() {
        _currentLanguage.value = if (_currentLanguage.value == Language.ES) Language.EN else Language.ES
        // No hace falta refreshAllData() porque los flujos ya observan el cambio de idioma
    }

    // --- 3. LÓGICA DE FILTRADO (NOTICIAS) ---
    private val _selectedCategory = MutableStateFlow("Blog")

    fun filterByCategory(category: String) {
        _selectedCategory.value = category
    }

    // AHORA ESCUCHAMOS 3 COSAS: DATOS, CATEGORÍA E IDIOMA
    val newsPosts: StateFlow<List<CarouselItem>> = combine(
        mediaRepository.allMedia,
        _selectedCategory,
        _currentLanguage // <--- ¡NUEVO!
    ) { allItems, category, language ->
        allItems
            .filter { it.category.equals(category, ignoreCase = true) }
            .map { entity -> mapEntityToItem(entity, language) } // Usamos el mapeador inteligente
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )


    // --- 4. FLUJO PRINCIPAL (MÚSICA) ---
    // AHORA ESCUCHAMOS 2 COSAS: DATOS E IDIOMA
    val appState: StateFlow<AppState> = combine(
        mediaRepository.allMedia,
        _currentLanguage // <--- ¡NUEVO!
    ) { mediaList, language ->
        if (mediaList.isEmpty()) {
            AppState.Loading
        } else {
            val organizedData = AppData(
                GUZZ = filterAndMap(mediaList, "GUZZ", language),
                Spotify = filterAndMap(mediaList, "Spotify", language),
                Bandcamp = filterAndMap(mediaList, "Bandcamp", language),
                Soundcloud = filterAndMap(mediaList, "Soundcloud", language),
                YouTube = filterAndMap(mediaList, "YouTube", language),
                Audio = null,
                Divulgacion = null,
                Blog = null
            )
            AppState.Success(organizedData)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppState.Loading
    )

    init {
        refreshAllData()
    }

    private fun refreshAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            mediaRepository.refreshMedia()
        }
    }

    // --- HELPER MAESTRO: TRADUCTOR ---
    // Esta función decide qué texto mostrar según el idioma seleccionado
    private fun mapEntityToItem(entity: MediaEntity, language: Language): CarouselItem {
        val isEnglish = language == Language.EN

        return CarouselItem(
            id = entity.id.toString(),
            imageUrl = entity.imageUrl,
            appPackageName = entity.appPackageName,
            category = entity.category,
            date = entity.artist, // Usamos el campo artist como fecha para noticias si aplica

            // --- LÓGICA DE TRADUCCIÓN ---
            // Título
            title = if (isEnglish && !entity.titleEn.isNullOrBlank()) entity.titleEn else entity.title,

            // Contenido (Resumen en noticias)
            content = if (isEnglish && !entity.contentEn.isNullOrBlank()) entity.contentEn else entity.content,

            // URL de destino (por si tienes links diferentes para inglés)
            targetUrl = if (isEnglish && !entity.targetUrlEn.isNullOrBlank()) entity.targetUrlEn else entity.targetUrl,

            // Artista (En música suele ser igual, pero si tienes lógica especial, va aquí)
            artist = entity.artist
        )
    }

    // Helper para filtrar listas de música
    private fun filterAndMap(list: List<MediaEntity>, category: String, language: Language): List<CarouselItem> {
        return list
            .filter { it.category.equals(category, ignoreCase = true) }
            .map { entity -> mapEntityToItem(entity, language) }
    }

    // --- ESTADO DE UI (SIN CAMBIOS) ---
    private val _currentPage = mutableIntStateOf(0)
    val currentPage: State<Int> = _currentPage
    fun setCurrentPage(page: Int) { _currentPage.value = page }

    val rotationAngle: StateFlow<Float> = savedStateHandle.getStateFlow(ROTATION_KEY, BANDCAMP_START_ANGLE)
    fun updateRotationAngle(angle: Float) { savedStateHandle[ROTATION_KEY] = angle }

    val isAnimatingOut: StateFlow<Boolean> = savedStateHandle.getStateFlow(ANIMATING_OUT_KEY, false)
    fun updateIsAnimatingOut(isAnimating: Boolean) { savedStateHandle[ANIMATING_OUT_KEY] = isAnimating }

    val clickedIconIndex: StateFlow<Int> = savedStateHandle.getStateFlow(CLICKED_INDEX_KEY, -1)
    fun updateClickedIconIndex(index: Int) { savedStateHandle[CLICKED_INDEX_KEY] = index }

    val isExpansionFinished: StateFlow<Boolean> = savedStateHandle.getStateFlow(EXPANSION_FINISHED_KEY, false)
    fun updateIsExpansionFinished(isFinished: Boolean) { savedStateHandle[EXPANSION_FINISHED_KEY] = isFinished }

    // --- LÓGICA DEL MINI DIAL (WEB) ---
    private val _isWebMenuOpen = MutableStateFlow(false)
    val isWebMenuOpen: StateFlow<Boolean> = _isWebMenuOpen.asStateFlow()
    fun setWebMenuOpen(isOpen: Boolean) { _isWebMenuOpen.value = isOpen }

    private val _webMenuRotationAngle = MutableStateFlow(0f)
    val webMenuRotationAngle: StateFlow<Float> = _webMenuRotationAngle.asStateFlow()
    fun updateWebMenuRotationAngle(angle: Float) { _webMenuRotationAngle.value = angle }
    // NUEVA VARIABLE: Guarda qué botón del Mini Dial pulsaste (Audio, Divulgacion, Blog)
    private val _selectedWebCategory = MutableStateFlow<String?>(null)
    val selectedWebCategory: StateFlow<String?> = _selectedWebCategory.asStateFlow()
    fun setSelectedWebCategory(category: String?) { _selectedWebCategory.value = category }
}
