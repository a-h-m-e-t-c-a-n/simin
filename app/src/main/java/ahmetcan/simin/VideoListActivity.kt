package ahmetcan.simin

import ahmetcan.simin.Api.GoogleService
import ahmetcan.simin.Discovery.Model.Paged
import ahmetcan.simin.Discovery.Model.VideoModel
import ahmetcan.simin.Discovery.Real.DiscoveryRepository
import ahmetcan.simin.Discovery.View.YoutubeVideoAdapter
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.gson.JsonArray
import com.paginate.Paginate
import kotlinx.android.synthetic.main.activity_video_list.*

class VideoListActivity : ActivityBase() {
    var adapter= YoutubeVideoAdapter()
    var loading:Boolean=false
    var isHasLoadedAll:Boolean=false
    var nextPageToken:String?=null
    private  var mInterstitialAd: InterstitialAd? = null

    lateinit var listAdapter: ArrayAdapter<String>
    private var channelid: String?=null
    private var playlistid: String?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_video_list)
        if(intent.extras.containsKey("playlistid")){
            playlistid=intent.extras["playlistid"] as String
        }
        else{
            channelid=intent.extras["channelid"] as String
        }

        backButton.setOnClickListener {
            finish()
        }
        listAdapter = ArrayAdapter<String>(this, R.layout.search_autocomplete_listitem, arrayListOf(""))

        InitList()

        if (BuildConfig.DEBUG) {
            Log.e("SİMİN WARNING","Debug olduğu  için reklam kaldırıldı")
        }
        else{
            mInterstitialAd = InterstitialAd(this@VideoListActivity);
            mInterstitialAd?.let {
                it.setAdUnitId(ApiKey.ADMOB_PREVIEWVIDEO_UNIT)
                it.loadAd(AdRequest.Builder().build())
                it.adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        super.onAdLoaded()
                        it.show()
                    }
                }

            }
        }
    }

    override fun onStart() {
        super.onStart()

    }
    fun InitList() {
        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL


        rvList.setLayoutManager(linearLayoutManager)
        rvList.adapter=adapter
        adapter.onClickItem=object : YoutubeVideoAdapter.OnItemClickListener{
            override fun onClick(itemModel: VideoModel) {
                var intent= Intent(this@VideoListActivity, PreviewVideo::class.java)
                intent.putExtra("videoid",itemModel.videoid)
                intent.putExtra("title",itemModel.title)
                intent.putExtra("description",itemModel.description)
                intent.putExtra("cover",itemModel.cover)
                startActivity(intent)
            }

        }

        val callbacks = object : Paginate.Callbacks {
            override fun onLoadMore() {
                loadMore()
            }

            override fun isLoading(): Boolean {
                // Indicate whether new page loading is in progress or not
                return loading
            }

            override fun hasLoadedAllItems(): Boolean {
                // Indicate whether all data (pages) are loaded or not
                return isHasLoadedAll
            }
        }


        Paginate.with(rvList, callbacks)
                .setLoadingTriggerThreshold(2)
                .addLoadingListItem(true)
                .build()



    }
    fun loadMore() =safeAsync {
        loading=true
        var result: Paged<String, VideoModel>
        if(!playlistid.isNullOrEmpty()){
            result = DiscoveryRepository.playlistItems(playlistid?:"",nextPageToken)
        }
        else{
            result = DiscoveryRepository.channelVideos(channelid?:"",nextPageToken)
        }


        if(result.isLastPage){
            isHasLoadedAll=true
        }
        nextPageToken=result.index

        onUI{
            result.items?.let {
                adapter.addData(it)
                adapter.notifyDataSetChanged()
            }
        }.join()
        loading=false
    }


}