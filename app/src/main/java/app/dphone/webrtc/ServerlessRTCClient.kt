package app.dphone.webrtc

import android.content.Context
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.*

/**
 * This class handles all around WebRTC peer connections.
 */
class ServerlessRTCClient(private val context: Context, val listener: IStateChangeListener) {

    lateinit var pc: PeerConnection
    private var pcInitialized = false

    /**
     * List of servers that will be used to establish the direct connection, STUN/TURN should be supported.
     */
    private val iceServers = arrayListOf(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())

    enum class State {
        INITIALIZING,
        CREATING_OFFER,
        CREATING_ANSWER,
        OFFER_CREATED,
        WAITING_FOR_ANSWER,
        PROCESSING_ANSWER,
        ANSWER_CREATED,
        ERROR,
        CONNECTION_ESTABLISHED,
        CONNECTION_ENDED
    }

    lateinit var pcf: PeerConnectionFactory
    val pcConstraints = object : MediaConstraints() {
        init {
            mandatory.add(KeyValuePair("OfferToReceiveAudio", "true"))
        }
    }

    var state: State = State.INITIALIZING
        set(value) {
            field = value
            listener.onStateChanged(value)
        }


    interface IStateChangeListener {
        /**
         * Called when status of client is changed.
         */
        fun onStateChanged(state: State)
    }

    open inner class DefaultObserver : PeerConnection.Observer {
        override fun onDataChannel(p0: DataChannel?) {
        }

        override fun onIceCandidate(p0: IceCandidate?) {
        }

        override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
        }

        override fun onIceConnectionReceivingChange(p0: Boolean) {
        }

