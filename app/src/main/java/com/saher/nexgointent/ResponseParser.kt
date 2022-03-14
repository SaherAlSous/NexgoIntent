package com.saher.nexgointent

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData


class ResponseParser {
    private val _packetID = MutableLiveData<String>()
    val packetID: LiveData<String>
    get() = _packetID

    private val _packetData = MutableLiveData<PacketDataDTO>()
    val packetData
    get() = _packetData

    fun setPacketID(packetID: String?) {
        _packetID.postValue(packetID)
    }

    fun setPacketData(packetData: PacketDataDTO?) {
        _packetData.postValue(packetData)
    }

}