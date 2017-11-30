package ahmetcan.simin

import android.support.v4.app.Fragment
import kotlinx.coroutines.experimental.*
import kotlin.coroutines.experimental.*

open class FragmentBase : Fragment() {
    private var jobs= arrayListOf<Job>()
    fun safeAsync(context: CoroutineContext = DefaultDispatcher,
                   start: CoroutineStart = CoroutineStart.DEFAULT,
                   block: suspend CoroutineScope.() -> Unit)
            :Job
    {
        var job= logAsync(context,start,block)
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
