package ahmetcan.simin

import ahmetcan.simin.Discovery.Real.DiscoveryRepository
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.inputmethod.InputMethodManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext


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
                    FirebaseCrashlytics.getInstance().recordException(ex)
                }
                finally {
                    Log.e("logAsync:",ex.toString())
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
    fun adsCheckpoint() {
        val subscription = getSharedPreferences("ads", Context.MODE_PRIVATE)
        val edit = subscription.edit()
        edit.putLong("last", Date().time)
        edit.commit()
    }
    fun doesAdsReview():Boolean {
        val subscription = getSharedPreferences("ads", Context.MODE_PRIVATE)

        var last =subscription.getLong("last", 0)
        if(last==0L){
            adsCheckpoint()
            return false
        };
        var asMinute = TimeUnit.MILLISECONDS.toMinutes(Calendar.getInstance().timeInMillis - last)
        if(asMinute>3){
            return true;
        }
        else{
            return false;
        }

    }
}