package ahmetcan.simin.Discovery.View

import ahmetcan.simin.ACApplication
import ahmetcan.simin.Discovery.Model.VideoModel
import ahmetcan.simin.R
import android.app.Application
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_video.view.*
import android.widget.LinearLayout




open class YoutubeVideoAdapter : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
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


    override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
       if(position<items.count()) {
           (holder as DefaultItemHolder).bindView(items[position])
       }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent?.context)
                .inflate(R.layout.item_video, parent, false)
//        if(viewType==1){
//
//            val adView = AdView(ACApplication.instance)
//            adView.setLayoutParams(LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
//                    ViewGroup.LayoutParams.WRAP_CONTENT))
//            adView.adSize = AdSize.BANNER
//            adView.adUnitId ="ca-app-pub-3787646629594216/7249545730"
//            val adRequest = AdRequest.Builder().build()
//            adView.loadAd(adRequest)
//            view.findViewById<LinearLayout>(R.id.item_video_container).addView(adView)
//        }


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

    inner class DefaultItemHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        lateinit var item: VideoModel
        init{
            view.setOnClickListener {
               this@YoutubeVideoAdapter.onClickItem?.onClick(items[adapterPosition])
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