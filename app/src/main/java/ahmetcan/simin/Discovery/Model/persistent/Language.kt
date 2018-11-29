package ahmetcan.simin.Discovery.Model.persistent

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class Language(): RealmObject(){
    @PrimaryKey
    var uid:String=""
    var isoCode:String=""
    var DisplayName:String=""
    var available:Boolean=false
    var default:Boolean=false
}