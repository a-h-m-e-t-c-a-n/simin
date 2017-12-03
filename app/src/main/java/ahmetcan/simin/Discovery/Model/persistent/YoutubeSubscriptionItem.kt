package ahmetcan.simin.Discovery.Model.persistent

import io.realm.RealmObject

open class YoutubeSubscriptionItem(): RealmObject(){

    var id:String?=null
    var cover:String?=null
    var title:String?=null
    var description:String?=null
    var itemCount:Long=0

}