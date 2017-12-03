package ahmetcan.simin

import ahmetcan.simin.Discovery.Real.DiscoveryRepository
import ahmetcan.simin.Discovery.View.YoutubeChannelAdapter
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import com.paginate.Paginate
import com.simin.CategoryView.YoutubePlaylistAdapter
import kotlinx.android.synthetic.main.fragment_channel.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch


class ChannelFragment : FragmentBase()  {

    var adapter= YoutubeChannelAdapter()
    var loading:Boolean=false
    var isHasLoadedAll:Boolean=false
    var page=0
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true);

        val linearLayoutManager = LinearLayoutManager(this@ChannelFragment.context)
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL

        rvList.setLayoutManager(linearLayoutManager)
        rvList.adapter=adapter

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
    fun loadMore() =safeAsync {
        loading=true
        var result = DiscoveryRepository.loadChannelList(page)
        if(result.isLastPage){
            isHasLoadedAll=true
        }

        launch(UI) {
            result.items?.let {
                adapter.addData(it)
                adapter.notifyDataSetChanged()
            }
        }.join()
        page++
        loading=false
    }
    fun clear() =safeAsync {

    }

   fun refresh()= safeAsync {
       loading=true
       page=0
       isHasLoadedAll=false
       DiscoveryRepository.invalidateChannelLists()
       var result = DiscoveryRepository.loadChannelList(page)
       isHasLoadedAll=result.isLastPage


       launch(UI) {
           result.items?.let {
               adapter.clearData()
               adapter.addData(it)
               adapter.notifyDataSetChanged()
           }
           page++
           loading = false
       }.join()

    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater!!.inflate(R.layout.fragment_discovery, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        activity?.menuInflater?.inflate(R.menu.home, menu)
    }


}