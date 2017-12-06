package ahmetcan.simin

import android.util.Log
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

              //  Crashlytics.logException(ex)
                throw ex
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

           // Crashlytics.logException(ex)
            Log.e("logAsync:",ex.toString())
            throw ex
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