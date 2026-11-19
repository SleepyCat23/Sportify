# Rencana & Laporan Testing — Pertemuan 13

**Kelompok:** (isi nama kelompok)
**Nama aplikasi:** Sportify — aplikasi pemesanan lapangan olahraga (Kotlin, Jetpack Compose, Firebase, Stripe)

| Anggota (Nama / NIM) | Username GitHub | Tugas testing | Berkas test yang ditulis |
|----------------------|-----------------|---------------|--------------------------|
| (isi) | (isi) | contoh: AuthViewModel + ProfileViewModel | `AuthViewModelTest.kt`, `ProfileViewModelTest.kt` |
| (isi) | (isi) | contoh: HomeViewModel + VenueDetailViewModel | `HomeViewModelTest.kt`, `VenueDetailViewModelTest.kt`, `BuildTimeSlotsUseCaseTest.kt` |
| (isi) | (isi) | contoh: Checkout + Bookings ViewModel, UI test | `CheckoutViewModelTest.kt`, `BookingsViewModelTest.kt`, `HomeScreenTest.kt`, `BookingsScreenTest.kt` |

**Link repository:** https://github.com/SleepyCat23/Sportify
**Jenis proyek:**
- [ ] Room (data lokal)
- [ ] Retrofit / REST API
- [x] Firebase (Auth / Firestore / Storage), ditambah pembayaran lewat Stripe + Cloud Functions

---

## A. Audit Testability (hasil `cek_proyek.sh`)

| No | Pemeriksaan | Sudah | Belum | Commit perbaikan |
|----|-------------|:-----:|:-----:|------------------|
| 1 | Setiap ViewModel menerima repository lewat **constructor** (Hilt `@Inject`) | ✔ | | (sejak awal) |
| 2 | Repository punya **interface** (`AuthRepository`, `VenueRepository`, `BookingRepository`, `PaymentRepository`) | ✔ | | (sejak awal) |
| 3 | ViewModel tidak memakai `Context`/`Uri`/`Log`/`Toast`/Firebase langsung | ✔ | | `ProfileViewModel` memakai `android.net.Uri` → diubah menjadi `String`; `CheckoutViewModel` bergantung pada tipe Stripe `PaymentSheetResult` → dipisah menjadi `onPaymentCompleted/Canceled/Failed`. Commit: (isi hash) |
| 4 | Logika murni ada di fungsi/kelas tersendiri | ✔ | | `BuildTimeSlotsUseCase` (aturan slot waktu) dan properti `Booking.isHoldExpired` / `timeLabel` |
| 5 | Screen yang diuji **stateless** | ✔ | | `HomeScreen` dan `BookingsScreen` dipisah menjadi `…Route` (tipis, memegang ViewModel) + `…Screen` (stateless). Commit: (isi hash) |
| 6 | Ikon yang dapat diketuk punya `contentDescription` | ✔ | | Tombol kembali berlabel "Back"; ikon lain dekoratif (di samping teks) |

Catatan: screen lain (Login, Venue detail, Checkout, Profile) masih menerima ViewModel
langsung; tidak diuji di UI test sehingga tidak diubah.

## B. Daftar Unit yang Akan Diuji

| Jenis | Nama kelas / fungsi | Berkas | Prioritas (1–3) |
|-------|---------------------|--------|:---------------:|
| ViewModel | `CheckoutViewModel` (alur pembayaran) | `CheckoutViewModelTest.kt` | 1 |
| ViewModel | `VenueDetailViewModel` (slot, pemesanan) | `VenueDetailViewModelTest.kt` | 1 |
| ViewModel | `AuthViewModel` (validasi login/daftar) | `AuthViewModelTest.kt` | 1 |
| ViewModel | `HomeViewModel` (filter, pencarian, harga termurah) | `HomeViewModelTest.kt` | 2 |
| ViewModel | `BookingsViewModel`, `ProfileViewModel` | `BookingsViewModelTest.kt`, `ProfileViewModelTest.kt` | 3 |
| Fungsi murni | `BuildTimeSlotsUseCase` | `BuildTimeSlotsUseCaseTest.kt` | 1 |
| Model | `Booking` (`isHoldExpired`, `timeLabel`) | `BookingTest.kt` | 2 |
| Screen (UI) | `HomeScreen` | `HomeScreenTest.kt` (androidTest) | 2 |
| Screen (UI) | `BookingsScreen` | `BookingsScreenTest.kt` (androidTest) | 2 |
| DAO (tantangan) | tidak berlaku (proyek memakai Firestore, bukan Room) | — | — |

