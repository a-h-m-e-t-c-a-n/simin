package ahmetcan.simin

import ahmetcan.simin.Discovery.Model.VideoModel
import ahmetcan.simin.Discovery.Real.DiscoveryRepository
import ahmetcan.simin.Discovery.View.FavoritesAdapter
import ahmetcan.simin.Discovery.View.YoutubeVideoAdapter
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import com.paginate.Paginate
import com.simin.CategoryView.YoutubePlaylistAdapter
import kotlinx.android.synthetic.main.fragment_favories.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch

class FavoriesFragment : FragmentBase()  {

    var adapter= FavoritesAdapter()
    var loading:Boolean=false
    var isHasLoadedAll:Boolean=false
    var page=0
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true);

        val linearLayoutManager = LinearLayoutManager(this@FavoriesFragment.context)
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL

        rvList.setLayoutManager(linearLayoutManager)
        rvList.adapter=adapter
        adapter.onClickItem=object :  FavoritesAdapter.OnItemClickListener{
            override fun onClick(itemModel: VideoModel) {
                var intent= Intent(this@FavoriesFragment.activity, PreviewVideo::class.java)
                intent.putExtra("videoid",itemModel.videoid)
                intent.putExtra("title",itemModel.title)
                intent.putExtra("description",itemModel.description)
                intent.putExtra("cover",itemModel.cover)
                startActivity(intent)
            }

        }

        async {
            var favs=DiscoveryRepository.favorites()
            onUI {
                adapter.addData(favs)
                adapter.notifyDataSetChanged()
            }
        }
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