package ahmetcan.simin.Discovery

import ahmetcan.simin.Discovery.Model.PlayListModel
import ahmetcan.simin.Discovery.Model.persistent.VideoViewState
import ahmetcan.simin.Discovery.Real.DiscoveryRepository
import ahmetcan.simin.Discovery.View.YoutubeChannelAdapter
import ahmetcan.simin.FragmentBase
import ahmetcan.simin.R
import ahmetcan.simin.VideoListActivity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import com.paginate.Paginate
import com.simin.CategoryView.YoutubePlaylistAdapter
import io.realm.Realm
import kotlinx.android.synthetic.main.fragment_discovery.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch


class DiscoveryFragment : FragmentBase()  {

    var adapter= YoutubePlaylistAdapter()
    var loading:Boolean=false
    var isHasLoadedAll:Boolean=false
    var page=0
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true);

        val linearLayoutManager = LinearLayoutManager(this@DiscoveryFragment.context)
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL

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
    fun loadMore() =safeAsync {
        loading=true
        var result = DiscoveryRepository.loadLists(page)
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
       DiscoveryRepository.invalidateLists()
       var result = DiscoveryRepository.loadLists(page)
       isHasLoadedAll=result.isLastPage


       launch(UI) {
           result.items?.let {
               adapter.clearData()
               adapter.addData(it)
               adapter.notifyDataSetChanged()
           }
           page++
           loading=false
       }.join()

//        page=0
//        isHasLoadedAll=false;
//        DiscoveryRepository.invalidateLists().join()
//        adapter.clearData()
//        launch(UI) {
//            clear().join()
//            loadMore().join()
//        }

    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater!!.inflate(R.layout.fragment_discovery, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        activity?.menuInflater?.inflate(R.menu.home, menu)
//        var inboxMenuItem = menu?.findItem(R.id.action_inbox)
//        inboxMenuItem?.setOnMenuItemClickListener {
//            async {
//                DiscoveryRepository.downloadCaption()
//            }
//
//            true
//        }
        //inboxMenuItem?.setActionView(R.layout.menu_item_view)
    }


}
