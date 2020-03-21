package ac.e.myapplication

import ahmetcan.simin.R
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.webkit.*
import java.util.*


/**
 * TODO: document your custom view class.
 */
class YTIFramePlayer : WebView {
    class WebViewInterface {
        var timeChanged:((seconds:Double)->Unit)?=null
        var playing:(()->Unit)?=null
        var paused:(()->Unit)?=null
        var ready:(()->Unit)?=null
        var unstarted:(()->Unit)?=null


        @JavascriptInterface
        fun TimeChanged(time: Double) {
            Log.w("ahmetcan",time.toString())
            timeChanged?.invoke(time)
        }
        @JavascriptInterface
        fun Playing() {
            playing?.invoke()
        }
        @JavascriptInterface
        fun Paused() {
            paused?.invoke()
        }
        @JavascriptInterface
        fun Ready() {
            ready?.invoke()
        }
        @JavascriptInterface
        fun Unstarted() {
            unstarted?.invoke()
        }
    }
    val Callbacks=WebViewInterface()
    fun loadVideo(videoId:String){
        loadUrl("javascript:loadVideo('"+videoId+"')")
    }
    fun play(){
        loadUrl("javascript:playVideo()")
    }
    fun pause(){
        loadUrl("javascript:pauseVideo()")
    }
    fun seekTo(seconds:Double){
        loadUrl("javascript:seekTo("+(seconds)+")")
    }
    constructor(context: Context) : super(context) {
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    }
    fun readPlayerHtml():String {
        try {
            var res = getResources();
            var in_s = res.openRawResource(R.raw.player);


            var s =  Scanner(in_s).useDelimiter("\\A");
            return s.next();

        } catch (e: java.lang.Exception) {
            // e.printStackTrace();

        }
        return ""
    }

    fun loadFromHtml(){
        getSettings().setMediaPlaybackRequiresUserGesture(false);

        //build your own src link with your video ID
        //build your own src link with your video ID
        val videoStr = readPlayerHtml()
        setWebChromeClient(WebChromeClient())
        setWebViewClient(object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return false
            }
        })
        val ws: WebSettings = getSettings()
        ws.javaScriptEnabled = true
        addJavascriptInterface(Callbacks, "Android")
        loadData(videoStr, "text/html", "utf-8")
    }


}
