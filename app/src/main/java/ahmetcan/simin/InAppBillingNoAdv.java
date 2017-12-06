package ahmetcan.simin;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import InAppBilling.IabBroadcastReceiver;
import InAppBilling.IabHelper;
import InAppBilling.IabResult;
import InAppBilling.Inventory;
import InAppBilling.Purchase;

public class InAppBillingNoAdv implements IabBroadcastReceiver.IabBroadcastListener {
    public interface IOnSubscriptionStateChanged {
        void action(Boolean isSubscripted);
    }

    public IOnSubscriptionStateChanged onSubsctiptionStateChangedListener;
    public IOnSubscriptionStateChanged onBuyCompleted;

    public void setOnSubscriptionStateChanged(IOnSubscriptionStateChanged callback) {
        onSubsctiptionStateChangedListener = callback;
    }
    public void setOnBuyCompleted(IOnSubscriptionStateChanged callback) {
        onBuyCompleted = callback;
    }

    static final String TAG = "Simin NoAds Billing";
    public boolean isSubscriped = false;
    static final String SKU_NOADV_MONTHLY = "noadssub";

    static final int RC_REQUEST = 10001;

    IabHelper mHelper;
    IabBroadcastReceiver mBroadcastReceiver;

    Activity context = null;

    public void Init(final Activity context, final IabBroadcastReceiver.IabBroadcastListener listener) {

        /* base64EncodedPublicKey should be YOUR APPLICATION'S PUBLIC KEY
         * (that you got from the Google Play developer console). This is not your
         * developer public key, it's the *app-specific* public key.
         *
         * Instead of just storing the entire literal string here embedded in the
         * program,  construct the key at runtime from pieces or
         * use bit manipulation (for example, XOR with some other string) to hide
         * the actual key.  The key itself is not secret information, but we don't
         * want to make it easy for an attacker to replace the public key with one
         * of their own and then fake messages from the server.
         */
        this.context = context;
        String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmkU/a3dumyBKKXQZLh4X6HAjtKXHoSnoEU3OU2/EBY1jX++vM5SV3tzgNDBp0Bonow2jyTZCQrFS8GuOWd6iG2mmjnGK92Y79RT+qQ+pMyJOzq3ChDvYNUQAyLeuAqQgWGkqnroSs8ENRngwbC1C5xARMs0Tiqr891ijw1tRgwXYvHl+FA/9ARtgc9mOCVh4i96JwcYirVlY9I98ciG1O5iMJGcOeRrqfJWWB6uxUEqGa++A9wb3k0YIIH0kS2FI3+fAVtuR3oAuv2sjM3VCAkYgFRAbhcb5kTjlZxwDVljFTPMTvt8KF3lSm8Q3OGjnxdbEN0GeVIK1504NGdKk/wIDAQAB";


        mHelper = new IabHelper(context, base64EncodedPublicKey);

        // enable debug logging (for a production application, you should set this to false).
        mHelper.enableDebugLogging(true);

        // Start setup. This is asynchronous and the specified listener
        // will be called once setup completes.
        Log.d(TAG, "Starting setup.");
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Log.d(TAG, "Setup finished.");

                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.

                    Log.e(TAG, "Problem setting up in-app billing: " + result);
                    return;
                }

                // Have we been disposed of in the meantime? If so, quit.
                if (mHelper == null) return;

                // Important: Dynamically register for broadcast messages about updated purchases.
                // We register the receiver here instead of as a <receiver> in the Manifest
                // because we always call getPurchases() at startup, so therefore we can ignore
                // any broadcasts sent while the app isn't running.
                // Note: registering this listener in an Activity is a bad idea, but is done here
                // because this is a SAMPLE. Regardless, the receiver must be registered after
                // IabHelper is setup, but before first call to getPurchases().
                mBroadcastReceiver = new IabBroadcastReceiver(listener);
                IntentFilter broadcastFilter = new IntentFilter(IabBroadcastReceiver.ACTION);
                context.registerReceiver(mBroadcastReceiver, broadcastFilter);

