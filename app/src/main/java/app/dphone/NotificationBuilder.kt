package app.dphone

import android.app.Notification
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat

class NotificationBuilder {

    val TYPE_INCOMING_RINGING = 1
    val TYPE_OUTGOING_RINGING = 2
    val TYPE_ESTABLISHED = 3
    val TYPE_INCOMING_ESTABLISHED = 4
    val TYPE_CONNECTING = 5

    fun getCallInProgressNotification(context: Context, type: Int, callerId: String): Notification {
        val contentIntent = Intent(context, CallActivity::class.java)
        contentIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP

        val pendingIntent = PendingIntent.getActivity(context, 0, contentIntent, 0)

        val builder = NotificationCompat.Builder(context, getNotificationChannel(type))
                .setSmallIcon(R.drawable.ic_stat_onesignal_default)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setContentTitle(callerId)

        if (type == TYPE_CONNECTING) {
            builder.setContentText(context.getString(R.string.connecting))
            builder.priority = NotificationCompat.PRIORITY_MIN
        } else if (type == TYPE_INCOMING_RINGING) {
            builder.setContentText(context.getString(R.string.incoming_call))
            builder.addAction(getActivityNotificationAction(context, CallActivity.DENY_ACTION, R.string.deny_call, ReceivedCallActivity::class.java))
            builder.addAction(getActivityNotificationAction(context, CallActivity.ANSWER_ACTION, R.string.answer_call, ReceivedCallActivity::class.java))

            if (callActivityRestricted()) {
                builder.setFullScreenIntent(pendingIntent, true)
                builder.priority = NotificationCompat.PRIORITY_HIGH
                builder.setCategory(NotificationCompat.CATEGORY_CALL)
            }
        } else if (type == TYPE_OUTGOING_RINGING) {
            builder.setContentText(context.getString(R.string.establishing_call))
            builder.addAction(getActivityNotificationAction(context, CallActivity.HANGUP_ACTION, R.string.cancel_call))
        } else {
            builder.setContentText(context.getString(R.string.call_in_progress))
            builder.addAction(getActivityNotificationAction(context, CallActivity.HANGUP_ACTION, R.string.end_call, if (type == TYPE_INCOMING_ESTABLISHED) ReceivedCallActivity::class.java else CallActivity::class.java))
        }

        return builder.build()
    }

    private fun getNotificationChannel(type: Int): String {
        if(type == TYPE_INCOMING_RINGING)
            return MyInstanceMessagingService.INCOMING_CHANNEL_ID

        return MyInstanceMessagingService.OUTGOING_CHANNEL_ID
    }

    private fun getActivityNotificationAction(context: Context, action: String,
              @StringRes titleResId: Int, cls : Class<*> = CallActivity::class.java): NotificationCompat.Action {
        val intent = Intent(context, cls)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        intent.action = action

        val pendingIntent = PendingIntent.getActivity(context, 5124, intent, FLAG_UPDATE_CURRENT)

        return NotificationCompat.Action(0, context.getString(titleResId), pendingIntent)
    }

    private fun callActivityRestricted(): Boolean {
        return Build.VERSION.SDK_INT >= 29
    }
}