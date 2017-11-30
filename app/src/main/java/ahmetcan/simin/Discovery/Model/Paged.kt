package ahmetcan.simin.Discovery.Model

import io.realm.RealmObject

open class Paged<K,T>(var index:K,var isLastPage:Boolean=false, var items:ArrayList<T>)