package ahmetcan.echo

import android.app.Activity
import billing.BillingManager
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase


class ACPremium(activity: Activity,callback:IState) {
    interface IState{
        fun onPremiumChanged(isPremium:Boolean)
        fun onUserCancelFlow()
    }
    var callback:IState=callback
    var billingManager = BillingManager(activity, object : BillingManager.BillingUpdatesListener {
        override fun onBillingClientSetupFinished() {
        }

        override fun onConsumeFinished(token: String?, result: Int) {
        }
        override fun onUserCancelFlow() {
            callback.onUserCancelFlow()
        }
        override fun onPurchasesUpdated(purchases: MutableList<Purchase>?) {
            if(purchases?.count()?:0>0 ){
                callback.onPremiumChanged(true)
            }
            else{
                callback.onPremiumChanged(false)
            }
        }

    })


    fun buyPremium() {

        billingManager.initiatePurchaseFlow("noads3",BillingClient.SkuType.INAPP)
    }


    fun destroy() {
        billingManager.destroy()
    }

}