        override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
        }

        override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
        }

        override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
        }

        override fun onAddStream(p0: MediaStream?) {
        }

        override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
        }

        override fun onRemoveStream(p0: MediaStream?) {
        }

        override fun onRenegotiationNeeded() {
        }

        fun getStateFromPeerStatus():State?{
            if(pc.iceGatheringState() == PeerConnection.IceGatheringState.COMPLETE){
                var p0 = pc.iceConnectionState()
                if (p0 == PeerConnection.IceConnectionState.CONNECTED){
                    return State.CONNECTION_ESTABLISHED
                }
                else if (p0 == PeerConnection.IceConnectionState.DISCONNECTED ||
                        p0 == PeerConnection.IceConnectionState.CLOSED ||
                        p0 == PeerConnection.IceConnectionState.FAILED
                ) {
                    return State.CONNECTION_ENDED
                }
            }
            return null
        }
    }

    open inner class DefaultSdpObserver : SdpObserver {

        override fun onCreateSuccess(p0: SessionDescription?) {
        }

        override fun onCreateFailure(p0: String?) {
        }

        override fun onSetFailure(p0: String?) {
        }

        override fun onSetSuccess() {
        }
    }

    private val JSON_TYPE = "type"
    private val JSON_SDP = "sdp"

    /**
     * Converts session description object to JSON object that can be used in other applications.
     * This is what is passed between parties to maintain connection. We need to pass the session description to the other side.
     * In normal use case we should use some kind of signalling server, but for this demo you can use some other method to pass it there (like e-mail).
     */
    private fun sessionDescriptionToJSON(sessDesc: SessionDescription): JSONObject {
        val json = JSONObject()
        json.put(JSON_TYPE, sessDesc.type.canonicalForm())
        json.put(JSON_SDP, sessDesc.description)
        return json
    }

    fun getSessionDescriptionJSON() : JSONObject{
        return sessionDescriptionToJSON(pc.localDescription)
    }

    /**
     * Process offer that was entered by user (this is called getOffer() in JavaScript example)
     */
    fun processOffer(sdpJSON: String) {
        try {
            val json = JSONObject(sdpJSON)
            val type = json.getString(JSON_TYPE)
            val sdp = json.getString(JSON_SDP)
            if (type != null && sdp != null && type == "offer") {
                state = State.CREATING_ANSWER
                val offer = SessionDescription(SessionDescription.Type.OFFER, sdp)
                pcInitialized = true
                pc = pcf.createPeerConnection(iceServers, object : DefaultObserver() {
                    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
                        super.onIceGatheringChange(p0)
                        if(getStateFromPeerStatus()!= null)
                            state = getStateFromPeerStatus()!!
                    }

                    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?){
                        super.onIceConnectionChange(p0)
                        if(getStateFromPeerStatus()!= null)
                            state = getStateFromPeerStatus()!!
                    }


                })!!

                addStreamToLocalPeer()

                //we have remote offer, let's create answer for that
                pc.setRemoteDescription(object : DefaultSdpObserver() {
                    override fun onSetSuccess() {
                        super.onSetSuccess()
                        pc.createAnswer(object : DefaultSdpObserver() {
                            override fun onCreateSuccess(p0: SessionDescription?) {
                                //answer is ready, set it
                                pc.setLocalDescription(DefaultSdpObserver(), p0)
                                state = State.ANSWER_CREATED
                            }
                        }, pcConstraints)
                    }
                }, offer)
            }
        } catch (e: JSONException) {
            state = State.ERROR
        }
    }



    /**
     * Process answer that was entered by user (this is called getAnswer() in JavaScript example)
     */
    fun processAnswer(sdpJSON: String) {
        try {
            val json = JSONObject(sdpJSON)
            val type = json.getString(JSON_TYPE)
            val sdp = json.getString(JSON_SDP)
            if (type != null && sdp != null && type == "answer") {
                state = State.PROCESSING_ANSWER
                val answer = SessionDescription(SessionDescription.Type.ANSWER, sdp)
                pc.setRemoteDescription(DefaultSdpObserver(), answer)
                state = State.CONNECTION_ESTABLISHED
            }
        } catch (e: JSONException) {
            state = State.ERROR
        }
    }

    /**
     * App creates the offer.
     */
    fun makeOffer() {
        state = State.CREATING_OFFER
        pcInitialized = true
        pc = pcf.createPeerConnection(iceServers, object : DefaultObserver() {
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
                super.onIceGatheringChange(p0)
                if (p0 == PeerConnection.IceGatheringState.COMPLETE) {
                    state = State.WAITING_FOR_ANSWER
                }
            }

            override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?){
                super.onIceConnectionChange(p0)
                if (p0 == PeerConnection.IceConnectionState.CONNECTED){
                    state = State.CONNECTION_ESTABLISHED
                }
                else if (p0 == PeerConnection.IceConnectionState.DISCONNECTED ||
                        p0 == PeerConnection.IceConnectionState.CLOSED
                ) {
                    state = State.CONNECTION_ENDED
                }
            }
        })!!

        addStreamToLocalPeer()

        pc.createOffer(object : DefaultSdpObserver() {
            override fun onCreateSuccess(p0: SessionDescription?) {
                if (p0 != null) {
                    pc.setLocalDescription(object : DefaultSdpObserver() {
                        override fun onCreateSuccess(p0: SessionDescription?) {
                        }
                    }, p0)
                    state = State.OFFER_CREATED
                }
            }
        }, pcConstraints)
    }

    private fun addStreamToLocalPeer() {
        val audioConstraints = MediaConstraints()
        val audioSource = pcf.createAudioSource(audioConstraints)
        val localAudioTrack = pcf.createAudioTrack("101", audioSource)

        val stream = pcf.createLocalMediaStream("102")
        stream.addTrack(localAudioTrack)
        pc.addStream(stream)
    }

    /**
     * Call this before using anything else from PeerConnection.
     */
    fun init() {
        val initializeOptions=PeerConnectionFactory.InitializationOptions.builder(context).setEnableInternalTracer(false).createInitializationOptions()
        PeerConnectionFactory.initialize(initializeOptions)
        val options=PeerConnectionFactory.Options()
        pcf = PeerConnectionFactory.builder().setOptions(options).createPeerConnectionFactory()
        state = State.INITIALIZING
    }


    /**
     * Clean up some resources.
     */
    fun destroy() {
        if (pcInitialized) {
            pc.close()
        }
    }
}