package ahmetcan.simin

import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import kotlinx.coroutines.experimental.*
import kotlin.coroutines.experimental.CoroutineContext
import android.content.Context.INPUT_METHOD_SERVICE
import android.view.inputmethod.InputMethodManager


open class ActivityBase : AppCompatActivity() {
    private var jobs= arrayListOf<Job>()

    fun safeAsync(context: CoroutineContext = DefaultDispatcher,
                   start: CoroutineStart = CoroutineStart.DEFAULT,
                   block: suspend CoroutineScope.() -> Unit)
            : Job
    {
        var job= logAsync(context, start, block)
        //jobs.add(job)
        //job.invokeOnCompletion {
            //jobs.remove(job)
        //}
        return job
    }

    override fun onDestroy() {
        super.onDestroy()

        jobs.forEach {it.cancel()}
    }

    fun hideSoftKeyboard() {
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0)
    }

}