package com.davi.datalayer.services

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import com.davi.datalayer.MainActivity
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

class DataLayerListenerService : WearableListenerService() {

    private val messageClient by lazy { Wearable.getMessageClient(this) }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @SuppressLint("VisibleForTests")
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        Log.w(TAG, "onDataChanged ================")

        dataEvents.forEach { dataEvent ->
            val uri = dataEvent.dataItem.uri
            when (uri.path) {
                COUNT_PATH -> {
                    scope.launch {
                        try {
                            val nodeId = uri.host!!
                            val payload = uri.toString().toByteArray()
                            messageClient.sendMessage(
                                nodeId,
                                DATA_ITEM_RECEIVED_PATH,
                                payload
                            )
                                .await()
                            Log.d(TAG, "Message sent successfully")
                        } catch (cancellationException: CancellationException) {
                            throw cancellationException
                        } catch (exception: Exception) {
                            Log.d(TAG, "Message failed")
                        }
                    }
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        Log.w(TAG, "onMessageReceived ================")

        /**
         * 원래는 모바일앱에서 Start Wearable Activity 버튼을 누르면 이곳으로 신호가 오고
         * 워치의 MainActivity가 실행되는 소스였다.
         * 모바일앱에서 보낸 json string 을 로그를 찍어서 확인하고, 잘받았습니다 string을 모바일앱으로 보내는 것으로 수정함.
         */
        when (messageEvent.path) {
            START_ACTIVITY_PATH -> {
                Log.d(TAG, "전달받은 String : ${String(messageEvent.data)}")
//                startActivity(
//                    Intent(this, MainActivity::class.java)
//                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                )
                scope.launch {
                    messageClient.sendMessage(
                        messageEvent.sourceNodeId,
                        START_ACTIVITY_PATH,
                        "잘받았습니다.".toByteArray()
                    ).await()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        private const val TAG = "DataLayerService"

        private const val START_ACTIVITY_PATH = "/start-activity"
        private const val DATA_ITEM_RECEIVED_PATH = "/data-item-received"
        const val COUNT_PATH = "/count"
        const val IMAGE_PATH = "/image"
        const val RESPONSE = "/response"
        const val IMAGE_KEY = "photo"
    }
}