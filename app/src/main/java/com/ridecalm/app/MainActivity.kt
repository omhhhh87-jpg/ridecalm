package com.ridecalm.app

import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var bridge: VehicleBridge? = null
    private lateinit var engine: RideEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Pleos TTS SDK 공개 전까지 안드로이드 기본 음성 사용
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = (tts?.setLanguage(Locale.KOREAN) ?: TextToSpeech.LANG_NOT_SUPPORTED) >= 0
            }
        }
        engine = RideEngine(lifecycleScope) { msg ->
            if (ttsReady) tts?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "ridecalm")
        }
        setContent { RideCalmApp(engine) }

        if (checkSelfPermission(PERM_SPEED) == PackageManager.PERMISSION_GRANTED) connectVehicle()
        else requestPermissions(arrayOf(PERM_SPEED), REQ_SPEED)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_SPEED) connectVehicle()
    }

    private fun connectVehicle() {
        if (bridge != null) return
        bridge = VehicleBridge(
            this,
            onSpeed = { v -> runOnUiThread { engine.onVehicleSpeed(v) } },
            onGear = { g -> runOnUiThread { engine.onVehicleGear(g) } },
            onLinked = { ok -> runOnUiThread { engine.vehicleLinked = ok } },
        ).also { it.connect() }
    }

    override fun onDestroy() {
        bridge?.disconnect()
        tts?.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val PERM_SPEED = "android.car.permission.CAR_SPEED"
        private const val REQ_SPEED = 7
    }
}
