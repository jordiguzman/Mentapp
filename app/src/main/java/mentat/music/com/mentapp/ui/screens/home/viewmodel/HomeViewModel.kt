package mentat.music.com.mentapp.ui.screens.home.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mentat.music.com.mentapp.data.local.AppDatabase
import mentat.music.com.mentapp.data.local.entity.MediaEntity
import mentat.music.com.mentapp.data.model.AppData
import mentat.music.com.mentapp.data.model.CarouselItem
import mentat.music.com.mentapp.data.repository.MediaRepository
import mentat.music.com.mentapp.ui.screens.home.DialConstants

// --- CONSTANTES DE UI ---


private const val ROTATION_KEY = "rotationAngle"
private const val ANIMATING_OUT_KEY = "isAnimatingOut"
private const val CLICKED_INDEX_KEY = "clickedIconIndex"
private const val EXPANSION_FINISHED_KEY = "isExpansionFinished"

// --- ESTADOS DE LA APP (DATOS) ---
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

    enum class Language { ES, EN }

    // --- 2. ESTADO UI UNIFICADO ---
    // Inicializamos con los valores guardados en SavedStateHandle si existen
    private val _uiState = MutableStateFlow(
        HomeUiState(
            rotationAngle = savedStateHandle[ROTATION_KEY] ?: DialConstants.START_ANGLE,
            isAnimatingOut = savedStateHandle[ANIMATING_OUT_KEY] ?: false,
            clickedIconIndex = savedStateHandle[CLICKED_INDEX_KEY] ?: -1,
            isExpansionFinished = savedStateHandle[EXPANSION_FINISHED_KEY] ?: false
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // --- 3. LÓGICA DE DATOS REACTIVA ---

    // Observamos cambios específicos dentro del uiState para no disparar recargas innecesarias
    private val currentLanguageFlow = _uiState.map { it.currentLanguage }.distinctUntilChanged()
    private val selectedCategoryFlow = _uiState.map { it.currentCategoryFilter }.distinctUntilChanged()

    // Flujo de Noticias (Blog, Divulgación, etc.)
    val newsPosts: StateFlow<List<CarouselItem>> = combine(
        mediaRepository.allMedia,
        selectedCategoryFlow,
        currentLanguageFlow
    ) { allItems, category, language ->
        allItems
            .filter { it.category.equals(category, ignoreCase = true) }
            .map { entity -> mapEntityToItem(entity, language) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Flujo Principal (Música, AppData)
    val appState: StateFlow<AppState> = combine(
        mediaRepository.allMedia,
        currentLanguageFlow
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

    // --- 4. ACCIONES DE UI (Modifican el UI State) ---

    fun toggleLanguage() {
        _uiState.update {
            val newLang = if (it.currentLanguage == Language.ES) Language.EN else Language.ES
            it.copy(currentLanguage = newLang)
        }
    }

    fun filterByCategory(category: String) {
        _uiState.update { it.copy(currentCategoryFilter = category) }
    }

    fun setCurrentPage(page: Int) {
        _uiState.update { it.copy(currentPage = page) }
    }

    // Métodos que guardan persistencia (SavedStateHandle) Y actualizan UI
    fun updateRotationAngle(angle: Float) {
        savedStateHandle[ROTATION_KEY] = angle
        _uiState.update { it.copy(rotationAngle = angle) }
    }

    fun updateIsAnimatingOut(isAnimating: Boolean) {
        savedStateHandle[ANIMATING_OUT_KEY] = isAnimating
        _uiState.update { it.copy(isAnimatingOut = isAnimating) }
    }

    // Unificación de updateClickedIconIndex y onIconClicked
    fun onIconClicked(index: Int) {
        savedStateHandle[CLICKED_INDEX_KEY] = index
        _uiState.update { it.copy(clickedIconIndex = index) }
    }
    // Alias para mantener compatibilidad si lo llamas así en otros sitios
    fun updateClickedIconIndex(index: Int) = onIconClicked(index)

    fun updateIsExpansionFinished(isFinished: Boolean) {
        savedStateHandle[EXPANSION_FINISHED_KEY] = isFinished
        _uiState.update { it.copy(isExpansionFinished = isFinished) }
    }

    // Lógica Web Menu
    fun setWebMenuOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isWebMenuOpen = isOpen) }
    }

    fun updateWebMenuRotationAngle(angle: Float) {
        _uiState.update { it.copy(webMenuRotationAngle = angle) }
    }

    fun setSelectedWebCategory(category: String?) {
        _uiState.update { it.copy(selectedWebCategory = category) }
    }


    // --- 5. HELPERS DE MAPEO (INTACTOS) ---

    private fun mapEntityToItem(entity: MediaEntity, language: Language): CarouselItem {
        val isEnglish = language == Language.EN
        val realContent = if (isEnglish && !entity.contentEn.isNullOrBlank()) entity.contentEn else entity.content
        val realArtist = entity.artist

        return CarouselItem(
            id = entity.id.toString(),
            imageUrl = entity.imageUrl,
            appPackageName = entity.appPackageName,
            category = entity.category,
            date = realArtist,
            title = if (isEnglish && !entity.titleEn.isNullOrBlank()) entity.titleEn else entity.title,
            targetUrl = if (isEnglish && !entity.targetUrlEn.isNullOrBlank()) entity.targetUrlEn else entity.targetUrl,
            content = realContent,
            artist = if (entity.category == "Blog" || entity.category == "Divulgacion") {
                realContent
            } else {
                realArtist
            }
        )
    }

    private fun filterAndMap(list: List<MediaEntity>, category: String, language: Language): List<CarouselItem> {
        return list
            .filter { it.category.equals(category, ignoreCase = true) }
            .map { entity -> mapEntityToItem(entity, language) }
    }
    // --- LÓGICA DE NAVEGACIÓN (BACK PRESS) ---
    // Devuelve true si el ViewModel consumió el evento 'Atrás', false si debe salir de la app
    fun handleBackPress() {
        val currentState = _uiState.value

        if (currentState.isExpansionFinished || currentState.clickedIconIndex != -1 || currentState.selectedWebCategory != null) {
            val wasWebMode = currentState.selectedWebCategory != null

            // 1. Reseteamos estados
            updateIsExpansionFinished(false)
            updateIsAnimatingOut(false)
            setSelectedWebCategory(null)
            onIconClicked(-1)

            // 2. Lógica específica de retorno al menú web
            if (wasWebMode) {
                viewModelScope.launch {
                    kotlinx.coroutines.delay(300)
                    setWebMenuOpen(true)
                }
            }
        } else if (currentState.isWebMenuOpen) {
            setWebMenuOpen(false)
        }
    }
}