## C. Matriks Skenario ViewModel

| No | ViewModel | Skenario (Given / When / Then) | Jenis | Nama test |
|----|-----------|--------------------------------|-------|-----------|
| 1 | `CheckoutViewModel` | Given sesi pembayaran tersedia / When `pay()` / Then server diminta sesi, `session` terisi, `busy` true | Sukses | `pay asks the server for a payment session` |
| 2 | `CheckoutViewModel` | Given server menolak (reservasi kedaluwarsa) / When `pay()` / Then pesan error tampil dan spinner berhenti | Gagal | `pay failure shows the error and stops the spinner` |
| 3 | `HomeViewModel` | Given tidak ada venue yang cocok / When mencari "zzz" / Then daftar kosong dan tidak loading | Kosong | `no match gives an empty list that is not loading` |
| 4 | `VenueDetailViewModel` | Given jam 10 sudah dipesan / When durasi 2 jam dipilih / Then jam 9 tidak tersedia, jam 8 tersedia | Aksi pengguna | `a longer duration also blocks the start time just before a booked hour` |
| 5 | `AuthViewModel` | Given password 5 karakter / When daftar / Then pesan validasi tampil dan repository tidak dipanggil | Validasi | `sign up rejects a password shorter than 6 characters` |
| 6 | `VenueDetailViewModel` | Given slot baru saja diambil orang lain / When `book` / Then pesan tampil dan jam terpilih dihapus | Gagal | `book failure shows the message and clears the start time` |
| 7 | `CheckoutViewModel` | Given pembayaran sukses tetapi konfirmasi server gagal / When `onPaymentCompleted()` / Then tampil pemberitahuan, bukan error | Gagal sebagian | `confirm failure after paying shows a notice, not an error` |

**Test double yang dipakai:**

| Dependency asli | Pengganti di test | Jenis (fake/stub/mock) | Alasan |
|-----------------|-------------------|------------------------|--------|
| `VenueRepositoryImpl` (Firestore) | `FakeVenueRepository` | fake | data venue/court/jam terpesan berupa `MutableStateFlow`, dapat diubah tiap test |
| `BookingRepositoryImpl` (Firestore transaction) | `FakeBookingRepository` | fake | mencatat pemesanan yang dibuat; error dapat dipaksa lewat `createError`/`cancelError` |
| `PaymentRepositoryImpl` (Cloud Functions + Stripe) | `FakePaymentRepository` | fake | hasil sesi dan konfirmasi dapat dipaksa sukses/gagal tanpa jaringan |
| `AuthRepositoryImpl` (Firebase Auth) | `FakeAuthRepository` | fake | mencatat pemanggilan `signIn`/`signUp`; hasil dapat dipaksa |

Tidak ada MockK: seluruh test memeriksa **hasil** (state), bukan interaksi.

## D. Hasil

(Isi setelah test dijalankan. Angka di bawah wajib sesuai hasil sebenarnya.)

| Jenis test | Jumlah | Lulus | Perintah |
|------------|:------:|:-----:|----------|
| Unit test (JVM) | | | `./gradlew testDebugUnitTest` |
| UI test (Compose) | | | `./gradlew connectedDebugAndroidTest` |
| Integration test (DAO) | — | — | tidak berlaku |

- Screenshot hasil test Android Studio: `docs/test-unit.png`, `docs/test-ui.png`
- Link run GitHub Actions yang **hijau**:
- Link run GitHub Actions yang **merah** (commit yang sengaja merusak logika):
- (Bonus) Coverage baris ViewModel: ……… %

## E. Refleksi

(Tulis dengan kata-kata kelompok sendiri setelah menjalankan test; jawab apa adanya.)

1. **Bug apa yang ditemukan test?**
2. **Refactor apa yang Anda lakukan agar kode dapat diuji? Apa yang paling sulit?**
3. **Satu test yang menurut Anda paling bernilai, dan mengapa?**
4. **Apa yang belum diuji dan mengapa?** (Contoh yang jujur: alur Stripe PaymentSheet, transaksi Firestore
   pada `BookingRepositoryImpl`, dan Cloud Functions tidak diuji otomatis.)
