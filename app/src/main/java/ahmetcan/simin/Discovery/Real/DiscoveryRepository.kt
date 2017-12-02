package ahmetcan.simin.Discovery.Real

import ahmetcan.simin.Api.Transcript
import ahmetcan.simin.Api.TranscriptList
import ahmetcan.simin.Api.YoutubeService
import ahmetcan.simin.ApiKey
import ahmetcan.simin.Discovery.Model.Paged
import ahmetcan.simin.Discovery.Model.PlayListModel
import ahmetcan.simin.Discovery.Model.VideoModel
import ahmetcan.simin.Discovery.Model.persistent.YoutubePlaylist
import ahmetcan.simin.Discovery.Model.persistent.YoutubePlaylistResult
import android.os.Build
import android.text.Html
import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.PlaylistListResponse
import io.realm.Realm
import java.io.IOException


object DiscoveryRepository {
    const val CHANNEL_ID = "UCAuUUnT6oDeKwE6v1NGQxug"
    //  const val CHANNEL_ID = "UChk2As5_2Q_c_o5QiCJB8vw"

    private fun youtubeService(): YouTube {
        var youtube = YouTube.Builder(NetHttpTransport(), JacksonFactory(), object : HttpRequestInitializer {
            @Throws(IOException::class)
            override fun initialize(request: HttpRequest) {
            }
        }).setApplicationName("simin").build()
        return youtube
    }

    private fun loadListOnline(nextToken: String? = null): PlaylistListResponse? {
        var youtube = youtubeService()
        val search = youtube.playlists().list("id,snippet,contentDetails")
        search.setKey(ApiKey.YOUTUBEDATAAPIV3_KEY)
        search.setChannelId(CHANNEL_ID)
        nextToken?.let {
            search.pageToken = nextToken
        }
        search.setMaxResults(30)
        return search.execute()
    }

    private fun persistList(index: Int, playListResponse: PlaylistListResponse): YoutubePlaylistResult {
        var realm: Realm = Realm.getDefaultInstance()
        var ytResult: YoutubePlaylistResult = YoutubePlaylistResult()
        try {
            realm.executeTransaction {
                ytResult = realm.createObject(YoutubePlaylistResult::class.java, index)
                ytResult.nextPageToken = playListResponse.nextPageToken
                ytResult.resultsPerPage = playListResponse.pageInfo.resultsPerPage
                ytResult.totalResults = playListResponse.pageInfo.totalResults
                for (item in playListResponse.items) {
                    try {
                        var plitem = realm.createObject(YoutubePlaylist::class.java)
                        plitem.cover =item.snippet.thumbnails?.standard?.url ?: item.snippet.thumbnails?.high?.url ?:item.snippet.thumbnails?.medium?.url ?:item.snippet.thumbnails?.default?.url
                        plitem.description = item.snippet.description
                        plitem.title = item.snippet.title
                        plitem.id = item.id
                        plitem.itemCount = item.contentDetails.itemCount
                        ytResult.items?.add(plitem)
                    }catch (ex:Exception){
                        ex.printStackTrace()
                    }

                }
            }
        }
        catch (ex:Exception){
            ex.printStackTrace()
        }




        return ytResult
    }

    fun loadLists(pageIndex: Int): Paged<Int,PlayListModel>  {

        var realm: Realm = Realm.getDefaultInstance()
        var ytobj: YoutubePlaylistResult?
        var result = Paged<Int,PlayListModel>(0,items=ArrayList())
        var count = realm.where(YoutubePlaylistResult::class.java).count()

        if (pageIndex < 0 || pageIndex > count) {
            throw Exception("Index değeri tutarsız")
        }

        ytobj = realm.where(YoutubePlaylistResult::class.java).equalTo("id", pageIndex as Int).findFirst()

        if (ytobj==null) {
            var nextPageToken=realm.where(YoutubePlaylistResult::class.java).equalTo("id",pageIndex-1).findFirst()?.nextPageToken
            ytobj = persistList(pageIndex, loadListOnline(nextPageToken)!!)
        }


        result.isLastPage = ytobj?.nextPageToken.isNullOrEmpty()
        ytobj?.items?.forEach {
            var playListModel = PlayListModel(it.cover, it.title, it.description, it.itemCount.toString())
            result.items?.add(playListModel)

        }


        return result
    }

