# QRIS Soundbox - Universal Payment Notification Reader

Aplikasi Android yang membaca notifikasi pembayaran QRIS secara real-time dan mengumumkan nominal dengan suara.

## Fitur

- ✅ **Universal Pattern Matching** - Support 20+ banking/payment apps Indonesia
- ✅ **Real-time TTS** - Suara otomatis saat notifikasi masuk
- ✅ **Smart Filtering** - Anti false-positive dengan keyword filtering
- ✅ **Multiple Format Support** - Baca berbagai format nominal (Rp 50.000, Rp50000, 50 ribu, dll)
- ✅ **Debounce Logic** - Mencegah duplicate announcement
- ✅ **Easy Setup** - Hanya perlu aktifkan notification access

## Instalasi

1. Build APK atau download release
2. Install di HP Android (min Android 6.0)
3. Buka app, klik "Aktifkan Notification Access"
4. Cari "QRIS Soundbox" di daftar, aktifkan
5. Hubungkan speaker via Bluetooth atau jack audio
6. Test dengan tombol "Test Audio"

## Supported Apps

- BCA Mobile
- Mandiri Mobile
- GoPay/Gojek
- OVO
- DANA
- ShopeePay
- LinkAja
- BNI Mobile
- BRI Mobile
- BSI Mobile
- Jenius
- Danamon
- CIMB Niaga
- Dan aplikasi pembayaran lainnya

## Cara Kerja

1. App mendengarkan notifikasi dari apps banking
2. Parse text notifikasi dengan multiple regex patterns
3. Extract nominal dengan smart filtering
4. Convert ke format natural (50 ribu, 1 juta, dll)
5. TTS announce: "Pembayaran [nominal] rupiah masuk"

## Pattern Matching

Support berbagai format:
- "Terima Rp 50.000"
- "Masuk Rp50000"
- "Rp 50.000 diterima"
- "IDR 50,000"
- "50000 rupiah"
- "50 ribu masuk"
- Dan lain-lain

## Troubleshooting

### Tidak ada suara
- Pastikan volume media tidak nol
- Pastikan speaker sudah terhubung
- Test dengan tombol "Test Audio"
- Cek TTS engine di Settings > Accessibility > Text-to-Speech

### Notifikasi tidak terbaca
- Pastikan notification access sudah aktif
- Pastikan app banking tidak di-silent
- Cek whitelist di kode (bisa ditambahkan)
- Lihat log di app untuk debugging

### Format nominal salah
- Kirim screenshot notifikasi untuk improvement
- Pattern akan di-update via crowdsource

## Development

Built with:
- Kotlin
- Android NotificationListenerService
- TextToSpeech API
- Regex Pattern Matching

## Roadmap

- [ ] Database untuk history transaksi
- [ ] Export to CSV/Excel
- [ ] Cloud pattern update
- [ ] Custom voice/speed settings
- [ ] Widget untuk quick toggle
- [ ] Support lebih banyak app

## Lisensi

MIT License - Bebas dimodifikasi dan dikembangkan
