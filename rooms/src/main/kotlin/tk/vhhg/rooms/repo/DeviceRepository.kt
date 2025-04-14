package tk.vhhg.rooms.repo

import kotlinx.coroutines.flow.StateFlow
import tk.vhhg.rooms.model.DeviceDto

interface DeviceRepository {
    suspend fun createDevice(userId: Int, deviceDto: DeviceDto): DeviceDto?
    suspend fun putDevice(userId: Int, deviceDto: DeviceDto): Boolean
    suspend fun deleteDevice(userId: Int, deviceId: Long): Boolean
    suspend fun setDeviceValue(userId: Int, roomId: Long, deviceId: Long, value: Float): Boolean
    suspend fun getCurrentDeviceData(userId: Int, roomId: Long, deviceId: Long, fromMillis: Long?, toMillis: Long?): DeviceDto?
//    suspend fun getTopicFor(deviceId: Long): String?
    suspend fun getSubscription(userId: Int, roomId: Long, deviceId: Long): StateFlow<String>?
}