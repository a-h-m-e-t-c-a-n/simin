package ahmetcan.simin.Discovery

import ahmetcan.simin.Discovery.Model.PlayListModel
import ahmetcan.simin.Discovery.Real.DiscoveryRepository
import ahmetcan.simin.FragmentBase
import ahmetcan.simin.R
import ahmetcan.simin.VideoListActivity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.paginate.Paginate
import com.simin.CategoryView.YoutubePlaylistAdapter
import kotlinx.android.synthetic.main.fragment_discovery.*
import kotlinx.coroutines.*


class DiscoveryFragment : FragmentBase()  {
    var scope = MainScope() + CoroutineExceptionHandler { _, ex ->
        FirebaseCrashlytics.getInstance().recordException(ex)
        Log.e("ahmetcan", "ChannelFragment main scope exception [blocked]", ex)
    }
    var adapter= YoutubePlaylistAdapter()
    var loading:Boolean=false
    var isHasLoadedAll:Boolean=false
    var page=0
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true);

        val linearLayoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@DiscoveryFragment.context)
        linearLayoutManager.orientation = androidx.recyclerview.widget.LinearLayoutManager.VERTICAL

        rvList.setLayoutManager(linearLayoutManager)
        rvList.adapter=adapter
        adapter.onClickItem=object :  YoutubePlaylistAdapter.OnItemClickListener{
            override fun onClick(itemModel: PlayListModel) {
                var intent= Intent(this@DiscoveryFragment.activity, VideoListActivity::class.java)
                intent.putExtra("playlistid",itemModel.playlistid)
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

        swipeRefreshLayout.setOnRefreshListener {
            refresh()
            swipeRefreshLayout.isRefreshing=false
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel("ondestroy")
    }
    fun loadMore() = scope.launch {
        loading=true
        var result = withContext(Dispatchers.IO){DiscoveryRepository.loadLists(page)}
        if(result.isLastPage){
            isHasLoadedAll=true
        }

        result.items?.let {
            adapter.addData(it)
            adapter.notifyDataSetChanged()
        }
        page++
        loading=false
    }

   fun refresh()= scope.launch {
       loading=true
       page=0
       isHasLoadedAll=false
       DiscoveryRepository.invalidateLists()
       var result = withContext(Dispatchers.IO){DiscoveryRepository.loadLists(page)}
       isHasLoadedAll=result.isLastPage


       result.items?.let {
           adapter.clearData()
           adapter.addData(it)
           adapter.notifyDataSetChanged()
       }
       page++
       loading=false



    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_discovery, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        activity?.menuInflater?.inflate(R.menu.home, menu)

    }


}
