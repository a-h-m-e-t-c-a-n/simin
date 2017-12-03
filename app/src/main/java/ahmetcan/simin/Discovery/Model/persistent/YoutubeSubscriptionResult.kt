package ahmetcan.simin.Discovery.Model.persistent

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class YoutubeSubscriptionResult(): RealmObject(){
    @PrimaryKey
    var id:Int=0
    var nextPageToken:String?=null
    var items: RealmList<YoutubeSubscriptionItem>?= RealmList()
    var totalResults=0
    var resultsPerPage=0
}