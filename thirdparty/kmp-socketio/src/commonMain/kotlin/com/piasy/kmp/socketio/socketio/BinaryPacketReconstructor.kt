package com.piasy.kmp.socketio.socketio

import com.piasy.kmp.xlog.Logging
import kotlinx.io.bytestring.ByteString
import org.hildan.socketio.PayloadElement
import org.hildan.socketio.SocketIOPacket

class BinaryPacketReconstructor(
    private val packet: SocketIOPacket.BinaryMessage,
    private val emitter: (isAck: Boolean, ackId: Int?, ArrayList<Any>) -> Unit,
) {
    private val buffers = ArrayList<ByteString>()

    fun add(buffer: ByteString) {
        buffers.add(buffer)
        if (buffers.size == packet.nBinaryAttachments) {
            val data = ArrayList<Any>()
            packet.payload.forEach {
                if (it is PayloadElement.AttachmentRef) {
                    if (it.attachmentIndex in 0..<buffers.size) {
                        data.add(buffers[it.attachmentIndex])
                    } else {
                        Logging.error(
                            Socket.TAG,
                            "BinaryPacketReconstructor bad index: ${it.attachmentIndex}, ${buffers.size}"
                        )
                        return
                    }
                } else {
                    data.add((it as PayloadElement.Json).jsonElement)
                }
            }
            emitter(packet is SocketIOPacket.BinaryAck, packet.ackId, data)
        }
    }
}
