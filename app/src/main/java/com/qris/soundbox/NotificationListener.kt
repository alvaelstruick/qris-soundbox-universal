package com.qris.soundbox

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.*
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log

class NotificationListener : NotificationListenerService() {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var lastNotificationTime = 0L
    private val debounceMs = 3000L // 3 detik debounce untuk notif yang sama

    // Whitelist apps (pakai nama package)
    private val paymentApps = listOf(
        "com.bca", // BCA Mobile
        "com.bca.mycards", // BCA Cards
        "bankin.mobile.id", // Mandiri Mobile
        "com.clicknesia.mobilebanking", // Mandiri
        "com.gojek.app", // Gojek/GoPay
        "ovo.id", // OVO
        "id.dana", // DANA
        "com.shopee.app", // Shopee
        "id.co.bankdkc.danamon.mobil", // D-Bank
        "com.linkaja.apps", // LinkAja
        "com.bni.mcmobile", // BNI Mobile
        "com.bri.brimo", // BRI
        "id.co.buksuk", // BUKU (BRIVA)
        "com.islamic.bank.bsi.mobile", // BSI Mobile
        "com.jenius", // Jenius
        "id.co.nobu.android.mbanking", // Nobu
        "id.co.cimbniaga.mobile.user", // OCTO Mobile
        "com.telkom.cashit", // LinkAja Partner
        "id.co.homecredit.financing", // Home Credit
        "com.ace.aceonline", // ACE
        "com.indomaret.klikindomaret", // Klik Indomaret
        "com.alfamart.alfagift" // Alfagift
    )

    // Keywords yang harus ada (positif)
    private val includeKeywords = listOf(
        "terima", "masuk", "diterima", "berhasil", "sukses",
        "transfer", "pembayaran", "qris", "credit", "cr",
        "dapat", "received", "payment", "trx", "transaksi"
    )

    // Keywords yang harus dihindari (negatif)
    private val excludeKeywords = listOf(
        "tarik", "keluar", "debit", "dr", "bayar ke",
        "promo", "diskon", "cashback", "saldo akhir",
        "remaining", "khusus", "penawaran", "special",
        "verifikasi", "otp", "kode", "password"
    )

    // Universal regex patterns (priority order)
    private val patterns = listOf(
        // Pattern 1: "Terima Rp 50.000" / "Masuk Rp50.000"
        Regex("(terima|masuk|diterima|dapat|berhasil).*Rp\\.?\\s?([\\d.,]+)", RegexOption.IGNORE_CASE),
        
        // Pattern 2: "Rp 50.000 masuk" / "Rp50.000 diterima"
        Regex("Rp\\.?\\s?([\\d.,]+).*(masuk|terima|diterima|berhasil)", RegexOption.IGNORE_CASE),
        
        // Pattern 3: "IDR 50,000" / "IDR50000"
        Regex("(IDR|Rp)\\s?([\\d.,]+)", RegexOption.IGNORE_CASE),
        
        // Pattern 4: "50000 rupiah" / "50 ribu"
        Regex("([\\d.,]+)\\s*(rupiah|ribu|juta)", RegexOption.IGNORE_CASE),
        
        // Pattern 5: Pure number after keyword "masuk/terima" (fallback)
        Regex("(masuk|terima|diterima|dapat)\\s*:\\s*([\\d.,]+)", RegexOption.IGNORE_CASE),
        
        // Pattern 6: Amount with currency symbol
        Regex("[¥€£$]\\s?([\\d.,]+)|([\\d.,]+)\\s*[¥€£$]", RegexOption.IGNORE_CASE)
    )

    override fun onCreate() {
        super.onCreate()
        initTTS()
    }

