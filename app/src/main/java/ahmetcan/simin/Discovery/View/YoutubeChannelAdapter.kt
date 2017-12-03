package ahmetcan.simin.Discovery.View

import ahmetcan.simin.Discovery.Model.PlayListModel
import ahmetcan.simin.Discovery.Model.VideoModel
import ahmetcan.simin.R
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_channel.view.*

open class YoutubeChannelAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var items :ArrayList<PlayListModel> = ArrayList()
    interface OnItemClickListener {
        fun onClick(itemModel: PlayListModel)
    }
    public var onClickItem: OnItemClickListener? = null


    fun addData(data:ArrayList<PlayListModel>) {
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
                .inflate(R.layout.item_channel, parent, false)
        return DefaultItemHolder(view)
    }

    inner class DefaultItemHolder(view: View) : RecyclerView.ViewHolder(view) {
        lateinit var item: PlayListModel
        init{
            view.setOnClickListener {
               if(onClickItem!=null)onClickItem?.onClick(items[adapterPosition])
            }
        }
        fun bindView(item: PlayListModel) {
            this.item = item
            with(item){
                itemView.ivCover.setImageURI(cover)
                itemView.tvTopText.text=topText
                itemView.tvBottomText.text=bottomText
                itemView.tvTopRightText.text=topRightText
            }
        }
    }
}