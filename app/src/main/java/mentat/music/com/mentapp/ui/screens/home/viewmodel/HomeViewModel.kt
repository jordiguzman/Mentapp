package mentat.music.com.mentapp.ui.screens.home.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// --- (Constantes de ángulo y claves de estado... sin cambios) ---
private val angleStep = (2 * Math.PI.toFloat() / 7)
private val targetAngleRad = (Math.PI.toFloat() / 2.0f)
private val BANDCAMP_START_ANGLE = targetAngleRad - (angleStep * 5)

private const val ROTATION_KEY = "rotationAngle"
private const val ANIMATING_OUT_KEY = "isAnimatingOut"
private const val CLICKED_INDEX_KEY = "clickedIconIndex"
private const val EXPANSION_FINISHED_KEY = "isExpansionFinished"
// --- (Fin Constantes) ---


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
    val GUZZ: List<CarouselItem>? = null,
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

// -----------------------------------------------------------
// --- EL VIEWMODEL ---
// -----------------------------------------------------------
class HomeViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // --- 1. LÓGICA DE IDIOMA ENCAPSULADA ---
    enum class Language { ES, EN }
    private val _currentLanguage = MutableStateFlow(Language.ES)
    val currentLanguage: StateFlow<Language> = _currentLanguage.asStateFlow()

    fun toggleLanguage() {
        val newLang = if (_currentLanguage.value == Language.ES) Language.EN else Language.ES
        _currentLanguage.value = newLang
    }
    // ------------------------------------------

    // --- (CONSTANTES DE URL) ---
    private val BASE_URL = "https://mentat-music.com/mentapp/"
    // ÚNICA FUENTE DE VERDAD:
    private val DEF_JSON_URL = BASE_URL + "mentat_data_DEF.json"

    private val _currentPage = mutableIntStateOf(0)
    val currentPage: State<Int> = _currentPage

    // --- (Lógica de Ktor) ---
    private val ktorClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    // --- 2. FLUJO DE DATOS REACTIVO (SIMPLIFICADO) ---
    // Eliminada la lógica de fusión y doble llamada.
    val appState: StateFlow<AppState> = _currentLanguage
        .flatMapLatest { lang ->
            flow {
                emit(AppState.Loading)

                try {
                    // LLAMADA ÚNICA Y LIMPIA
                    // Descargamos el objeto completo (AppData) del archivo DEF.
                    // Ya no buscamos archivos que no existen (404).
                    val data = ktorClient.get(DEF_JSON_URL).body<AppData>()

                    emit(AppState.Success(data))

                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Error cargando datos (Idioma: $lang)", e)
                    emit(AppState.Error("Error al cargar datos. Comprueba tu conexión."))
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppState.Loading
        )

    // --- (Lógica de Rotación y Estado de UI... sin cambios) ---
    fun setCurrentPage(page: Int) {
        _currentPage.value = page
    }

    override fun onCleared() {
        super.onCleared()
        ktorClient.close()
    }

    // [Gestión de estado de UI]
    val rotationAngle: StateFlow<Float> = savedStateHandle.getStateFlow(ROTATION_KEY, BANDCAMP_START_ANGLE)
    fun updateRotationAngle(angle: Float) {
        savedStateHandle[ROTATION_KEY] = angle
    }

    val isAnimatingOut: StateFlow<Boolean> = savedStateHandle.getStateFlow(ANIMATING_OUT_KEY, false)
    val clickedIconIndex: StateFlow<Int> = savedStateHandle.getStateFlow(CLICKED_INDEX_KEY, -1)
    val isExpansionFinished: StateFlow<Boolean> = savedStateHandle.getStateFlow(EXPANSION_FINISHED_KEY, false)

    fun updateIsAnimatingOut(isAnimating: Boolean) {
        savedStateHandle[ANIMATING_OUT_KEY] = isAnimating
    }
    fun updateClickedIconIndex(index: Int) {
        savedStateHandle[CLICKED_INDEX_KEY] = index
    }
    fun updateIsExpansionFinished(isFinished: Boolean) {
        savedStateHandle[EXPANSION_FINISHED_KEY] = isFinished
    }
}