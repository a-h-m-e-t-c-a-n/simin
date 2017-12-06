package ahmetcan.simin.Discovery

import ahmetcan.simin.*
import ahmetcan.simin.Api.GoogleService
import ahmetcan.simin.Discovery.Model.VideoModel
import ahmetcan.simin.Discovery.Real.DiscoveryRepository
import ahmetcan.simin.Discovery.View.YoutubeVideoAdapter
import android.content.Intent
import android.os.Bundle
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
import com.google.android.gms.ads.MobileAds
import com.google.gson.JsonArray
import com.paginate.Paginate
import kotlinx.android.synthetic.main.activity_search.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch


class SearchActivity : ActivityBase() {
    var adapter= YoutubeVideoAdapter()
    var loading:Boolean=false
    var isHasLoadedAll:Boolean=true
    var nextPageToken:String?=null
    private  var mInterstitialAd: InterstitialAd? = null

    lateinit var listAdapter: ArrayAdapter<String>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_search)
        backButton.setOnClickListener {
            finish()
        }
        autocompleteEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                //listAdapter.clear()
                async {
                    var result = GoogleService.instance.youtube_suggest(autocompleteEdit.text.toString()).execute().body()
                    onUI {
                        listAdapter.clear()
                        listAdapter.add(result?.get(0).toString().replace("\"", ""))
                        (result?.get(1) as? JsonArray)?.forEach {
                            listAdapter.add(it.toString().replace("\"", ""))
                        }
                    }
                }

            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

        })
        autocompleteEdit.setOnFocusChangeListener(object : View.OnFocusChangeListener {
            override fun onFocusChange(v: View, hasFocus: Boolean) {
                if (hasFocus) {
                    autocompleteList.visibility = View.VISIBLE
                }

            }

        })
        autocompleteEdit.setOnClickListener {
            autocompleteList.visibility = View.VISIBLE


        }
        autocompleteList.setOnItemClickListener(object : AdapterView.OnItemClickListener {
            override fun onItemClick(p0: AdapterView<*>?, p1: View?, p2: Int, position: Long) {
                val value = listAdapter.getItem(position.toInt()) as String
                autocompleteEdit.setText(value)
                autocompleteEdit.setSelection(autocompleteEdit.text.length);
                autocompleteList.visibility = View.GONE
                rvList.requestFocus()
                hideSoftKeyboard()
                performSearch()
            }

        })

        listAdapter = ArrayAdapter<String>(this, R.layout.search_autocomplete_listitem, arrayListOf(""))
        autocompleteList.adapter = listAdapter
        autocompleteEdit.setOnEditorActionListener(object :TextView.OnEditorActionListener {
            override fun onEditorAction(p0: TextView?, actionId: Int, event: KeyEvent?): Boolean {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    autocompleteList.visibility = View.GONE
                    rvList.requestFocus()
                    hideSoftKeyboard()
                    performSearch()
                    return true
                }

                return false
            }
        })
        autocompleteEdit.requestFocus()

//        MobileAds.initialize(this, ApiKey.ADMOB_APPID);
//        val adRequest = AdRequest.Builder().build()
//        adView.loadAd(adRequest)

        InitList()

        if (BuildConfig.DEBUG) {
            Log.e("SİMİN WARNING","Debug olduğu  için reklam kaldırıldı")
        }
        else{
            mInterstitialAd = InterstitialAd(this@SearchActivity);
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
        adapter.onClickItem=object :YoutubeVideoAdapter.OnItemClickListener{
            override fun onClick(itemModel: VideoModel) {
                var intent=Intent(this@SearchActivity, PreviewVideo::class.java)
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
        var result = DiscoveryRepository.search(autocompleteEdit.text.toString(),nextPageToken)
        if(result.isLastPage){
            isHasLoadedAll=true
        }
        nextPageToken=result.index

        launch(UI){
            result.items?.let {
                adapter.addData(it)
                adapter.notifyDataSetChanged()
            }
        }.join()
        loading=false
    }

    fun performSearch(){
        loading=false
        isHasLoadedAll=false
        nextPageToken=null

        adapter.clearData()
        adapter.notifyDataSetChanged()


       loadMore()
    }
}
