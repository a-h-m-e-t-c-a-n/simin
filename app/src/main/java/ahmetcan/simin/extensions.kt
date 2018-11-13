package ahmetcan.simin

import android.util.Log
import com.google.firebase.crash.FirebaseCrash
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.UI
import kotlin.coroutines.experimental.CoroutineContext

//async exception fırlattığında uygulama crash olmuyor ama launc da oluyor
fun logLaunch(block: suspend CoroutineScope.() -> Unit)
        : Job {
    return launch(Dispatchers.IO) {
        try {
            block()
        }
        catch (ex:Exception){
            try{
                FirebaseCrash.report(ex)
            }
            finally {
                Log.e("translationplayer logAsync:",ex.toString())
                throw ex
            }
        }
    }

}
fun logAsync(block: suspend CoroutineScope.() -> Unit)
        : Job {
    return async(Dispatchers.IO) {
        try {
            block()
        }
        catch (ex:Exception){
            try{
                FirebaseCrash.report(ex)
            }
            finally {
                Log.e("translationplayer logAsync:",ex.toString())
                throw ex
            }
        }
    }

}
fun onUI(block: suspend CoroutineScope.() -> Unit)
        : Job {
    return async(Dispatchers.Main) {
        block()
    }

}