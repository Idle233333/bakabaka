package com.touhou.aichat

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

object CallManager {
    enum class Phase { IDLE, OUTGOING, ACTIVE, ENDED }

    data class State(
        val phase: Phase = Phase.IDLE,
        val callId: Long = 0,
        val peerUserId: Long = 0,
        val peerName: String = "",
        val peerAvatar: String = "",
        val durationSeconds: Int = 0,
        val error: String? = null
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var context: Context? = null
    private var token: String = ""

    private var factory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null

    private var pollJob: Job? = null
    private var timerJob: Job? = null

    fun init(ctx: Context, tok: String) {
        context = ctx.applicationContext
        token = tok
    }

    fun setToken(tok: String) {
        token = tok
    }

    fun isInCall(): Boolean {
        val p = _state.value.phase
        return p == Phase.OUTGOING || p == Phase.ACTIVE
    }

    // ===== 发起呼叫 =====
    fun startCall(toUserId: Long, peerName: String, peerAvatar: String) {
        if (isInCall()) return
        _state.value = State(Phase.OUTGOING, peerUserId = toUserId, peerName = peerName, peerAvatar = peerAvatar)
        scope.launch {
            when (val r = Api.startCall(token, toUserId)) {
                is ApiResult.Success -> {
                    val callId = r.data
                    _state.value = _state.value.copy(callId = callId)
                    setupPeer()
                    startPolling(callId)
                }
                is ApiResult.Error -> {
                    _state.value = _state.value.copy(phase = Phase.ENDED, error = r.message)
                }
            }
        }
    }

    // ===== 接听 =====
    fun answerCall(callId: Long, callerId: Long, peerName: String, peerAvatar: String) {
        if (isInCall()) return
        _state.value = State(Phase.ACTIVE, callId = callId, peerUserId = callerId, peerName = peerName, peerAvatar = peerAvatar)
        startTimer()
        scope.launch {
            Api.respondCall(token, callId, "answer")
            setupPeer()
            startPolling(callId)
        }
    }

    // ===== 拒绝 =====
    fun rejectCall(callId: Long) {
        scope.launch {
            Api.respondCall(token, callId, "reject")
        }
    }

    // ===== 挂断 =====
    fun hangUp() {
        val st = _state.value
        if (st.phase == Phase.IDLE || st.phase == Phase.ENDED) return
        val callId = st.callId
        val duration = st.durationSeconds
        val peerUserId = st.peerUserId
        val peerName = st.peerName
        val peerAvatar = st.peerAvatar
        scope.launch {
            if (callId > 0) Api.endCall(token, callId)
            cleanup()
            _state.value = State(Phase.ENDED, callId = callId, peerUserId = peerUserId, peerName = peerName, peerAvatar = peerAvatar, durationSeconds = duration)
            if (duration > 0 && peerUserId > 0) {
                Api.sendDm(token, peerUserId, "通话时长 ${formatDuration(duration)}")
            }
        }
    }

    // ===== 通话结束（双方结束后的收尾）=====
    fun dismiss() {
        cleanup()
        _state.value = State(Phase.IDLE)
    }

    // ===== WebRTC 初始化 =====
    private fun setupPeer() {
        val ctx = context ?: return
        try {
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(ctx).createInitializationOptions()
            )
            val f = PeerConnectionFactory.builder()
                .setOptions(PeerConnectionFactory.Options())
                .createPeerConnectionFactory()
            factory = f

            val src = f.createAudioSource(MediaConstraints())
            audioSource = src
            val track = f.createAudioTrack("audio0", src)
            track.setEnabled(true)
            localAudioTrack = track

            val config = PeerConnection.RTCConfiguration(
                listOf(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
            )
            val pc = f.createPeerConnection(config, observer) ?: throw IllegalStateException("创建 PeerConnection 失败")
            peerConnection = pc
            pc.addTrack(track, listOf("stream0"))

            if (_state.value.phase == Phase.OUTGOING) {
                createOffer()
            }
        } catch (e: Throwable) {
            _state.value = _state.value.copy(error = "通话初始化失败: ${e.message}")
        }
    }

    private fun createOffer() {
        val pc = peerConnection ?: return
        pc.createOffer(object : SdpObserver {
            override fun onCreateSuccess(offer: SessionDescription?) {
                val desc = offer ?: return
                pc.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(sdp: SessionDescription?) {}
                    override fun onSetSuccess() {
                        sendSignal("offer", desc.description)
                    }
                    override fun onCreateFailure(s: String?) {}
                    override fun onSetFailure(s: String?) {}
                }, desc)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(s: String?) {}
            override fun onSetFailure(s: String?) {}
        }, MediaConstraints())
    }