    fun invalidateLists()  {
        var realm: Realm = Realm.getDefaultInstance()
        realm.executeTransaction {
            realm.delete(YoutubePlaylistResult::class.java)
        }
    }
    fun search(q:String,nextPageToken:String?):Paged<String,VideoModel>{
        var result = Paged<String,VideoModel>("",items=ArrayList())

        var youtube= youtubeService()

        val search = youtube.search().list("id,snippet")

        search.setKey(ApiKey.YOUTUBEDATAAPIV3_KEY)
        search.setQ(q)
        search.setVideoCaption("closedCaption")

        search.setType("video")
        nextPageToken?.let {
            search.setPageToken(nextPageToken)
        }
        search.setMaxResults(25)
        val searchResponse = search.execute()
        searchResponse?.items?.forEach {
            var model=VideoModel()
            model.videoid=it.id.videoId
            model.cover = it.snippet.thumbnails?.standard?.url ?: it.snippet.thumbnails?.high?.url ?:it.snippet.thumbnails?.medium?.url ?:it.snippet.thumbnails?.default?.url
            model.topText=it.snippet.channelTitle
            model.bottomText=it.snippet.title
            result.items?.add(model)

        }
        result.isLastPage=searchResponse.nextPageToken.isNullOrEmpty()
        if(result.isLastPage==false)result.index=searchResponse.nextPageToken

        return result

    }
    fun captionList(videoId:String): TranscriptList? {

        var list = YoutubeService.instance.caption_list(videoId).execute().body()
        return list;

    }
    fun caption(videoId:String,languageCode:String,translateCode:String): Transcript? {
        var caption = YoutubeService.instance.caption_text(videoId, languageCode, translateCode).execute().body()
        caption?.texts?.forEach {
            it.start*=1000
            it.duration*=1000
            it.sentence = if (Build.VERSION.SDK_INT >= 24) Html.fromHtml(it.sentence, Html.FROM_HTML_MODE_LEGACY).toString() else Html.fromHtml(it?.sentence).toString()

        }
        return caption;
    }


    //    fun DownloadCaption():SRTInfo{
//        var youtube= youtubeService()
//        var output:ByteArrayOutputStream= ByteArrayOutputStream()
//        var input=ByteArrayInputStream(output.toByteArray())
//        var download=youtube.Captions().download("TS5L5RJ7wVJDlvE27h3g4yutQ375JbfG")
//        download.setKey(ApiKey.YOUTUBEDATAAPIV3_KEY)
//        download.setTfmt("srt")
//        download.executeAndDownloadTo(output)
//        var srtInfo=SRTReader.read(InputStreamReader(input))
//        output.close()
//        input.close()
//        return srtInfo
//
//    }
//    fun downloadCaption(){
//
//    var list= YoutubeService.instance.caption_list("pGY5XH52GJo").execute().body()
//    var caption= YoutubeService.instance.caption_text("Lp7E973zozc","en","").execute().body()
//    caption?.texts?.forEach {
//        it.sentence= if(Build.VERSION.SDK_INT >= 24) Html.fromHtml(it.sentence,Html.FROM_HTML_MODE_LEGACY).toString() else Html.fromHtml(it?.sentence).toString()
//
//    }
//
//    }
//
//    fun subscriptions():ArrayList<YoutubeVideoItem> {
//        var result=ArrayList<YoutubeVideoItem>()
//        var youtube= youtubeService()
//        val search = youtube.subscriptions().list("id,snippet")
//        search.setKey(API_KEY)
//        search.setChannelId(CHANNEL_ID)
//        //search.setFields("items(id/videoId)")
//        search.setMaxResults(20)
//        val searchResponse = search.execute()
//        val searchResultList = searchResponse.items
//        return result
//    }
//    fun playLists():ArrayList<YoutubeVideoItem> {
//        var result=ArrayList<YoutubeVideoItem>()
//        var youtube= youtubeService()
//        val search = youtube.playlists().list("id,snippet")
//        search.setKey(API_KEY)
//        search.setChannelId(CHANNEL_ID)
//        search.setMaxResults(20)
//        val searchResponse = search.execute()
//        val searchResultList = searchResponse.items
//
//        return result
//    }

//
//    fun mostUsedVideoList(): ArrayList<YoutubeVideoItem> = runBlocking {
//        var result=ArrayList<YoutubeVideoItem>()
//
//        subscriptions()
//      //  Log.e("aaa",apiResult.toString())
//
//        result
//    }
}

