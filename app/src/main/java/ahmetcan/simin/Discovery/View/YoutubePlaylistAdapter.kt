package com.simin.CategoryView

import ahmetcan.simin.Discovery.Model.PlayListModel
import ahmetcan.simin.R
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_playlist.view.*

open class YoutubePlaylistAdapter : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
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


    override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
       if(position<items.count()) {
           (holder as DefaultItemHolder).bindView(items[position])
       }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent?.context)
                .inflate(R.layout.item_playlist, parent, false)
        return DefaultItemHolder(view)
    }

    inner class DefaultItemHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        lateinit var item:PlayListModel
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
