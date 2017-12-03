package ahmetcan.simin.Discovery.Model.persistent

import io.realm.RealmObject

open class Language(): RealmObject(){

    lateinit var isoCode:String
    lateinit var DisplayName:String
    var available:Boolean=false
    var default:Boolean=false
}