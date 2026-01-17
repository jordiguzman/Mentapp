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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mentat.music.com.mentapp.data.PostEntity
import mentat.music.com.mentapp.data.local.AppDatabase
import mentat.music.com.mentapp.data.local.entity.MediaEntity
import mentat.music.com.mentapp.data.model.AppData
import mentat.music.com.mentapp.data.model.CarouselItem
import mentat.music.com.mentapp.data.repository.MediaRepository
import mentat.music.com.mentapp.data.repository.PostRepository

// --- CONSTANTES DE UI (Sin cambios) ---
private val angleStep = (2 * Math.PI.toFloat() / 7)
private val targetAngleRad = (Math.PI.toFloat() / 2.0f)
private val BANDCAMP_START_ANGLE = targetAngleRad - (angleStep * 5)

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

    // --- 1. INICIALIZACIÓN DE BASES DE DATOS Y REPOSITORIOS ---
    private val database = AppDatabase.getDatabase(application)

    // Repositorio de Noticias (RSS)
    private val postRepository = PostRepository(database.postDao())

    // NUEVO: Repositorio de Música/Video (JSON -> Room)
    private val mediaRepository = MediaRepository(database.mediaDao())

    // --- 2. GESTIÓN DE IDIOMA (RSS) ---
    enum class Language { ES, EN }
    private val _currentLanguage = MutableStateFlow(Language.ES)
    val currentLanguage: StateFlow<Language> = _currentLanguage.asStateFlow()

    // Flujo de Noticias (RSS) - Igual que antes
    val newsPosts: StateFlow<List<PostEntity>> = _currentLanguage
        .flatMapLatest { lang ->
            val langCode = if (lang == Language.ES) "es" else "en"
            postRepository.getPosts(langCode)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // --- 3. NUEVO FLUJO PRINCIPAL (JSON -> ROOM -> UI) ---
    // Aquí ocurre la magia: La UI observa la base de datos, no internet.
    val appState: StateFlow<AppState> = mediaRepository.allMedia
        .map { mediaList ->
            // Convertimos la lista plana de Room a la estructura AppData
            if (mediaList.isEmpty()) {
                AppState.Loading // O Success vacío si prefieres
            } else {
                val organizedData = AppData(
                    GUZZ = filterAndMap(mediaList, "GUZZ"),
                    Spotify = filterAndMap(mediaList, "Spotify"),
                    Bandcamp = filterAndMap(mediaList, "Bandcamp"),
                    Soundcloud = filterAndMap(mediaList, "Soundcloud"),
                    YouTube = filterAndMap(mediaList, "YouTube"),
                    Concepto = null // Este va por RSS (newsPosts), así que null aquí
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
        // AL ARRANCAR:
        // 1. Bajamos noticias (RSS)
        downloadAllRssData()

        // 2. Bajamos JSON y actualizamos Room (Música/Video)
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("MENTAT_ViewModel", "Iniciando sincronización de Medios...")
            mediaRepository.refreshMedia()
        }
    }

    // --- HELPERS ---
    private fun filterAndMap(list: List<MediaEntity>, category: String): List<CarouselItem> {
        return list.filter { it.category == category }.map { entity ->
            CarouselItem(
                title = entity.title,
                artist = entity.artist,
                imageUrl = entity.imageUrl,
                targetUrl = entity.targetUrl,
                appPackageName = entity.appPackageName
            )
        }
    }

    fun toggleLanguage() {
        _currentLanguage.value = if (_currentLanguage.value == Language.ES) Language.EN else Language.ES
        downloadAllRssData()
    }

    private fun downloadAllRssData() {
        viewModelScope.launch(Dispatchers.IO) {
            postRepository.refreshPosts("es")
            postRepository.refreshPosts("en")
        }
    }

    // --- ESTADO DE UI (Sin cambios) ---
    private val _currentPage = mutableIntStateOf(0)
    val currentPage: State<Int> = _currentPage

    fun setCurrentPage(page: Int) { _currentPage.value = page }

    // Gestión de rotación y animaciones
    val rotationAngle: StateFlow<Float> = savedStateHandle.getStateFlow(ROTATION_KEY, BANDCAMP_START_ANGLE)
    fun updateRotationAngle(angle: Float) { savedStateHandle[ROTATION_KEY] = angle }

    val isAnimatingOut: StateFlow<Boolean> = savedStateHandle.getStateFlow(ANIMATING_OUT_KEY, false)
    fun updateIsAnimatingOut(isAnimating: Boolean) { savedStateHandle[ANIMATING_OUT_KEY] = isAnimating }

    val clickedIconIndex: StateFlow<Int> = savedStateHandle.getStateFlow(CLICKED_INDEX_KEY, -1)
    fun updateClickedIconIndex(index: Int) { savedStateHandle[CLICKED_INDEX_KEY] = index }

    val isExpansionFinished: StateFlow<Boolean> = savedStateHandle.getStateFlow(EXPANSION_FINISHED_KEY, false)
    fun updateIsExpansionFinished(isFinished: Boolean) { savedStateHandle[EXPANSION_FINISHED_KEY] = isFinished }
}