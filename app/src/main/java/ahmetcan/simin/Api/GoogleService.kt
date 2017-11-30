package ahmetcan.simin.Api

import com.google.gson.JsonArray
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleService {

    @GET("search")
    fun youtube_suggest(
             @Query("q") q: String="",
             @Query("hl") hl: String="en",
             @Query("region") region: String="US",
             @Query("ds") ds: String="yt",
             @Query("client") client: String="firefox"
            ): Call<JsonArray>


    companion object {

        val instance: GoogleService by lazy {
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
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .baseUrl("https://suggestqueries.google.com/complete/")
                    .build()

            retrofit.create(GoogleService::class.java)
        }
    }
}