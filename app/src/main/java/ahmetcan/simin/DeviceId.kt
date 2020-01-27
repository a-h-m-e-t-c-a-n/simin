package ahmetcan.simin


import android.content.Context
import android.content.ContextWrapper
import android.provider.Settings
import java.util.*


class DeviceId(context:Context):ContextWrapper(context) {

    fun getId():String{
        val sharedPref = getSharedPreferences("settings", Context.MODE_PRIVATE)
        var deviceId=sharedPref.getString("deviceid",null)
        if(deviceId!=null){
            return deviceId
        }
        else{
            deviceId=getSecureId()
            if(deviceId!=null){

            }
            else{
                deviceId= UUID.randomUUID().toString()
            }
            with (sharedPref.edit()) {
                putString("deviceid", deviceId)
                commit()
            }
            return deviceId

        }
    }
    fun getSecureId():String?{
        try{
            return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        }
        catch (ex:Exception){
            return null;
        }

    }

}