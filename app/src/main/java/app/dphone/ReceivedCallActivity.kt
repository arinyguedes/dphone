package app.dphone

import app.dphone.webrtc.ServerlessRTCClient
import app.dphone.webrtc.ServerlessRTCClient.State.*
import android.content.Intent
import android.media.AudioManager
import android.view.View
import android.media.RingtoneManager
import android.media.Ringtone
import android.os.*
import android.view.View.VISIBLE
import com.android.volley.Response
import kotlinx.android.synthetic.main.activity_received_call.*
import org.json.JSONObject
import androidx.constraintlayout.widget.ConstraintLayout


class ReceivedCallActivity : CallActivity() {
    private var offer : String? = null
    private var vibrator: Vibrator? = null
    private var ringToneAlarm: Ringtone? = null

    override fun getContentLayoutId(): Int {
        return R.layout.activity_received_call
    }

    override fun onCreateContent(content: View) {
        val encryptedOffer = intent.getStringExtra("Offer")
        if (encryptedOffer == null) {
            finishAndRemoveTask()
        } else {
            validateLoggedUser()
            if(MyInstanceMessagingService.isOnCall){
                sendRefused(intent.getStringExtra("CallerId"))
                finishAndRemoveTask()
            }
            else {
                setCheckingCallerInfoText()
                setContactModel()
                validateCallOrigin{verified ->
                    if(verified) {
                        setReceivingCallText()
                        super.onCreateContent()
                        val decryptedOffer = decryptStringToByteArray(encryptedOffer)
                        offer = Compress().ungzip(decryptedOffer)

                        setAnimationOnButton(contactReceiveCallButton)

                        contactReceiveCallButton.show()
                        contactReceiveCallButton.setSafeOnClickListener {
                            removeAnimationOnButton(contactReceiveCallButton)
                            setEstablishinConnectionText()
                            onAnswerClick()
                        }

                        startRingtone()
                        vibrate()
                        setCallTimeout()
                        MyInstanceMessagingService.showCallNotification(this, NotificationBuilder().TYPE_INCOMING_RINGING, contact!!.username)
                    }
                    else{
                        finishAndRemoveTask()
                    }
                }
            }
        }
    }

    private fun validateCallOrigin(callback: (Boolean) -> Unit){
        getPublicKey(contact!!.username!!){publicKey ->
            contact!!.publicKey = publicKey
            callback.invoke(true)
        }
    }

    override fun getCallType(): Int{
        return CallDetailsModel.INCOMING_CALL_TYPE
    }

    private fun setCheckingCallerInfoText() {
        setScreenText("<p>Checking caller info...</p>")
    }

    private fun setReceivingCallText() {
        setScreenText("<p><font color='#fa1d49'><b>Secure</b></font> call...</p>")
    }

    private fun startRingtone() {
        val ringTone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ringToneAlarm = RingtoneManager.getRingtone(applicationContext, ringTone)
        ringToneAlarm!!.play()
    }

    private fun setContactModel() {
        contact = ContactModel(JSONObject())
        contact!!.username = intent.getStringExtra("CallerId")
        fillProfile()
    }

    private fun fillProfile(){
        blockstack().getProfile(contact!!.username!!) { profile ->
            if (profile != null) {
                contact!!.name = profile.name
                contact!!.avatarUrl = profile.avatarImage
                callDetails.user = contact
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAudioAndVibration()
        MyInstanceMessagingService.dismissCallNotification(this)
    }

    override fun onNewIntent(intent: Intent?) {
        if(contact?.username == null){
            finishAndRemoveTask()
        }
        else if (intent?.action == ANSWER_ACTION) {
            onAnswerClick()
        } else if (intent?.action == DENY_ACTION || intent?.action == HANGUP_ACTION) {
            hangup()
        } else {
            if (intent != null) {
                val intentCallerId = intent.getStringExtra("CallerId")
                if (intentCallerId != null || intentCallerId.equals(contact!!.username!!)) {
                    val answer = intent.getStringExtra("Answer")
                    if (answer == "hangup") {
                        finishAndRemoveTask()
                    }
                }
            }
        }
    }

    private fun vibrate() {
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        if(am.ringerMode != AudioManager.RINGER_MODE_SILENT) {
            if (vibrator == null)
                vibrator = (getSystemService(VIBRATOR_SERVICE) as Vibrator)
            if (vibrator!!.hasVibrator())
                vibrator!!.vibrate(VibrationEffect.createWaveform(longArrayOf(1500, 1500), intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE), 0))
        }
    }

    private fun stopAudioAndVibration(){
        if(vibrator!= null)
            vibrator!!.cancel()
        if(ringToneAlarm!=null)
            ringToneAlarm!!.stop()
    }

    override fun onStateChanged(state: ServerlessRTCClient.State) {
        runOnUiThread {
            when (state) {
                ANSWER_CREATED -> {
                    sendAnswer()
                }
                CONNECTION_ESTABLISHED->{
                    setOnCallText()
                    onConnectionEstablished()
                }
                ERROR-> {
                    setErrorText()
                    finishAndRemoveTask()
                }
                CONNECTION_ENDED-> {
                    setFinishingText()
                    finishAndRemoveTask()
                }
                else -> {
                }
            }
        }
    }

    private fun sendAnswer() {
        val descriptionJSON = client.getSessionDescriptionJSON()
        val compressedAnswer = Compress().gzip(descriptionJSON.toString())
        val encryptedAnswer = encryptByteArray(compressedAnswer, contact!!.publicKey!!)
        var signature = Keys.Sign(encryptedAnswer, loggedPrivateKey())
        val pushJsonData = JSONObject()
        pushJsonData.put("type", "answerGroup")
        pushJsonData.put("answer", encryptedAnswer)
        pushJsonData.put("signature", signature)
        val pushJson = JSONObject()
        pushJson.put("data", pushJsonData)
        sendPush(contact!!.username!!, pushJson, Response.Listener {

        }, Response.ErrorListener {
            client.state = ERROR
        } )
    }

    private fun sendRefused(){
        sendRefused(contact!!.username!!)
    }

    private fun sendRefused(username:String){
        val pushJsonData = JSONObject()
        pushJsonData.put("type", "answerGroup")
        pushJsonData.put("answer", "refused")
        val pushJson = JSONObject()
        pushJson.put("data", pushJsonData)
        sendPush(username, pushJson, Response.Listener {
            finishAndRemoveTask()
        }, Response.ErrorListener {
            client.state = ERROR
        } )
    }

    fun onAnswerClick(){
        MyInstanceMessagingService.showCallNotification(this,
               NotificationBuilder().TYPE_CONNECTING, contact!!.username!!)

        runOnUiThread {
            contactReceiveCallButton.hide()

            val params = contactCallCancelButton.layoutParams as ConstraintLayout.LayoutParams
            params.startToStart = R.id.mainConstrainLayout
            params.endToEnd = R.id.mainConstrainLayout
            params.bottomToBottom = R.id.mainConstrainLayout
            params.marginStart = 0
            contactCallCancelButton.layoutParams = params
        }

        stopAudioAndVibration()

        client.processOffer(offer!!)
    }

    override fun hangup(){
        stopAudioAndVibration()
        sendRefused()
    }

    override fun getConnectionEstablishedNotificationType() : Int{
        return NotificationBuilder().TYPE_INCOMING_ESTABLISHED
    }

}
