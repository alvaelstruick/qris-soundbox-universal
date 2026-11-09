package com.qris.soundbox

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.content.pm.PackageManager

class MainActivity : Activity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvLog: TextView
    private lateinit var btnEnable: Button
    private lateinit var btnTest: Button
    private lateinit var btnClear: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContentView(R.layout.activity_main)
        
        tvStatus = findViewById(R.id.tvStatus)
        tvLog = findViewById(R.id.tvLog)
        btnEnable = findViewById(R.id.btnEnable)
        btnTest = findViewById(R.id.btnTest)
        btnClear = findViewById(R.id.btnClear)

        updateStatus()

        btnEnable.setOnClickListener {
            openNotificationSettings()
        }

        btnTest.setOnClickListener {
            testTTS()
        }

        btnClear.setOnClickListener {
            tvLog.text = ""
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val isEnabled = isNotificationListenerEnabled()
        tvStatus.text = if (isEnabled) {
            "✅ Service AKTIF\n\nSoundbox siap membaca notifikasi pembayaran dari banking apps"
        } else {
            "⚠️ Service NONAKTIF\n\nTekan tombol di bawah untuk mengaktifkan akses notifikasi"
        }
        btnEnable.visibility = if (isEnabled) Button.GONE else Button.VISIBLE
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val componentName = ComponentName(this, NotificationListener::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat?.contains(componentName.flattenToString()) ?: false
    }

    private fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
        Toast.makeText(this, "Cari 'QRIS Soundbox' dan aktifkan", Toast.LENGTH_LONG).show()
    }

    private var testTts: android.speech.tts.TextToSpeech? = null
    
    private fun testTTS() {
        // Test TTS dengan nominal contoh
        val testAmounts = listOf(50000.0, 125000.0, 1500000.0, 25500.0)
        val random = java.util.Random()
        val testAmount = testAmounts[random.nextInt(testAmounts.size)]
        
        val speechText = "Pembayaran ${formatAmount(testAmount)} rupiah masuk"
        
        testTts = android.speech.tts.TextToSpeech(this) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                testTts?.let { ttsEngine ->
                    ttsEngine.language = java.util.Locale("id", "ID")
                    ttsEngine.speak(speechText, android.speech.tts.TextToSpeech.QUEUE_ADD, null, "test")
                    
                    runOnUiThread {
                        appendLog("🔊 TEST: $speechText")
                    }
                }
            } else {
                runOnUiThread {
                    appendLog("❌ TTS engine tidak tersedia")
                    Toast.makeText(this, "TTS tidak tersedia. Install Google TTS dari Play Store", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    override fun onDestroy() {
        testTts?.stop()
        testTts?.shutdown()
        super.onDestroy()
    }

    private fun formatAmount(amount: Double): String {
        return when {
            amount >= 1_000_000 -> "${(amount / 1_000_000).toLong()} juta"
            amount >= 1_000 -> "${(amount / 1_000).toLong()} ribu"
            else -> amount.toLong().toString()
        }
    }

    private fun appendLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        tvLog.append("[$timestamp] $message\n")
        
        // Auto scroll to bottom
        val scrollView = findViewById<ScrollView>(R.id.scrollView)
        scrollView.post {
            scrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    // Called from NotificationListener to log events
    companion object {
        private var instance: MainActivity? = null
        
        fun getInstance(): MainActivity? = instance
        
        fun logToUI(message: String) {
            instance?.runOnUiThread {
                instance?.appendLog(message)
            }
        }
    }
}