    private fun initTTS() {
        tts = TextToSpeech(applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.let { engine ->
                    val result = engine.setLanguage(Locale("id", "ID"))
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        // Fallback ke bahasa Inggris jika bahasa Indonesia tidak tersedia
                        engine.setLanguage(Locale.US)
                    }
                    
                    engine.setSpeechRate(1.0f)
                    engine.setPitch(1.0f)
                    
                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}
                        override fun onDone(utteranceId: String?) {}
                        override fun onError(utteranceId: String?) {}
                    })
                    
                    isTtsReady = true
                }
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!isTtsReady) return

        val packageName = sbn.packageName

        // Check whitelist
        if (!paymentApps.any { packageName.contains(it) || it.contains(packageName) }) {
            return
        }

        // Debounce - skip jika notif terlalu cepat (3 detik terakhir dari app yang sama)
        val now = System.currentTimeMillis()
        if (now - lastNotificationTime < debounceMs) {
            return
        }

        val extras = sbn.notification.extras
        
        // Extract notification text
        val title = extras.getCharSequence("android.text")?.toString() ?: ""
        val text = extras.getCharSequence("android.bigText")?.toString() 
            ?: extras.getCharSequence("android.text")?.toString() 
            ?: ""

        val fullText = "$title $text"

        Log.d("QRIS-Soundbox", "Notification from $packageName: $fullText")

        // Process notification
        processNotification(fullText, packageName)
    }

    private fun processNotification(text: String, packageName: String) {
        // Check exclude keywords first
        if (excludeKeywords.any { text.contains(it, ignoreCase = true) }) {
            Log.d("QRIS-Soundbox", "Excluded (contains negative keywords): $text")
            return
        }

        // Check include keywords
        if (!includeKeywords.any { text.contains(it, ignoreCase = true) }) {
            Log.d("QRIS-Soundbox", "Skipped (no positive keywords): $text")
            return
        }

        // Try extract amount
        val amount = extractAmount(text)

        if (amount != null && amount > 0) {
            lastNotificationTime = System.currentTimeMillis()
            speakAmount(amount)
            
            // Log to database (implementasi nanti)
            saveTransaction(text, amount, packageName)
        } else {
            Log.d("QRIS-Soundbox", "No amount found in: $text")
        }
    }

    private fun extractAmount(text: String): Double? {
        for ((index, pattern) in patterns.withIndex()) {
            val match = pattern.find(text)
            if (match != null) {
                val amountStr = match.groups.last()?.value ?: match.groups[1]?.value ?: ""
                val amount = parseAmount(amountStr)
                
                Log.d("QRIS-Soundbox", "Pattern ${index + 1} matched: $amountStr -> $amount")
                
                if (amount > 0) {
                    return amount
                }
            }
        }
        return null
    }

    private fun parseAmount(str: String): Double {
        // Clean string
        var cleaned = str
            .replace(Regex("[RpRpRpIDRIDR\\s]"), "")
            .replace(Regex("[.,](\\d{3})"), "$1") // Remove thousand separators
            .replace(",", ".") // Decimal separator

        // Handle Indonesian format (1.000.000 -> 1000000)
        if (cleaned.count { it == '.' } > 1) {
            cleaned = cleaned.replace(".", "")
        }

        return try {
            cleaned.toDouble()
        } catch (e: NumberFormatException) {
            0.0
        }
    }

    private fun speakAmount(amount: Double) {
        val amountText = formatAmount(amount)
        val speechText = "Pembayaran $amountText rupiah masuk"

        Log.d("QRIS-Soundbox", "Speaking: $speechText")

        tts?.speak(speechText, TextToSpeech.QUEUE_ADD, null, "payment_notification")
    }

    private fun formatAmount(amount: Double): String {
        return when {
            amount >= 1_000_000 -> {
                val juta = amount / 1_000_000
                if (juta == juta.toLong().toDouble()) {
                    "${juta.toLong()} juta"
                } else {
                    String.format("%.1f juta", juta)
                }
            }
            amount >= 1_000 -> {
                val ribu = amount / 1_000
                if (ribu == ribu.toLong().toDouble()) {
                    "${ribu.toLong()} ribu"
                } else {
                    String.format("%.1f ribu", ribu)
                }
            }
            else -> {
                amount.toLong().toString()
            }
        }
    }

    private fun saveTransaction(text: String, amount: Double, packageName: String) {
        // TODO: Implement database storage
        // Untuk sekarang hanya log
        Log.d("QRIS-Soundbox", "Transaction saved: $amount from $packageName")
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // No action needed
    }
}
