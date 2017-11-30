package ahmetcan.simin.Discovery.Model.persistent

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class YoutubePlaylist(): RealmObject(){

    var id:String?=null
    var cover:String?=null
    var title:String?=null
    var description:String?=null
    var itemCount:Long=0

}