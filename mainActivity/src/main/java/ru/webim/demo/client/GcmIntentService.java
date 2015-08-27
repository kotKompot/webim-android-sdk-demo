package ru.webim.demo.client;

import ru.webim.android.sdk.WMSession;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

/**
 * This {@code IntentService} does the actual handling of the GCM message.
 * {@code GcmBroadcastReceiver} (a {@code WakefulBroadcastReceiver}) holds a
 * partial wake lock for this service while the service does its work. When the
 * service is finished, it calls {@code completeWakefulIntent()} to release the
 * wake lock.
 */
public class GcmIntentService extends IntentService {
	private static String mySenderId = "YOUR_SENDER_ID";

	public GcmIntentService() {
		super(mySenderId);
	}
	
    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        String senderId = intent.getStringExtra("from");
        if (!senderId.equals(mySenderId)){
        	if(WebimSDKApplication.isInBackground())
        		WMSession.onPushMessage(this.getApplicationContext(), extras, MainActivity.class);
        } else {
	       // Your push logic 
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    } 
}