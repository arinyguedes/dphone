package app.dphone;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import org.jetbrains.annotations.NotNull;
import java.util.Arrays;
import java.util.Map;


public class MyInstanceMessagingService extends FirebaseMessagingService {
    public static final String INCOMING_CHANNEL_ID = "incoming_channel";
    public static final String INCOMING_CHANNEL_NAME = "Incoming Notifications";
    public static final String INCOMING_CHANNEL_DESCRIPTION = "Incoming Notifications";

    public static final String OUTGOING_CHANNEL_ID = "outgoing_channel";
    public static final String OUTGOING_CHANNEL_NAME = "Outgoing Call Notifications";
    public static final String OUTGOING_CHANNEL_DESCRIPTION = "Outgoing Call Notifications";

    private static final String GENERAL_GROUP = "generalGroup";
    private static final String CALL_GROUP = "callGroup";
    private static final String ANSWER_GROUP = "answerGroup";
    private static final String HANGUP_GROUP = "hangupGroup";

    public static boolean isOnCall = false;
    public static String callUsername = null;

    @Override
    public void
    onNewToken(String s) {
        super.onNewToken(s);
    }
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Map<String, String> remoteMessageData = remoteMessage.getData();

        String nhMessage = remoteMessageData.get("message");
        String nhTitle = remoteMessageData.get("title");
        String nhType = remoteMessageData.get("type");
        try {
            if(nhType != null && nhType.equals(CALL_GROUP)) {
                String nhOffer = remoteMessageData.get("offer");
                String callerId = remoteMessageData.get("callerId");
                sendCallNotification(nhOffer, callerId);
            }
            else if(nhType != null && nhType.equals(ANSWER_GROUP)) {
                String nhAnswer = remoteMessageData.get("answer");
                String signature = remoteMessageData.get("signature");
                sendAnswerNotification(nhAnswer, signature);
            }
            else if(nhType != null && nhType.equals(HANGUP_GROUP)) {
                String callerId = remoteMessageData.get("callerId");
                sendHangupNotification(callerId);
            }
            else
                sendNotification(nhTitle, nhMessage);
        }
        catch (Exception ex) {
            Log.i("NH_EXCEPTION", ex.getMessage());
        }
    }

    private void sendCallNotification(String offer, String callerId){
        Intent intent = getReceivedCallActivityIntent(callerId);
        intent.putExtra("Offer", offer);
        getApplicationContext().startActivity(intent);
    }

    private void sendHangupNotification(String callerId){
        Intent intent = getReceivedCallActivityIntent(callerId);
        intent.putExtra("Answer", "hangup");
        getApplicationContext().startActivity(intent);
    }

    @NotNull
    private Intent getReceivedCallActivityIntent(String callerId) {
        Intent intent = new Intent(this, ReceivedCallActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("CallerId", callerId);
        return intent;
    }

    private void sendAnswerNotification(String answer, String signature){
        Intent intent = new Intent(this, CallActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("Answer", answer);
        intent.putExtra("Signature", signature);
        getApplicationContext().startActivity(intent);
    }

    private void sendNotification(String title, String msg) {
        sendNotification(title, msg, GENERAL_GROUP);
    }
    private void sendNotification(String title, String msg, String group) {
        Context ctx = getApplicationContext();
        Intent intent = new Intent(ctx, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        NotificationManager notificationManager = (NotificationManager)
                ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0,
                intent, PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(
                ctx,
                INCOMING_CHANNEL_ID)
                .setContentText(msg)
                .setContentTitle(title)
                .setGroup(group)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.ic_stat_onesignal_default)
                .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL);

        notificationBuilder.setContentIntent(contentIntent);
        notificationManager.notify(msg.hashCode(), notificationBuilder.build());
    }

    public static void showCallNotification(Context context, int notificationType,String callPeer){
        NotificationBuilder builder = new NotificationBuilder();
        Notification notification = builder.getCallInProgressNotification(context, notificationType, callPeer);
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.notify(5124,notification);
    }

    public static void dismissCallNotification(Context context){
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.cancel(5124);
    }

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            NotificationChannel incomingChannel = new NotificationChannel(
                    INCOMING_CHANNEL_ID,
                    INCOMING_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            incomingChannel.setDescription(INCOMING_CHANNEL_DESCRIPTION);
            incomingChannel.setShowBadge(false);

            NotificationChannel outgoingChannel = new NotificationChannel(
                    OUTGOING_CHANNEL_ID,
                    OUTGOING_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW);
            outgoingChannel.setDescription(OUTGOING_CHANNEL_DESCRIPTION);
            outgoingChannel.setShowBadge(false);

            notificationManager.createNotificationChannels(Arrays.asList(incomingChannel,outgoingChannel));
        }
    }
}