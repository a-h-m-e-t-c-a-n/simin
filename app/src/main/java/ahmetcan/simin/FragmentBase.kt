package ahmetcan.simin

import androidx.fragment.app.Fragment
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.*
import kotlin.coroutines.*

open class FragmentBase : Fragment() {
    private var jobs= arrayListOf<Job>()
    fun safeAsync(context: CoroutineContext = DefaultDispatcher,
                  start: CoroutineStart = CoroutineStart.DEFAULT,
                  block: suspend CoroutineScope.() -> Unit)
            : Job
    {
        // var job= logAsync(context, start, block)
        var job=   async(context,start) {
            try {

                block()
            }
            catch (ex:Exception){

                try{
                    FirebaseCrashlytics.getInstance().recordException(ex)
                }
                finally {
                    Log.e("simin logAsync:",ex.toString())
                    throw ex
                }
            }
        }
        jobs.add(job)
        job.invokeOnCompletion {
            jobs.remove(job)
        }
        return job
    }
    override fun onDestroy() {
        super.onDestroy()

        jobs.forEach {it.cancel()}
    }

}
