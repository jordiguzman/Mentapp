package mentat.music.com.mentapp.ui.screens.home.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
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
// import mentat.music.com.mentapp.data.repository.PostRepository // Ya no lo usamos para el flujo principal

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

    // Repositorio Principal (JSON -> Room)
    private val mediaRepository = MediaRepository(database.mediaDao())

    // (Opcional) Si quieres mantener RSS para algo legacy, déjalo,
    // pero Audio/Blog/Divulgación ahora van por mediaRepository.
    // private val postRepository = PostRepository(database.postDao())

    // --- 2. GESTIÓN DE IDIOMA ---
    enum class Language { ES, EN }
    private val _currentLanguage = MutableStateFlow(Language.ES)
    val currentLanguage: StateFlow<Language> = _currentLanguage.asStateFlow()

    fun toggleLanguage() {
        _currentLanguage.value = if (_currentLanguage.value == Language.ES) Language.EN else Language.ES
        // Al cambiar idioma, refrescamos datos porsiaca
        refreshAllData()
    }

    // --- 3. LÓGICA DE FILTRADO (LO NUEVO) ---

    // Variable para saber qué categoría quiere ver el usuario (Audio, Blog, Divulgacion...)
    private val _selectedCategory = MutableStateFlow("Blog") // Valor por defecto

    // FUNCIÓN QUE LLAMA LA UI AL PULSAR UN BOTÓN
    fun filterByCategory(category: String) {
        _selectedCategory.value = category
    }

    // FLUJO DE NOTICIAS (Sustituye al antiguo RSS)
    // Combina: "Todos los datos de Room" + "Categoría seleccionada"
    val newsPosts: StateFlow<List<MediaEntity>> = combine(
        mediaRepository.allMedia,
        _selectedCategory
    ) { allItems, category ->
        // Filtramos la lista gigante de Room para dejar solo lo que pide el botón
        allItems.filter { it.category.equals(category, ignoreCase = true) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )


    // --- 4. FLUJO PRINCIPAL (MÚSICA / JSON ESTÁTICO) ---
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

                    // Estas categorías ahora se gestionan via 'newsPosts' filtrado,
                    // pero si quisieras tenerlas aquí también, podrías mapearlas.
                    // De momento las dejamos a null para no duplicar lógica visual antigua.
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

    // --- HELPERS ---
    private fun filterAndMap(list: List<MediaEntity>, category: String): List<CarouselItem> {
        // Aquí podríamos meter lógica de idioma (title vs titleEn) si el carrusel lo pide directo
        return list.filter { it.category.equals(category, ignoreCase = true) }.map { entity ->
            CarouselItem(
                title = entity.title,
                // OJO: Si entity tiene titleEn, aquí podrías decidir cuál pasar según _currentLanguage.value
                // De momento pasamos el default.
                artist = entity.artist,
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