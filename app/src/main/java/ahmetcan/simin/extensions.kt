package ahmetcan.simin

import android.util.Log
import com.google.firebase.crash.FirebaseCrash
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.UI
import kotlin.coroutines.experimental.CoroutineContext

//async exception fırlattığında uygulama crash olmuyor ama launc da oluyor
fun logLaunch(context: CoroutineContext = DefaultDispatcher,
               start: CoroutineStart = CoroutineStart.DEFAULT,
               block: suspend CoroutineScope.() -> Unit)
        : Job {
        return launch(context,start) {
            try {
                block()
            }
            catch (ex:Exception){
                try{
                    FirebaseCrash.report(ex)
                }
                finally {
                    Log.e("simin logAsync:",ex.toString())
                    throw ex
                }
            }
    }

}
fun logAsync(context: CoroutineContext = DefaultDispatcher,
              start: CoroutineStart = CoroutineStart.DEFAULT,
              block: suspend CoroutineScope.() -> Unit)
        : Job {
    return async(context,start) {
        try {
            block()
        }
        catch (ex:Exception){
            try{
                FirebaseCrash.report(ex)
            }
            finally {
                Log.e("simin logAsync:",ex.toString())
                throw ex
            }
        }
    }

}
fun onUI(    start: CoroutineStart = CoroutineStart.DEFAULT,
             block: suspend CoroutineScope.() -> Unit)
        : Job {
    return async(UI,start) {
        block()
    }

}