package ahmetcan.simin

import ahmetcan.simin.Discovery.Model.Paged
import ahmetcan.simin.Discovery.Model.VideoModel
import ahmetcan.simin.Discovery.Real.DiscoveryRepository
import ahmetcan.simin.Discovery.View.YoutubeVideoAdapter
import android.arch.lifecycle.ViewModel
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.reward.RewardItem
import com.google.android.gms.ads.reward.RewardedVideoAd
import com.google.android.gms.ads.reward.RewardedVideoAdListener
import com.paginate.Paginate
import kotlinx.android.synthetic.main.activity_search.*
import kotlinx.android.synthetic.main.activity_video_list.*
import kotlinx.coroutines.android.UI
import kotlinx.coroutines.launch

class VideoListActivity : ActivityBase() {
    var adapter = YoutubeVideoAdapter()
    var loading: Boolean = false
    var isHasLoadedAll: Boolean = false
    var nextPageToken: String? = null
    private  var mRewardedVideoAd: RewardedVideoAd? = null
    //private  var mInterstitialAd: InterstitialAd? = null

    lateinit var listAdapter: ArrayAdapter<String>
    private var channelid: String? = null
    private var playlistid: String? = null
    fun fetchSubscriptionState(): Boolean {
        val subscription = getSharedPreferences("subscription", Context.MODE_PRIVATE)
        val has: Boolean = subscription.getBoolean("has", false)
        return has;
    }

    fun saveAds(){

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

    override fun onStart() {
        super.onStart()

    }

    fun checkAds(itemModel:VideoModel){
        if(doesAdsReview()) {
        if (BuildConfig.DEBUG) {
            Log.e("SİMİN WARNING","Debug olduğu  için reklam kaldırıldı")
            goNext(itemModel);
        }
        else{

                if(!fetchSubscriptionState()) {


                    try {
                        mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this)
                        mRewardedVideoAd?.let {

                            it.rewardedVideoAdListener = object : RewardedVideoAdListener {
                                override fun onRewardedVideoAdClosed() {
                                }

                                override fun onRewardedVideoAdLeftApplication() {
                                }

                                override fun onRewardedVideoAdLoaded() {
                                    it.show()
                                }

                                override fun onRewardedVideoAdOpened() {
                                }

                                override fun onRewardedVideoCompleted() {
                                }

                                override fun onRewarded(reward: RewardItem?) {
                                    adsCheckpoint()
                                    goNext(itemModel);
                                }

                                override fun onRewardedVideoStarted() {
                                }

                                override fun onRewardedVideoAdFailedToLoad(p0: Int) {
                                    goNext(itemModel);
                                }
                            }

                            it.loadAd(getString(R.string.rewarded_search), AdRequest.Builder().build())

                        }

                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        goNext(itemModel);
                    }
                }
                else{
                    goNext(itemModel);
                }
            }

        }
        else{
            goNext(itemModel)
        }
    }
    fun goNext(itemModel:VideoModel){
        progressBar.visibility=View.GONE
        var intent = Intent(this@VideoListActivity, PreviewVideo::class.java)
        intent.putExtra("videoid", itemModel.videoid)
        intent.putExtra("title", itemModel.title)
        intent.putExtra("description", itemModel.description)
        intent.putExtra("cover", itemModel.cover)
        startActivity(intent)
    }
    fun InitList() {
        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL


        rvList.setLayoutManager(linearLayoutManager)
        rvList.adapter = adapter
        adapter.onClickItem = object : YoutubeVideoAdapter.OnItemClickListener {
            override fun onClick(itemModel: VideoModel) {
                progress.visibility= View.VISIBLE
                checkAds(itemModel)
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

    fun loadMore() = safeAsync {
        loading = true
        var result: Paged<String, VideoModel>
        if (!playlistid.isNullOrEmpty()) {
            result = DiscoveryRepository.playlistItems(playlistid ?: "", nextPageToken)
        } else {
            result = DiscoveryRepository.channelVideos(channelid ?: "", nextPageToken)
        }


        if (result.isLastPage) {
            isHasLoadedAll = true
        }
        nextPageToken = result.index

        launch(UI) {
            result.items?.let {
                adapter.addData(it)
                adapter.notifyDataSetChanged()
            }
        }.join()
        loading = false
    }


}
