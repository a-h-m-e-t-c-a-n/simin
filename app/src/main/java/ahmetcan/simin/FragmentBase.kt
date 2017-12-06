package ahmetcan.simin

import android.support.v4.app.Fragment
import android.util.Log
import com.google.firebase.crash.FirebaseCrash
import kotlinx.coroutines.experimental.*
import kotlin.coroutines.experimental.*

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
                    FirebaseCrash.log(ex.toString());
                }
                finally {
                    Log.e("simin logAsync:",ex.toString())
                    throw ex
                }
                throw ex
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