                // IAB is fully set up. Now, let's get an inventory of stuff we own.
                Log.d(TAG, "Setup successful. Querying inventory.");
                try {
                    mHelper.queryInventoryAsync(mGotInventoryListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    Log.e(TAG, "Error querying inventory. Another async operation in progress.");
                }
            }
        });
    }

    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(TAG, "Query inventory finished.");

            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) return;

            // Is it a failure?
            if (result.isFailure()) {
                Log.e(TAG, "Failed to query inventory: " + result);
                return;
            }

            Log.d(TAG, "Query inventory was successful.");


            Purchase noadvMonthly = inventory.getPurchase(SKU_NOADV_MONTHLY);

            // The user is subscribed if either subscription exists, even if neither is auto
            // renewing
            isSubscriped = (noadvMonthly != null && verifyDeveloperPayload(noadvMonthly));
            if (onSubsctiptionStateChangedListener != null)
                onSubsctiptionStateChangedListener.action(isSubscriped);

            Log.d(TAG, "Initial inventory query finished; enabling main UI.");
        }
    };

    @Override
    public void receivedBroadcast() {
        // Received a broadcast notification that the inventory of items has changed
        Log.d(TAG, "Received broadcast notification. Querying inventory.");
        try {
            mHelper.queryInventoryAsync(mGotInventoryListener);
        } catch (IabHelper.IabAsyncInProgressException e) {
            Log.e(TAG, "Error querying inventory. Another async operation in progress.");
        }
    }

    // User clicked the "Buy Gas" button
    public void Buy() {


            /* TODO: for security, generate your payload here for verification. See the comments on
             *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
             *        an empty string, but on a production app you should carefully generate
             *        this. */
        String payload = "";

        Log.d(TAG, "Launching purchase flow for gas subscription.");
        try {
            mHelper.launchPurchaseFlow(context, SKU_NOADV_MONTHLY, IabHelper.ITEM_TYPE_SUBS, null, RC_REQUEST, mPurchaseFinishedListener, payload);
        } catch (IabHelper.IabAsyncInProgressException e) {
            Log.e(TAG, "Error launching purchase flow. Another async operation in progress.");
        }

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
        if (mHelper == null) return;

        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
        } else {
            Log.d(TAG, "onActivityResult handled by IABUtil.");
        }
    }

    /**
     * Verifies the developer payload of a purchase.
     */
    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();

        /*
         * TODO: verify that the developer payload of the purchase is correct. It will be
         * the same one that you sent when initiating the purchase.
         *
         * WARNING: Locally generating a random string when starting a purchase and
         * verifying ith, but this will fail in the
         * case where the user purchases an item on one device and then uses your app on
         * a different device, because on the other device you will not have access to the
         * random string you originally generated. here might seem like a good approac
         *
         * So a good developer payload has these characteristics:
         *
         * 1. If two different users purchase an item, the payload is different between them,
         *    so that one user's purchase can't be replayed to another user.
         *
         * 2. The payload must be such that you can verify it even when the app wasn't the
         *    one who initiated the purchase flow (so that items purchased by the user on
         *    one device work on other devices owned by the user).
         *
         * Using your own server to store and verify developer payloads across app
         * installations is recommended.
         */

        return true;
    }

    // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            if (result.isFailure()) {
                Log.e(TAG, "Error purchasing: " + result);
                if (onBuyCompleted != null)
                    onBuyCompleted.action(false);
                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
                Log.e(TAG, "Error purchasing. Authenticity verification failed.");
                if (onBuyCompleted != null)
                    onBuyCompleted.action(false);
                return;
            }

            Log.d(TAG, "Purchase successful.");

            if (purchase.getSku().equals(SKU_NOADV_MONTHLY)) {
                // bought the infinite gas subscription
                Log.d(TAG, "Infinite gas subscription purchased.");
                isSubscriped = true;
                if (onSubsctiptionStateChangedListener != null)
                    onSubsctiptionStateChangedListener.action(isSubscriped);

            }

        }
    };

    // Called when consumption is complete
    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {
            Log.d(TAG, "Consumption finished. Purchase: " + purchase + ", result: " + result);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            // We know this is the "gas" sku because it's the only one we consume,
            // so we don't check which sku was consumed. If you have more than one
            // sku, you probably should check...
            if (result.isSuccess()) {
                // successfully consumed, so we apply the effects of the item in our
                // game world's logic, which in our case means filling the gas tank a bit
                Log.d(TAG, "Consumption successful. Provisioning.");

            } else {
                Log.e(TAG, "Error while consuming: " + result);
            }

            Log.d(TAG, "End consumption flow.");
        }
    };


    public void destroy() {

        context.unregisterReceiver(mBroadcastReceiver);
        Log.d(TAG, "Destroying helper.");
        if (mHelper != null) {
            mHelper.disposeWhenFinished();
            mHelper = null;
        }
    }


}

