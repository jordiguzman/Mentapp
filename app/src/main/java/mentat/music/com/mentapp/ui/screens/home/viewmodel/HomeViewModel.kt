package mentat.music.com.mentapp.ui.screens.home.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mentat.music.com.mentapp.data.PostEntity
import mentat.music.com.mentapp.data.local.AppDatabase
import mentat.music.com.mentapp.data.repository.PostRepository

// --- CONSTANTES ---
private val angleStep = (2 * Math.PI.toFloat() / 7)
private val targetAngleRad = (Math.PI.toFloat() / 2.0f)
private val BANDCAMP_START_ANGLE = targetAngleRad - (angleStep * 5)

private const val ROTATION_KEY = "rotationAngle"
private const val ANIMATING_OUT_KEY = "isAnimatingOut"
private const val CLICKED_INDEX_KEY = "clickedIconIndex"
private const val EXPANSION_FINISHED_KEY = "isExpansionFinished"

// --- ESTRUCTURAS DE DATOS ---
@Serializable
data class CarouselItem(
    val imageUrl: String? = null,
    val targetUrl: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val appPackageName: String? = null
)

@Serializable
data class AppData(
    val GUZZ: List<CarouselItem>? = null, // Esto lo sustituiremos pronto por 'newsPosts'
    val Spotify: List<CarouselItem>? = null,
    val Bandcamp: List<CarouselItem>? = null,
    val Soundcloud: List<CarouselItem>? = null,
    val YouTube: List<CarouselItem>? = null,
    val Concepto: List<CarouselItem>? = null
)

sealed class AppState {
    object Loading : AppState()
    data class Success(val data: AppData) : AppState()
    data class Error(val message: String) : AppState()
}

class HomeViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    // --- BASE DE DATOS Y REPOSITORIO ---
    private val database = AppDatabase.getDatabase(application)
    private val repository = PostRepository(database.postDao())

    // --- IDIOMA ---
    enum class Language { ES, EN }
    private val _currentLanguage = MutableStateFlow(Language.ES)
    val currentLanguage: StateFlow<Language> = _currentLanguage.asStateFlow()

    // --- NUEVO: FLUJO DE NOTICIAS REALES (ROOM) ---
    // Esta variable observa el idioma y devuelve automáticamente la lista correcta de la BD
    val newsPosts: StateFlow<List<PostEntity>> = _currentLanguage
        .flatMapLatest { lang ->
            val langCode = if (lang == Language.ES) "es" else "en"
            repository.getPosts(langCode)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // AL ARRANCAR: Bajamos AMBOS idiomas para tener todo listo offline
        downloadAllRssData()
    }

    fun toggleLanguage() {
        _currentLanguage.value = if (_currentLanguage.value == Language.ES) Language.EN else Language.ES
        // Opcional: Podríamos volver a refrescar al cambiar, pero con el init ya debería bastar
        downloadAllRssData()
    }

    private fun downloadAllRssData() {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("MENTAT_DEBUG", "--- INICIANDO SINCRONIZACIÓN COMPLETA (ES + EN) ---")
            // Lanzamos las dos peticiones en paralelo (o secuencial, da igual aquí)
            repository.refreshPosts("es")
            repository.refreshPosts("en")
        }
    }

    // ------------------------------------------
    // --- COMPATIBILIDAD JSON (LEGACY) ---
    // ------------------------------------------
    private val BASE_URL = "https://mentat-music.com/mentapp/"
    private val DEF_JSON_URL = BASE_URL + "mentat_data_DEF.json"
    private val ktorClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; coerceInputValues = true })
        }
    }

    val appState: StateFlow<AppState> = kotlinx.coroutines.flow.flow {
        emit(AppState.Loading)
        try {
            val data = ktorClient.get(DEF_JSON_URL).body<AppData>()
            emit(AppState.Success(data))
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error JSON", e)
            emit(AppState.Error("Error"))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppState.Loading)


    // --- ESTADO DE UI (Sin cambios) ---
    private val _currentPage = mutableIntStateOf(0)
    val currentPage: State<Int> = _currentPage

    fun setCurrentPage(page: Int) { _currentPage.value = page }

    override fun onCleared() {
        super.onCleared()
        ktorClient.close()
    }

    val rotationAngle: StateFlow<Float> = savedStateHandle.getStateFlow(ROTATION_KEY, BANDCAMP_START_ANGLE)
    fun updateRotationAngle(angle: Float) { savedStateHandle[ROTATION_KEY] = angle }

    val isAnimatingOut: StateFlow<Boolean> = savedStateHandle.getStateFlow(ANIMATING_OUT_KEY, false)
    fun updateIsAnimatingOut(isAnimating: Boolean) { savedStateHandle[ANIMATING_OUT_KEY] = isAnimating }

    val clickedIconIndex: StateFlow<Int> = savedStateHandle.getStateFlow(CLICKED_INDEX_KEY, -1)
    fun updateClickedIconIndex(index: Int) { savedStateHandle[CLICKED_INDEX_KEY] = index }

    val isExpansionFinished: StateFlow<Boolean> = savedStateHandle.getStateFlow(EXPANSION_FINISHED_KEY, false)
    fun updateIsExpansionFinished(isFinished: Boolean) { savedStateHandle[EXPANSION_FINISHED_KEY] = isFinished }
}