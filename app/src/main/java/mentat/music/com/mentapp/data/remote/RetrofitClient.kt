package mentat.music.com.mentapp.data.remote

import com.tickaroo.tikxml.TikXml
import com.tickaroo.tikxml.retrofit.TikXmlConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "https://www.mentat-music.com/"

    // 1. Configuramos el parser de XML
    private val tikXml = TikXml.Builder()
        .exceptionOnUnreadXml(false) // Si hay etiquetas raras, las ignoramos (¡Seguridad!)
        .build()

    // 2. Configuramos el cliente HTTP (Logs y Tiempos de espera)
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // Veremos todo el XML en el logcat
        })
        .connectTimeout(30, TimeUnit.SECONDS) // Tiempo para conectar
        .readTimeout(30, TimeUnit.SECONDS)    // Tiempo para descargar
        .build()

    // 3. Creamos la instancia de la API
    val api: MentatApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(TikXmlConverterFactory.create(tikXml)) // Aquí conectamos TikXML
            .build()
            .create(MentatApi::class.java)
    }
}