    private fun createAnswer() {
        val pc = peerConnection ?: return
        pc.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(answer: SessionDescription?) {
                val desc = answer ?: return
                pc.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(sdp: SessionDescription?) {}
                    override fun onSetSuccess() {
                        sendSignal("answer", desc.description)
                    }
                    override fun onCreateFailure(s: String?) {}
                    override fun onSetFailure(s: String?) {}
                }, desc)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(s: String?) {}
            override fun onSetFailure(s: String?) {}
        }, MediaConstraints())
    }

    private fun sendSignal(type: String, data: String) {
        val callId = _state.value.callId
        if (callId <= 0) return
        scope.launch {
            Api.sendCallSignal(token, callId, type, data)
        }
    }

    private fun handleSignal(signal: CallSignal) {
        val pc = peerConnection ?: return
        when (signal.type) {
            "offer" -> {
                val sdp = SessionDescription(SessionDescription.Type.OFFER, signal.data)
                pc.setRemoteDescription(object : SdpObserver {
                    override fun onCreateSuccess(sdp: SessionDescription?) {}
                    override fun onSetSuccess() { createAnswer() }
                    override fun onCreateFailure(s: String?) {}
                    override fun onSetFailure(s: String?) {}
                }, sdp)
            }
            "answer" -> {
                val sdp = SessionDescription(SessionDescription.Type.ANSWER, signal.data)
                pc.setRemoteDescription(object : SdpObserver {
                    override fun onCreateSuccess(sdp: SessionDescription?) {}
                    override fun onSetSuccess() {}
                    override fun onCreateFailure(s: String?) {}
                    override fun onSetFailure(s: String?) {}
                }, sdp)
            }
            "candidate" -> {
                val parts = signal.data.split("|", limit = 3)
                if (parts.size == 3) {
                    val mline = parts[1].toIntOrNull() ?: 0
                    pc.addIceCandidate(IceCandidate(parts[0], mline, parts[2]))
                }
            }
        }
    }

    private val observer = object : PeerConnection.Observer {
        override fun onIceCandidate(candidate: IceCandidate?) {
            if (candidate != null) {
                val data = "${candidate.sdpMid ?: ""}|${candidate.sdpMLineIndex}|${candidate.sdp}"
                sendSignal("candidate", data)
            }
        }
        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
        override fun onIceConnectionReceivingChange(receiving: Boolean) {}
        override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
        override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
        override fun onAddStream(stream: MediaStream?) {}
        override fun onRemoveStream(stream: MediaStream?) {}
        override fun onDataChannel(channel: DataChannel?) {}
        override fun onRenegotiationNeeded() {}
        override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
    }

    // ===== 轮询信令 + 状态 =====
    private fun startPolling(callId: Long) {
        pollJob?.cancel()
        var after = 0L
        pollJob = scope.launch {
            while (isActive) {
                when (val r = Api.fetchCallSignals(token, callId, after)) {
                    is ApiResult.Success -> {
                        for (s in r.data) {
                            after = maxOf(after, s.id)
                            handleSignal(s)
                        }
                    }
                    is ApiResult.Error -> {}
                }
                when (val r = Api.fetchCallStatus(token, callId)) {
                    is ApiResult.Success -> {
                        val st = r.data
                        if (st == "ended" || st == "rejected" || st == "cancelled") {
                            finishRemote()
                            return@launch
                        }
                        if (st == "active" && _state.value.phase == Phase.OUTGOING) {
                            _state.value = _state.value.copy(phase = Phase.ACTIVE)
                            startTimer()
                        }
                    }
                    is ApiResult.Error -> {}
                }
                delay(1000)
            }
        }
    }

    private fun finishRemote() {
        val st = _state.value
        val duration = st.durationSeconds
        val peerUserId = st.peerUserId
        cleanup()
        _state.value = State(Phase.ENDED, callId = st.callId, peerUserId = peerUserId, peerName = st.peerName, peerAvatar = st.peerAvatar, durationSeconds = duration)
        if (duration > 0 && peerUserId > 0) {
            scope.launch {
                Api.sendDm(token, peerUserId, "通话时长 ${formatDuration(duration)}")
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        var seconds = 0
        timerJob = scope.launch {
            while (isActive) {
                delay(1000)
                seconds++
                _state.value = _state.value.copy(durationSeconds = seconds)
            }
        }
    }

    private fun cleanup() {
        pollJob?.cancel()
        pollJob = null
        timerJob?.cancel()
        timerJob = null
        try { peerConnection?.close() } catch (_: Throwable) {}
        peerConnection = null
        try { localAudioTrack?.setEnabled(false) } catch (_: Throwable) {}
        try { localAudioTrack?.dispose() } catch (_: Throwable) {}
        localAudioTrack = null
        try { audioSource?.dispose() } catch (_: Throwable) {}
        audioSource = null
        try { factory?.dispose() } catch (_: Throwable) {}
        factory = null
    }

    private fun formatDuration(totalSeconds: Int): String {
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) String.format("%02d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
    }
}
