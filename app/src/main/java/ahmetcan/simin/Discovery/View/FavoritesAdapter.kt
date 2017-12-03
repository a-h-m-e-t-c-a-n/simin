package ahmetcan.simin.Discovery.View

import ahmetcan.simin.Discovery.Model.VideoModel
import ahmetcan.simin.R
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_video.view.*

open class FavoritesAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var items :ArrayList<VideoModel> = ArrayList()

    interface OnItemClickListener {
        fun onClick(itemModel: VideoModel)
    }
    public var onClickItem: OnItemClickListener? = null


    fun addData(data:ArrayList<VideoModel>) {
        items.addAll(data)
    }
    fun clearData() {
        items.clear()
    }

    override fun getItemCount(): Int =items.count()


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
       if(position<items.count()) {
           (holder as DefaultItemHolder).bindView(items[position])
       }

    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent?.context)
                .inflate(R.layout.item_video, parent, false)
        return DefaultItemHolder(view)
    }

    override fun getItemViewType(position: Int): Int {
        if(position%3==0){
            return 1
        }
        else{
            return 0
        }
    }

    inner class DefaultItemHolder(view: View) : RecyclerView.ViewHolder(view) {
        lateinit var item: VideoModel
        init{
            view.setOnClickListener {
               this@FavoritesAdapter.onClickItem?.onClick(items[adapterPosition])
            }
        }

        fun bindView(item: VideoModel) {
            this.item = item
            with(item){
                itemView.ivCover.setImageURI(cover)
                itemView.tvTopText.text=topText
                itemView.tvBottomText.text=bottomText
            }
        }
    }
}