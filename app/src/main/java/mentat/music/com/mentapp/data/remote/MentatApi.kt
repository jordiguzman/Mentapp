package mentat.music.com.mentapp.data.remote

import mentat.music.com.mentapp.data.remote.dto.RssFeedDto
import retrofit2.http.GET

interface MentatApi {

    // Llama a: https://www.mentat-music.com/feed/
    @GET("feed/")
    suspend fun getFeedEs(): RssFeedDto

    // Llama a: https://www.mentat-music.com/en/feed/
    @GET("en/feed/")
    suspend fun getFeedEn(): RssFeedDto
}