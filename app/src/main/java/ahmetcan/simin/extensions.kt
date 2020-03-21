import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.lang.Exception
import java.math.BigInteger
import java.security.MessageDigest

fun <R> absorbError(block: () -> R): R? {
    try {
        return block();
    }
    catch (ex: Exception){
        var absorbedException= Exception((block.javaClass?.enclosingMethod?.name?:"")+"[absorbed]",ex)
        FirebaseCrashlytics.getInstance().recordException(absorbedException)
        Log.e("ahmetcan","absorbError",absorbedException)
    }
    return null
}
fun String.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(toByteArray())).toString(16).padStart(32, '0')
}