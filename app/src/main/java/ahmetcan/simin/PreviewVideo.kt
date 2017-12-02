package ahmetcan.simin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import com.google.android.youtube.player.YouTubeBaseActivity
import com.google.android.youtube.player.YouTubeInitializationResult
import com.google.android.youtube.player.YouTubePlayer
import kotlinx.android.synthetic.main.activity_preview_video.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import android.content.ActivityNotFoundException
import android.graphics.Color
import android.speech.RecognizerIntent
import android.widget.CompoundButton
import android.widget.RadioGroup
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds


class PreviewVideo : YouTubeBaseActivity(),  YouTubePlayer.OnInitializedListener , YouTubePlayer.OnFullscreenListener {
    private var fullscreen: Boolean = false
    private var player:YouTubePlayer?=null
    private var listenPlayerJob: Job?=null;
    private var captionOff:Boolean=true
    protected val RESULT_SPEECH = 2
    companion object {
        const val  RECOVERY_DIALOG_REQUEST = 1;
    }
    fun listenPlayer(player:YouTubePlayer)= async{
        var currentTime:Int=0
        while(this@PreviewVideo.playerView!=null){
            if(player.isPlaying){
                var playerTime=player.currentTimeMillis
                if(playerTime!=currentTime){
                    currentTime=playerTime
                    launch(UI) {
                        onPlayerTimeChanged(currentTime)
                    }

                }
            }
            delay(100)
        }
    }
    fun onPlayerTimeChanged(time:Int){
        Log.i("A------------>",time.toString())
        if(time > 6000){
//            if(adIntercept){
//                if(mInterstitialAd?.isLoaded==true){
//                    adIntercept=false
//                    player?.pause()
//                    mInterstitialAd?.show()
//                }
//            }

        }
    }
    override fun onInitializationSuccess(p0: YouTubePlayer.Provider?, pl: YouTubePlayer?, wasRestored: Boolean) {
        player=pl
        player?.addFullscreenControlFlag(YouTubePlayer.FULLSCREEN_FLAG_ALWAYS_FULLSCREEN_IN_LANDSCAPE)
        player?.setOnFullscreenListener(this)
        player?.let {
            listenPlayerJob=listenPlayer(it)
        }
        if (!wasRestored) {
            player?.loadVideo(intent.extras["videoid"] as? String);
        }
    }



    override fun onInitializationFailure(provider: YouTubePlayer.Provider,
                                         errorReason: YouTubeInitializationResult) {
        if (errorReason.isUserRecoverableError) {
            errorReason.getErrorDialog(this, RECOVERY_DIALOG_REQUEST).show()
        } else {
            val errorMessage = String.format(getString(R.string.error_player), errorReason.toString())
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        when (requestCode) {
            RECOVERY_DIALOG_REQUEST -> {
                // Retry initialization if user performed a recovery action
                playerView.initialize(ApiKey.YOUTUBEDATAAPIV3_KEY, this)
            }
            RESULT_SPEECH -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.let {
                        val text = it.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)

                        Log.e("AAAAA->>>>>>>>>>>>",text[0])
                    }


                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_preview_video)



        playerView.initialize(ApiKey.YOUTUBEDATAAPIV3_KEY, this);

        video_SpeechTest.setOnClickListener {

            val intent = Intent( RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US")

            try {
                startActivityForResult(intent, RESULT_SPEECH)

            } catch (a: ActivityNotFoundException) {
                val t = Toast.makeText(applicationContext,"Opps! Your device doesn't support Speech to Text",Toast.LENGTH_SHORT)
                t.show()
            }

        }
        backButton.setOnClickListener {
            finish()
        }
        MobileAds.initialize(this, ApiKey.ADMOB_APPID);
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        video_delayButton.setOnCheckedChangeListener(object :CompoundButton.OnCheckedChangeListener{
            override fun onCheckedChanged(button: CompoundButton?, state: Boolean) {
                if(state){
                    button?.setTextColor(Color.RED)
                }
                else{
                    button?.setTextColor(Color.BLACK)
                }

            }

        })
    }


    private fun doLayout() {
        val playerParams = playerView.getLayoutParams() as LinearLayout.LayoutParams
        if (fullscreen) {
//            // When in fullscreen, the visibility of all other views than the player should be set to
//            // GONE and the player should be laid out across the whole screen.
//            playerParams.width = ViewGroup.LayoutParams.MATCH_PARENT
//            playerParams.height = ViewGroup.LayoutParams.MATCH_PARENT

            info.setVisibility(View.GONE)
        } else {
            // This layout is up to you - this is just a simple example (vertically stacked boxes in
            // portrait, horizontally stacked in landscape).
            info.setVisibility(View.VISIBLE)
//            val otherViewsParams = info.getLayoutParams()
//            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
//                otherViewsParams.width = 0
//                playerParams.width = otherViewsParams.width
//                playerParams.height = WRAP_CONTENT
//                otherViewsParams.height = MATCH_PARENT
//                playerParams.weight = 1f
//                baseLayout.setOrientation(LinearLayout.HORIZONTAL)
//            } else {
//                otherViewsParams.width = MATCH_PARENT
//                playerParams.width = otherViewsParams.width
//                playerParams.height = WRAP_CONTENT
//                playerParams.weight = 0f
//                otherViewsParams.height = 0
//                baseLayout.setOrientation(LinearLayout.VERTICAL)
//            }
        }
    }

    override fun onFullscreen(isFullscreen: Boolean) {
        fullscreen = isFullscreen;
        doLayout();
    }

    override fun onDestroy() {
        super.onDestroy()
      //  listenPlayerJob?.cancel()
    }
}
