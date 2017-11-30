package ahmetcan.simin.Discovery.Mock

import ahmetcan.simin.ACApplication
import ahmetcan.simin.Discovery.Model.Paged
import ahmetcan.simin.Discovery.Model.PlayListModel
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch

object  DiscoveryRepository{

    fun loadLists(page:Int): Deferred<Paged<Int,PlayListModel>> = async{
        var context: Context = ACApplication.instance

        Thread.sleep(1000) //simulate network access

        var sampleData:Paged<Int,PlayListModel> = Paged<Int,PlayListModel>(0,items = ArrayList(),isLastPage = false)

        for (item in 1..25) {
            var imjPath = "asset:///stock/" + (item % 11 + 1) + ".png"
            var item = PlayListModel(imjPath,topText = "", bottomText = "",topRightText = "")
            sampleData.items?.add(item)
        }
        if(page>3)sampleData.isLastPage=true


        return@async sampleData
    }
    fun invalidateLists()= launch{

    }
}