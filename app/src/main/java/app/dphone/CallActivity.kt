package app.dphone

import android.Manifest
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.app.AlertDialog
import android.app.KeyguardManager
import android.view.View
import app.dphone.webrtc.ServerlessRTCClient
import app.dphone.webrtc.ServerlessRTCClient.State.*
import kotlinx.android.synthetic.main.activity_call.*
import android.media.AudioManager
import android.content.Intent
import android.hardware.Sensor
import android.text.Html
import com.android.volley.Response
import org.json.JSONObject
import android.hardware.Sensor.TYPE_PROXIMITY
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.view.View.VISIBLE
import android.view.WindowManager
import android.view.animation.ScaleAnimation
import android.widget.Chronometer
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONArray
import java.nio.Buffer
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import kotlin.concurrent.schedule


open class CallActivity : BaseActivity(), ServerlessRTCClient.IStateChangeListener {
    private var mediaPlayer: MediaPlayer? = null
    protected var contact: ContactModel? = null
    private var audioManager: AudioManager? = null
    private var callHistory: JSONArray? = null
    protected var callDetails: CallDetailsModel = CallDetailsModel(JSONObject())

    public override fun onStart() {
        super.onStart()
        MyInstanceMessagingService.isOnCall = true
        MyInstanceMessagingService.callUsername = contact?.username
    }

    public override fun onStop() {
        super.onStop()
        MyInstanceMessagingService.isOnCall = false
        MyInstanceMessagingService.callUsername = null
    }

    override fun getContentLayoutId(): Int {
        return R.layout.activity_call
    }

    override fun onCreateContent(content: View) {
        hideToolbar()
        validateLoggedUser()
        if(intent.hasExtra("contact")) {
            onCreateContent()
        } else {
            finishAndRemoveTask()
        }
    }

