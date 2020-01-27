package ahmetcan.echo

import android.app.Activity
import android.os.Bundle
import billing.BillingManager
import com.android.billingclient.api.*
import com.google.firebase.analytics.FirebaseAnalytics


class ACPremium(var activity: Activity,callback:IState) {
    interface IState{
        fun onPremiumChanged(isPremium:Boolean)
        fun onUserCancelFlow()
        fun onError()
    }
    var skuId="premium1"

    var callback:IState=callback
    var billingManager = BillingManager(activity, object : BillingManager.BillingUpdatesListener {
        override fun onError() {
            callback.onError();
        }

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
        billingManager.querySkuDetailsAsync(BillingClient.SkuType.SUBS, listOf(skuId),object : SkuDetailsResponseListener {
            override fun onSkuDetailsResponse(result: BillingResult?, skuDetails:  MutableList<SkuDetails>?) {
                if(skuDetails!=null && skuDetails.count()>0){
                    billingManager.initiatePurchaseFlow(skuDetails!!.first())

                    val params = Bundle()
                    params.putString("sku", skuDetails!!.first().sku)
                    FirebaseAnalytics.getInstance(activity).logEvent("buyPremium_initiatePurchaseFlow", params)

                }

            }

        });
    }


    fun destroy() {
        billingManager.destroy()
    }

}