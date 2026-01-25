package mentat.music.com.mentapp.ui.screens.home.viewmodel

import android.app.Application
import android.util.Log
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mentat.music.com.mentapp.data.local.AppDatabase
import mentat.music.com.mentapp.data.local.entity.MediaEntity
import mentat.music.com.mentapp.data.model.AppData
import mentat.music.com.mentapp.data.model.CarouselItem
import mentat.music.com.mentapp.data.repository.MediaRepository

// --- CONSTANTES DE UI (AJUSTADAS A 6 ICONOS) ---
// 1. Dividimos el círculo en 6 partes iguales (60 grados por item)
private val angleStep = (2 * Math.PI.toFloat() / 6)

// 2. Definimos el ángulo objetivo (Las 6 en punto = Abajo = 90º = PI/2)
private val targetAngleRad = (Math.PI.toFloat() / 2.0f)

// 3. POSICIÓN INICIAL: BANDCAMP AL CENTRO
// Bandcamp es el índice 3. Para que esté a las 6 en punto (90º),
// tenemos que rotar el dial -90º hacia atrás.
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
        refreshAllData()
    }

    // --- 3. LÓGICA DE FILTRADO ---
    private val _selectedCategory = MutableStateFlow("Blog") // Por defecto Blog

    fun filterByCategory(category: String) {
        _selectedCategory.value = category
    }

    val newsPosts: StateFlow<List<CarouselItem>> = combine(
        mediaRepository.allMedia,
        _selectedCategory
    ) { allItems, category ->
        allItems
            .filter { it.category.equals(category, ignoreCase = true) }
            .map { entity ->
                CarouselItem(
                    id = entity.id.toString(),
                    title = entity.title,
                    imageUrl = entity.imageUrl,
                    targetUrl = entity.targetUrl,
                    // Intercambio de campos para visualización correcta de noticias
                    content = entity.content,
                    artist = entity.artist,
                    appPackageName = entity.appPackageName
                )
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )


    // --- 4. FLUJO PRINCIPAL (MÚSICA) ---
    val appState: StateFlow<AppState> = mediaRepository.allMedia
        .map { mediaList ->
            if (mediaList.isEmpty()) {
                AppState.Loading
            } else {
                val organizedData = AppData(
                    GUZZ = filterAndMap(mediaList, "GUZZ"),
                    Spotify = filterAndMap(mediaList, "Spotify"),
                    Bandcamp = filterAndMap(mediaList, "Bandcamp"),
                    Soundcloud = filterAndMap(mediaList, "Soundcloud"),
                    YouTube = filterAndMap(mediaList, "YouTube"),
                    Audio = null,
                    Divulgacion = null,
                    Blog = null,
                    Concepto = null
                )
                AppState.Success(organizedData)
            }
        }
        .stateIn(
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

    // --- HELPERS ---
    private fun filterAndMap(list: List<MediaEntity>, category: String): List<CarouselItem> {
        return list.filter { it.category.equals(category, ignoreCase = true) }.map { entity ->
            CarouselItem(
                id = entity.id.toString(),
                title = entity.title,
                artist = entity.artist,
                imageUrl = entity.imageUrl,
                targetUrl = entity.targetUrl,
                appPackageName = entity.appPackageName
            )
        }
    }

    // --- ESTADO DE UI ---
    private val _currentPage = mutableIntStateOf(0)
    val currentPage: State<Int> = _currentPage
    fun setCurrentPage(page: Int) { _currentPage.value = page }

    // AQUÍ SE USA LA NUEVA CONSTANTE BANDCAMP_START_ANGLE
    val rotationAngle: StateFlow<Float> = savedStateHandle.getStateFlow(ROTATION_KEY, BANDCAMP_START_ANGLE)
    fun updateRotationAngle(angle: Float) { savedStateHandle[ROTATION_KEY] = angle }

    val isAnimatingOut: StateFlow<Boolean> = savedStateHandle.getStateFlow(ANIMATING_OUT_KEY, false)
    fun updateIsAnimatingOut(isAnimating: Boolean) { savedStateHandle[ANIMATING_OUT_KEY] = isAnimating }

    val clickedIconIndex: StateFlow<Int> = savedStateHandle.getStateFlow(CLICKED_INDEX_KEY, -1)
    fun updateClickedIconIndex(index: Int) { savedStateHandle[CLICKED_INDEX_KEY] = index }

    val isExpansionFinished: StateFlow<Boolean> = savedStateHandle.getStateFlow(EXPANSION_FINISHED_KEY, false)
    fun updateIsExpansionFinished(isFinished: Boolean) { savedStateHandle[EXPANSION_FINISHED_KEY] = isFinished }
    // --- LÓGICA DEL MINI DIAL (WEB) ---

    // 1. Interruptor: ¿Está desplegado el menú satélite?
    private val _isWebMenuOpen = MutableStateFlow(false)
    val isWebMenuOpen: StateFlow<Boolean> = _isWebMenuOpen.asStateFlow()
    fun setWebMenuOpen(isOpen: Boolean) { _isWebMenuOpen.value = isOpen }

    // 2. Física Independiente: El ángulo de rotación del Mini Dial
    // (Necesita su propia variable porque el Dial Grande estará quieto)
    private val _webMenuRotationAngle = MutableStateFlow(0f)
    val webMenuRotationAngle: StateFlow<Float> = _webMenuRotationAngle.asStateFlow()
    fun updateWebMenuRotationAngle(angle: Float) { _webMenuRotationAngle.value = angle }
}
