package ahmetcan.simin

import ahmetcan.simin.Discovery.Model.VideoModel
import ahmetcan.simin.Discovery.Real.DiscoveryRepository
import ahmetcan.simin.Discovery.View.FavoritesAdapter
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.android.synthetic.main.fragment_favories.*
import kotlinx.coroutines.*


class FavoriesFragment : FragmentBase()  {
    var scope = MainScope() + CoroutineExceptionHandler { _, ex ->
        FirebaseCrashlytics.getInstance().recordException(ex)
        Log.e("ahmetcan", "FavoriesFragment main scope exception [blocked]", ex)
    }
    var adapter= FavoritesAdapter()
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(false);

        val linearLayoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@FavoriesFragment.context)
        linearLayoutManager.orientation = androidx.recyclerview.widget.LinearLayoutManager.VERTICAL

        rvList.setLayoutManager(linearLayoutManager)
        rvList.adapter=adapter
        adapter.onClickItem=object :  FavoritesAdapter.OnItemClickListener{
            override fun onClick(itemModel: VideoModel) {
                var intent= Intent(this@FavoriesFragment.activity, PreviewVideoWeb::class.java)
                intent.putExtra("videoid",itemModel.videoid)
                intent.putExtra("title",itemModel.title)
                intent.putExtra("description",itemModel.description)
                intent.putExtra("cover",itemModel.cover)
                startActivity(intent)
            }

        }
        swipeRefreshLayout.setOnRefreshListener {
            scope.launch {
                var favs= withContext(Dispatchers.IO){DiscoveryRepository.favorites()}
                    adapter.clearData()
                    adapter.addData(favs)
                    adapter.notifyDataSetChanged()
                    swipeRefreshLayout.setRefreshing(false);

                }

        }
        scope.launch {
            var favs= withContext(Dispatchers.IO){DiscoveryRepository.favorites()}
            adapter.clearData()
            adapter.addData(favs)
            adapter.notifyDataSetChanged()
            swipeRefreshLayout.setRefreshing(false);

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel("ondestroy")
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater!!.inflate(R.layout.fragment_favories, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        activity?.menuInflater?.inflate(R.menu.home, menu)
    }


}