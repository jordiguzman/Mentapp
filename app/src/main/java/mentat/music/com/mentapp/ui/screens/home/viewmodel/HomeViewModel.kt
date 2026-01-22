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

// --- CONSTANTES DE UI ---
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

    // --- 3. LÓGICA DE FILTRADO (RECUPERADA Y CORREGIDA) ---

    // Estado para saber qué botón ha pulsado el usuario (Audio, Blog, Divulgacion)
    private val _selectedCategory = MutableStateFlow("Blog") // Por defecto Blog

    // Función que llama tu HomeScreen (¡Recuperada!)
    fun filterByCategory(category: String) {
        _selectedCategory.value = category
    }

    // El flujo que escucha tu HomeScreen.
    // AQUÍ APLICAMOS LA MAGIA: Al crear la lista, cruzamos los cables (Artist->Content)
    val newsPosts: StateFlow<List<CarouselItem>> = combine(
        mediaRepository.allMedia,
        _selectedCategory
    ) { allItems, category ->
        allItems
            .filter { it.category.equals(category, ignoreCase = true) }
            .map { entity ->
                // --- CHIVATO DE LA VERDAD ---
                if (entity.title?.contains("plugins", ignoreCase = true) == true) {
                    Log.e("MENTAPP_VM", "LEYENDO DE DB -> ID: ${entity.id}")
                    Log.e("MENTAPP_VM", "LEYENDO DE DB -> Content: ${entity.content?.take(20)}...")
                    Log.e("MENTAPP_VM", "LEYENDO DE DB -> Artist: ${entity.artist}")
                }
                // TRADUCTOR PARA NOTICIAS (Arregla lo del texto cortado)
                CarouselItem(
                    id = entity.id.toString(), // Convertimos el ID numérico a String
                    title = entity.title,
                    imageUrl = entity.imageUrl,
                    targetUrl = entity.targetUrl,

                    // 1. EL RESUMEN (Guardado en 'artist') va a 'content' para leerse bien
                    content = entity.content,
                    // 2. LA CATEGORÍA va a 'artist' para verse pequeñita abajo
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

                    // Dejamos esto null porque se usa el newsPosts de arriba
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
            Log.d("MENTAT_ViewModel", "Refrescando datos...")
            mediaRepository.refreshMedia()
        }
    }

    // --- HELPERS (AYUDANTES) ---

    // Helper clásico para música.
    // HE AÑADIDO 'id' AQUÍ, QUE ERA LO QUE FALLABA EN TU COMPILACIÓN.
    private fun filterAndMap(list: List<MediaEntity>, category: String): List<CarouselItem> {
        return list.filter { it.category.equals(category, ignoreCase = true) }.map { entity ->
            CarouselItem(
                id = entity.id.toString(), // <--- ESTO FALTABA y provocaba el error en CarouselItem
                title = entity.title,
                artist = entity.artist, // En música sí queremos el artista normal
                imageUrl = entity.imageUrl,
                targetUrl = entity.targetUrl,
                appPackageName = entity.appPackageName
            )
        }
    }

    // --- ESTADO DE UI (Paginación, Rotación, Animaciones) ---
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
}