package app.dphone;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.microsoft.windowsazure.messaging.NotificationHub;
import java.util.concurrent.TimeUnit;


public class RegistrationIntentService extends IntentService {

    private static final String TAG = "RegIntentService";
    private String FCM_token = null;

    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String userIdentifier = intent.getStringExtra("UserIdentifier");
        String regID = null;
        Boolean unregister = intent.getBooleanExtra("Unregister", false);

        if ((userIdentifier == null || userIdentifier.equals("")) && !unregister)
            return;
        try {
            if (unregister){
                getHub().unregister();
                sharedPreferences.edit().remove("registrationID").apply();
                sharedPreferences.edit().remove("FCMtoken").apply();
                return;
            }

            FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
                @Override
                public void onSuccess(InstanceIdResult instanceIdResult) {
                    FCM_token = instanceIdResult.getToken();
                    Log.d("FCMtoken", FCM_token);
                }
            });
            TimeUnit.SECONDS.sleep(1);

            // Storing the registration ID that indicates whether the generated token has been
            // sent to your server. If it is not stored, send the token to your server,
            // otherwise your server should have already received the token.
            if (sharedPreferences.getString("registrationID", null) == null){

                Log.d(TAG, "Attempting a new registration with NH using FCM token : " + FCM_token);
                regID = getHub().register(FCM_token, userIdentifier).getRegistrationId();

                // If you want to use tags...
                // Refer to : https://azure.microsoft.com/documentation/articles/notification-hubs-routing-tag-expressions/
                // regID = hub.register(token, "tag1,tag2").getRegistrationId();
                Log.d(TAG, "New NH Registration Successfully - RegId : " + regID);

                sharedPreferences.edit().putString("registrationID", regID ).apply();
                sharedPreferences.edit().putString("FCMtoken", FCM_token ).apply();
            }

            // Check if the token may have been compromised and needs refreshing.
            else if (!sharedPreferences.getString("FCMtoken", "").equals(FCM_token)) {

                Log.d(TAG, "NH Registration refreshing with token : " + FCM_token);
                regID = getHub().register(FCM_token, userIdentifier).getRegistrationId();

                // If you want to use tags...
                // Refer to : https://azure.microsoft.com/documentation/articles/notification-hubs-routing-tag-expressions/
                // regID = hub.register(token, "tag1,tag2").getRegistrationId();
                Log.d(TAG, "New NH Registration Successfully - RegId : " + regID);

                sharedPreferences.edit().putString("registrationID", regID ).apply();
                sharedPreferences.edit().putString("FCMtoken", FCM_token).apply();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to complete registration", e);
            // If an exception happens while fetching the new token or updating our registration data
            // on a third-party server, this ensures that we'll attempt the update at a later time.
        }
    }

    private NotificationHub getHub() {
        return new NotificationHub(NotificationSettings.HubName,
                NotificationSettings.HubListenConnectionString, this);
    }
}