package ahmetcan.simin

import ahmetcan.simin.Discovery.Model.Paged
import ahmetcan.simin.Discovery.Model.VideoModel
import ahmetcan.simin.Discovery.Real.DiscoveryRepository
import ahmetcan.simin.Discovery.View.YoutubeVideoAdapter
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.paginate.Paginate
import kotlinx.android.synthetic.main.activity_video_list.*
import kotlinx.coroutines.*

class VideoListActivity : ActivityBase() {
    var adapter = YoutubeVideoAdapter()
    var loading: Boolean = false
    var isHasLoadedAll: Boolean = false
    var nextPageToken: String? = null

    lateinit var listAdapter: ArrayAdapter<String>
    private var channelid: String? = null
    private var playlistid: String? = null
    var scope = MainScope() + CoroutineExceptionHandler { _, ex ->
        FirebaseCrashlytics.getInstance().recordException(ex)
        Log.e("ahmetcan", "VideoListActivity main scope exception [blocked]", ex)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_video_list)
        if (intent.extras.containsKey("playlistid")) {
            playlistid = intent.extras["playlistid"] as String
        } else {
            channelid = intent.extras["channelid"] as String
        }

        backButton.setOnClickListener {
            finish()
        }
        listAdapter = ArrayAdapter<String>(this, R.layout.search_autocomplete_listitem, arrayListOf(""))

        InitList()


    }


    override fun onDestroy() {
        super.onDestroy()
        scope.cancel("ondestroy")
    }
    fun goNext(itemModel:VideoModel){
        progress.visibility=View.GONE
        var intent = Intent(this@VideoListActivity, PreviewVideoWeb::class.java)
        intent.putExtra("videoid", itemModel.videoid)
        intent.putExtra("title", itemModel.title)
        intent.putExtra("description", itemModel.description)
        intent.putExtra("cover", itemModel.cover)
        startActivity(intent)
    }
    fun InitList() {
        val linearLayoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        linearLayoutManager.orientation = androidx.recyclerview.widget.LinearLayoutManager.VERTICAL


        rvList.setLayoutManager(linearLayoutManager)
        rvList.adapter = adapter
        adapter.onClickItem = object : YoutubeVideoAdapter.OnItemClickListener {
            override fun onClick(itemModel: VideoModel) {
                progress.visibility= View.VISIBLE
                //checkAds(itemModel)
                goNext(itemModel)
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

    fun loadMore() = scope.launch {
        loading = true
        var result: Paged<String, VideoModel>

        if (!playlistid.isNullOrEmpty()) {
            result = withContext(Dispatchers.IO){DiscoveryRepository.playlistItems(playlistid ?: "", nextPageToken)}
        } else {
            result =  withContext(Dispatchers.IO){DiscoveryRepository.channelVideos(channelid ?: "", nextPageToken)}
        }


        if (result.isLastPage) {
            isHasLoadedAll = true
        }
        nextPageToken = result.index

        result.items?.let {
            adapter.addData(it)
            adapter.notifyDataSetChanged()
        }
        loading = false
    }


}
