package ahmetcan.simin.Discovery.Model.persistent

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class VideoViewState(): RealmObject(){
    @PrimaryKey
    var videoId:String=""
    var cover:String=""
    var title:String=""
    var description:String=""
    var secondaryLanguageIso:String=""
    var captionIndex:Int=0
    var captionOffMode:Boolean=false
    var showCaption:Boolean=false
    var showSecondSubtitle:Boolean=false
    var syncSubtitle:Boolean=false
}