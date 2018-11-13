package ahmetcan.simin

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.inputmethod.InputMethodManager
import com.google.firebase.crash.FirebaseCrash
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.async
import kotlin.coroutines.experimental.CoroutineContext


open class ActivityBase : AppCompatActivity(), CoroutineScope {
    lateinit var job: Job

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        job = Job()
    }

    fun safeAsync(block: suspend CoroutineScope.() -> Unit)
            : Job
    {
        // var job= logAsync(context, start, block)
        return async(Dispatchers.IO)  {
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

    override fun onDestroy() {
        super.onDestroy()

        job.cancel()
    }

    fun hideSoftKeyboard() {
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0)
    }

}