    private fun setProximitySensor() {
        val mySensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val myProximitySensor = mySensorManager.getDefaultSensor(TYPE_PROXIMITY)
        if (myProximitySensor != null) {
            mySensorManager.registerListener(
                    object : SensorEventListener {
                        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                        }

                        override fun onSensorChanged(event: SensorEvent) {
                            if (event.sensor.type == TYPE_PROXIMITY) {
                                val params = window.attributes
                                if (event.values[0] < 5f && event.values[0] != myProximitySensor.maximumRange) {
                                    params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                                    params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF
                                    window.attributes = params
                                } else {
                                    params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                                    window.attributes = params
                                    window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                                }
                            }
                        }
                    }, myProximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if(::client.isInitialized){
            client.destroy()
        }
        stopCallingTone()
        playBusyTone()

        if(audioManager != null) {
            audioManager!!.isSpeakerphoneOn = false
            audioManager!!.isMicrophoneMute = false
        }
        if(callDetails.status == CallDetailsModel.STATUS_ANSWERED) {
            callDetails.endDate = Date()
        }
        saveCallDetails()
    }

    private fun saveCallDetails(){
        if (contact != null && callDetails.user != null) {
            if (callHistory == null) {
                callHistory = JSONArray()
            }

            val arrayOfJson = Util.ConvertJSONArrayToArrayOfJSONObject(callHistory)
            arrayOfJson.add(0, callDetails.getJson())
            saveCallHistory(arrayOfJson)
        }
    }

    override fun onBackPressed() {
    }

    protected open fun onCreateContent(){
        setProximitySensor()
        setWakeUpProperties()
        checkPermissionsAndInit()
        audioManager = getSystemService(AUDIO_SERVICE) as (AudioManager)

        getCallHistory {history ->
            callHistory = history
        }

        if (contact == null) {
            contact = ContactModel(JSONObject(intent.getStringExtra("contact")))
        }
        val contractFragment = ContactFragment.newInstance(contact!!.username!!, contact!!.name, contact!!.avatarUrl, false)
        val manager = supportFragmentManager
        val transaction = manager.beginTransaction()
        transaction.replace(R.id.contactCallInfoWrapper, contractFragment)
        transaction.commit()

        setAnimationOnButton(contactCallCancelButton)

        contactCallCancelButton.visibility = VISIBLE
        contactCallCancelButton.setSafeOnClickListener {
            removeAnimationOnButton(contactCallCancelButton)
            hangup()
        }

        callDetails.user = contact
        callDetails.type = getCallType()
        callDetails.status = CallDetailsModel.STATUS_NOT_ANSWERED
        callDetails.callDate = Date()
    }

    open fun getCallType(): Int{
        return CallDetailsModel.OUTGOING_CALL_TYPE
    }

    protected fun setAnimationOnButton(btn: FloatingActionButton) {
        val scaleAnimation = ScaleAnimation(.95f, 1.05f, .95f, 1.05f)
        scaleAnimation.duration = 800
        scaleAnimation.repeatCount = ScaleAnimation.INFINITE
        scaleAnimation.repeatMode = ScaleAnimation.REVERSE
        btn.animation = scaleAnimation
    }

    protected fun removeAnimationOnButton(btn: FloatingActionButton) {
        btn.animation = null
    }

    override fun onNewIntent(intent: Intent?) {
        if (contact == null) {
            finishAndRemoveTask()
        } else if (intent != null) {
            if (intent.action == HANGUP_ACTION) {
                hangup()
            } else {
                val answer = intent.getStringExtra("Answer")
                if (answer == "refused") {
                    endCall()
                } else if(answer!=null){
                    validateAnswerOrigin(answer, intent.getStringExtra("Signature")) { verified ->
                        if (verified) {
                            setProcessingCallAnswer()
                            val decryptedAnswer = decryptStringToByteArray(answer)
                            val ungzippedAnswer = Compress().ungzip(decryptedAnswer)
                            MyInstanceMessagingService.showCallNotification(this, NotificationBuilder().TYPE_CONNECTING, contact!!.username!!)
                            client.processAnswer(ungzippedAnswer)
                        }
                    }
                }
            }
        }
    }

    private fun validateAnswerOrigin(message:String, signature:String, callback: (Boolean) -> Unit){
        getPublicKey(contact!!.username!!){publicKey ->
            contact!!.publicKey = publicKey
            callback.invoke(Keys.VerifySignature(message, publicKey, signature))
        }
    }

    private fun endCall(){
        MyInstanceMessagingService.dismissCallNotification(this)
        finishAndRemoveTask()
    }

    private fun checkPermissionsAndInit() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.RECORD_AUDIO)) {
                val builder = AlertDialog.Builder(this)
                builder.setMessage("Permission to access the microphone is required for this app to record audio.")
                        .setTitle("Permission required")

                builder.setPositiveButton("OK") { _, _ ->
                    makeRequest()
                }

                builder.create().show()
            } else {
                makeRequest()
            }
        } else {
            initCapture()
        }
    }

    private fun makeRequest() {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    checkPermissionsAndInit()
                } else {
                    initCapture()
                }
            }
        }
    }

    protected lateinit var client: ServerlessRTCClient
    private fun initCapture(){
        client = ServerlessRTCClient(applicationContext, this)
        try {
            client.init()
        } catch (e:Exception) {
            toastNotify(e.message!!)
            e.printStackTrace()
        }
    }

    override fun onStateChanged(state: ServerlessRTCClient.State) {
        //it could be in different thread
        runOnUiThread {
            when (state) {
                INITIALIZING -> {
                    setEstablishinConnectionText()
                    client.makeOffer()
                }
                OFFER_CREATED->{
                    setEncryptingCallText()
                    sendOffer()
                }
                CONNECTION_ESTABLISHED->{
                    setOnCallText()
                    onConnectionEstablished()
                }
                ERROR-> {
                    setErrorText()
                    endCall()
                }
                CONNECTION_ENDED-> {
                    setFinishingText()
                    endCall()
                }
                else ->{

                }
            }
        }
    }

    protected fun setEstablishinConnectionText() {
        setScreenText("<p>Establishing <font color='#fa1d49'><b>Secure</b></font> connection...</p>")
    }

    protected fun setEncryptingCallText() {
        setScreenText("<p>Encrypting the <font color='#fa1d49'><b>Secure</b></font> call...</p>")
    }

    protected fun setCallingText() {
        setScreenText("<p><font color='#fa1d49'><b>Secure</b></font> calling...</p>")
    }

    protected fun setProcessingCallAnswer() {
        setScreenText("<p>Processing the call answer...</p>")
    }

    protected fun setOnCallText() {
        setScreenText("<p>On a <font color='#fa1d49'><b>Secure</b></font> call...</p>")
    }

    protected fun setFinishingText() {
        setScreenText("<p>Finishing...</p>")
    }

    protected fun setErrorText() {
        setScreenText("<p>Error :(</p>")
    }

    protected fun setScreenText(htmlText: String) {
        runOnUiThread {
            contactCallText.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT)
            contactCallText.visibility = View.VISIBLE
        }
    }

    private fun sendOffer(){
        MyInstanceMessagingService.showCallNotification(this, NotificationBuilder().TYPE_OUTGOING_RINGING, contact!!.username!!)
        val descriptionJSON = client.getSessionDescriptionJSON()
        val compressedOffer = Compress().gzip(descriptionJSON.toString())
        val encryptedOffer = encryptByteArray(compressedOffer, contact!!.publicKey!!)

        val pushJsonData = JSONObject()
        pushJsonData.put("callerId", loggedUsername())
        pushJsonData.put("type", "callGroup")
        pushJsonData.put("offer", encryptedOffer)
        val pushJson = JSONObject()
        pushJson.put("data", pushJsonData)
        if(!isDestroyed && !isFinishing) {
            sendPush(contact!!.username!!, pushJson, Response.Listener {
                setCallingText()
                startCallingTone()
                setCallTimeout()
            }, Response.ErrorListener {
                client.state = ERROR
            })
        }
    }

    protected fun setCallTimeout() {
        Timer("TimeoutCall", false).schedule(60 * 1000) {
            runOnUiThread {
                if (!isDestroyed && !isFinishing && client.state != CONNECTION_ESTABLISHED) {
                    hangup()
                }
            }
        }
    }

    private fun sendHangUp(){
        val pushJsonData = JSONObject()
        pushJsonData.put("callerId", loggedUsername())
        pushJsonData.put("type", "hangupGroup")
        val pushJson = JSONObject()
        pushJson.put("data", pushJsonData)
        sendPush(contact!!.username!!, pushJson, Response.Listener {
        }, Response.ErrorListener {
        } )
    }

    private fun startCallingTone(){
        playTone(R.raw.outgoing)
    }

    private fun playBusyTone(){
        if(callDetails.type == CallDetailsModel.OUTGOING_CALL_TYPE ||
            callDetails.status == CallDetailsModel.STATUS_ANSWERED) {
            playTone(R.raw.busy)
        }
    }

    private fun playTone(tone:Int){
        if( mediaPlayer != null ) {
            mediaPlayer!!.release()
        }

        mediaPlayer = MediaPlayer()
        mediaPlayer!!.setAudioAttributes(
                AudioAttributes
                        .Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING)
                        .setLegacyStreamType(AudioManager.STREAM_VOICE_CALL)
                        .build())
        mediaPlayer!!.isLooping = false

        var packageName = packageName
        var dataUri     = Uri.parse("android.resource://" + packageName + "/" + tone)

        try {
            mediaPlayer!!.setDataSource(this, dataUri)
            mediaPlayer!!.prepare()
            mediaPlayer!!.start()
        } catch (e:Exception) {
            Log.w("CallActivity", e)
        }
    }

    private fun stopCallingTone() {
        if (mediaPlayer == null) return
        mediaPlayer!!.stop()
        mediaPlayer!!.release()
        mediaPlayer = null
    }

    open fun onConnectionEstablished() {
        stopCallingTone()
        callDetails.status = CallDetailsModel.STATUS_ANSWERED
        callDetails.startDate = Date()

        val chronometer = findViewById<Chronometer>(R.id.chronometer)
        chronometer.base = SystemClock.elapsedRealtime()
        chronometer.visibility = VISIBLE
        chronometer.start()

        MyInstanceMessagingService.showCallNotification(this, getConnectionEstablishedNotificationType(), contact!!.username!!)
    }

    open fun getConnectionEstablishedNotificationType(): Int{
        return NotificationBuilder().TYPE_ESTABLISHED
    }

    open fun hangup(){
        endCall()
        sendHangUp()
    }

    private fun mute(){
        audioManager!!.isMicrophoneMute = true
    }

    private fun unmute(){
        audioManager!!.isMicrophoneMute = false
    }

    private fun enableSpeaker(){
        audioManager!!.isSpeakerphoneOn = true
    }

    private fun disableSpeaker(){
        audioManager!!.isSpeakerphoneOn = false
    }

    private fun setWakeUpProperties() {
        val window = window
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val pm = applicationContext.getSystemService(POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, "dphone:WAKE_LOCK")
        wakeLock.acquire()

        val keyguardManager = applicationContext.getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        val keyguardLock = keyguardManager.newKeyguardLock("dphone:KEYGUARD_LOCK")
        keyguardLock.disableKeyguard()
    }

    companion object {
        val ANSWER_ACTION = CallActivity::class.java.canonicalName!! + ".ANSWER_ACTION"
        val DENY_ACTION = CallActivity::class.java.canonicalName!! + ".DENY_ACTION"
        val HANGUP_ACTION = CallActivity::class.java.canonicalName!! + ".HANGUP_ACTION"
        private const val REQUEST_CODE = 101
    }
}