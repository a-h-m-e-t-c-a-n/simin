package ahmetcan.simin.Api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query


interface YoutubeService {

    @GET("timedtext?type=list")
    fun caption_list(
            @Query("v") videoid: String=""
    ): Call<TranscriptList>

    @GET("timedtext")
    fun caption_text(
            @Query("v") videoid: String,
            @Query("lang") langugeCode: String,
            @Query("tlang") translateLangugeCode: String
    ): Call<Transcript>


    companion object {

        val instance: YoutubeService by lazy {
            val clientInterceptor: Interceptor = Interceptor() {
                var request = it.request()
                val url = request.url().newBuilder()
                        .build()
                request = request.newBuilder().url(url).build()
                it.proceed(request)
            }

            val interceptor = HttpLoggingInterceptor()
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)

            val client = OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .addNetworkInterceptor(clientInterceptor)
                    .build()

            val retrofit = Retrofit.Builder()
                    .addConverterFactory(SimpleXmlConverterFactory.create())
                    .client(client)
                    .baseUrl("http://video.google.com/")
                    .build()

            retrofit.create(YoutubeService::class.java)
        }
    }
}