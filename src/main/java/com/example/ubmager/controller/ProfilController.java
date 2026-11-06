package com.example.ubmager.controller;

import com.example.ubmager.dto.Hasil;
import com.example.ubmager.entity.DataOTP;
import com.example.ubmager.entity.Profil;
import com.example.ubmager.entity.User;
import com.example.ubmager.repository.DataLaporanRepository;
import com.example.ubmager.repository.DataOTPRepository;
import com.example.ubmager.repository.DataReviewRepository;
import com.example.ubmager.repository.PesananRepository;
import com.example.ubmager.repository.ProfilRepository;
import com.example.ubmager.repository.UserRepository;
import com.example.ubmager.util.IdGenerator;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profil")
@Transactional
public class ProfilController {

    private final UserRepository userRepository;
    private final DataOTPRepository dataOTPRepository;
    private final ProfilRepository profilRepository;
    private final DataReviewRepository dataReviewRepository;
    private final DataLaporanRepository dataLaporanRepository;
    private final PesananRepository pesananRepository;
    private final IdGenerator idGenerator;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    public ProfilController(UserRepository userRepository, DataOTPRepository dataOTPRepository,
                            ProfilRepository profilRepository, DataReviewRepository dataReviewRepository,
                            DataLaporanRepository dataLaporanRepository, PesananRepository pesananRepository,
                            IdGenerator idGenerator) {
        this.userRepository = userRepository;
        this.dataOTPRepository = dataOTPRepository;
        this.profilRepository = profilRepository;
        this.dataReviewRepository = dataReviewRepository;
        this.dataLaporanRepository = dataLaporanRepository;
        this.pesananRepository = pesananRepository;
        this.idGenerator = idGenerator;
    }

    @PostMapping("/registrasi")
    public ResponseEntity<Hasil> registrasi(@RequestBody Map<String, Object> body) {
        return prosesKelolaProfil(null, "CREATE", body, "", false).kirim(HttpStatus.OK);
    }

    @PostMapping("/verifikasi")
    public ResponseEntity<Hasil> verifikasi(@RequestBody Map<String, Object> body) {
        return prosesKelolaProfil(null, "VERIFIKASI_OTP", body, "", false).kirim(HttpStatus.CREATED);
    }

    @GetMapping("/{idUser}")
    public ResponseEntity<Hasil> lihat(@PathVariable String idUser) {
        return prosesKelolaProfil(idUser, "READ", Map.of(), "", false).kirim(HttpStatus.OK);
    }

    @PutMapping("/{idUser}")
    public ResponseEntity<Hasil> ubah(@PathVariable String idUser, @RequestBody Map<String, Object> body) {
        return prosesKelolaProfil(idUser, "UPDATE", body, "", false).kirim(HttpStatus.OK);
    }

    @DeleteMapping("/{idUser}")
    public ResponseEntity<Hasil> hapus(@PathVariable String idUser, @RequestBody Map<String, Object> body) {
        boolean konfirmasi = Boolean.parseBoolean(String.valueOf(body.get("konfirmasi_hapus")));
        return prosesKelolaProfil(idUser, "DELETE", body, teks(body, "password"), konfirmasi).kirim(HttpStatus.OK);
    }

    public Hasil prosesKelolaProfil(String idUser, String aksi, Map<String, Object> dataProfil,
                                    String passwordInput, boolean konfirmasiHapus) {
        if (aksi.equals("CREATE")) {
            return kirimKodeRegistrasi(dataProfil);
        } else if (aksi.equals("VERIFIKASI_OTP")) {
            return buatProfil(dataProfil);
        } else if (aksi.equals("READ")) {
            return lihatProfil(idUser);
        } else if (aksi.equals("UPDATE")) {
            return editProfil(idUser, dataProfil);
        } else if (aksi.equals("DELETE")) {
            return hapusProfil(idUser, passwordInput, konfirmasiHapus, teks(dataProfil, "pilihan_pengguna"));
        }
        return new Hasil(false, "Aksi tidak valid", null);
    }

    private Hasil kirimKodeRegistrasi(Map<String, Object> data) {
        if (!dataRegistrasiValid(data) || sudahTerdaftar(data)) {
            return new Hasil(false, "Registrasi gagal", null);
        }

        String kode = String.format("%06d", random.nextInt(1000000));
        LocalDateTime sekarang = LocalDateTime.now();

        DataOTP otp = new DataOTP();
        otp.setIdOtp(idGenerator.buat("OTP", "dataotp", "id_otp"));
        otp.setEmailAtauHp(teks(data, "email_atau_hp"));
        otp.setKodeOtp(kode);
        otp.setWaktuDibuat(sekarang);
        otp.setWaktuKadaluarsa(sekarang.plusMinutes(5));
        otp.setJumlahSalah((short) 0);
        otp.setStatusOtp("menunggu");
        dataOTPRepository.save(otp);

        kirimOTP(teks(data, "email_atau_hp"), kode);

        return new Hasil(true, "Kode OTP berhasil dikirim", null);
    }

    private Hasil buatProfil(Map<String, Object> data) {
        if (!dataRegistrasiValid(data) || sudahTerdaftar(data)) {
            return new Hasil(false, "Registrasi gagal", null);
        }

        if (!validasiKode(teks(data, "email_atau_hp"), teks(data, "kode_input"))) {
            return new Hasil(false, "Verifikasi OTP gagal", null);
        }

        LocalDateTime sekarang = LocalDateTime.now();
        User user = new User();
        user.setIdUser(idGenerator.buat("USR", "users", "id_user"));
        user.setUsername(teks(data, "username"));
        user.setEmailAtauHp(teks(data, "email_atau_hp"));
        user.setPassword(encoder.encode(teks(data, "password")));
        user.setStatusAkun("aktif");
        user.setCreatedAt(sekarang);
        user.setUpdateAt(sekarang);
        user.setRataRataRating(0.0f);
        user.setBadgeKepercayaan("Pengguna Terpercaya Baru");
        userRepository.save(user);

        Profil profil = new Profil();
        profil.setIdProfil(idGenerator.buat("PRF", "profil", "id_profil"));
        profil.setIdUser(user.getIdUser());
        profil.setNamaLengkap(teks(data, "nama_lengkap"));
        profil.setFotoProfil("default.jpg");
        profilRepository.save(profil);

        return new Hasil(true, "Profil berhasil dibuat",
                Map.of("id_user", user.getIdUser(), "id_profil", profil.getIdProfil()));
    }

    private Hasil lihatProfil(String idUser) {
        Optional<Profil> profil = profilRepository.findByIdUser(idUser);
        if (profil.isPresent()) {
            return new Hasil(true, "Profil ditemukan", profil.get());
        }
        return new Hasil(false, "Profil tidak ditemukan", null);
    }

    private Hasil editProfil(String idUser, Map<String, Object> data) {
        String nama = teks(data, "nama_lengkap");
        if (nama.isBlank()) {
            return new Hasil(false, "Data profil tidak valid", null);
        }

        Optional<Profil> cari = profilRepository.findByIdUser(idUser);
        if (cari.isEmpty()) {
            return new Hasil(false, "Profil tidak ditemukan", null);
        }

        String foto = teks(data, "foto_profil");
        String bio = teks(data, "bio");
        Profil profil = cari.get();
        profil.setNamaLengkap(nama);
        profil.setFotoProfil(foto.isBlank() ? "default.jpg" : foto);
        profil.setBio(bio.isBlank() ? null : bio);
        profilRepository.save(profil);

        userRepository.findById(idUser).ifPresent(user -> user.setUpdateAt(LocalDateTime.now()));

        return new Hasil(true, "Profil berhasil diperbarui", null);
    }

    private Hasil hapusProfil(String idUser, String passwordInput, boolean konfirmasiHapus, String pilihan) {
        User user = userRepository.findById(idUser).orElse(null);
        boolean passwordValid = user != null && encoder.matches(passwordInput, user.getPassword());

        if (passwordValid && konfirmasiHapus) {
            if (pilihan.equals("HAPUS_PERMANEN")) {
                if (pesananRepository.existsByIdPembeli(idUser)) {
                    return new Hasil(false, "Penghapusan dibatalkan atau gagal", null);
                }

                dataLaporanRepository.hapusByUser(idUser);
                dataReviewRepository.hapusByUser(idUser);
                profilRepository.hapusByIdUser(idUser);
                dataOTPRepository.hapusByEmail(user.getEmailAtauHp());
                userRepository.delete(user);
                return new Hasil(true, "Akun berhasil dihapus", null);
            } else if (pilihan.equals("NONAKTIF_SEMENTARA")) {
                user.setStatusAkun("nonaktif_sementara");
                user.setUpdateAt(LocalDateTime.now());
                userRepository.save(user);
                return new Hasil(true, "Akun berhasil dinonaktifkan", null);
            }
        }
        return new Hasil(false, "Penghapusan dibatalkan atau gagal", null);
    }

    private boolean validasiKode(String emailAtauHp, String kodeInput) {
        List<DataOTP> otp = dataOTPRepository.cariOtpValid(emailAtauHp, kodeInput, LocalDateTime.now());

        if (!otp.isEmpty()) {
            otp.get(0).setStatusOtp("tervalidasi");
            dataOTPRepository.save(otp.get(0));
            return true;
        }

        dataOTPRepository.tambahJumlahSalah(emailAtauHp);
        return false;
    }

    private void kirimOTP(String emailAtauHp, String kode) {
        System.out.println("OTP untuk " + emailAtauHp + " adalah " + kode);
    }

    private boolean dataRegistrasiValid(Map<String, Object> data) {
        return !teks(data, "username").isBlank() && !teks(data, "email_atau_hp").isBlank()
                && !teks(data, "password").isBlank() && !teks(data, "nama_lengkap").isBlank();
    }

    private boolean sudahTerdaftar(Map<String, Object> data) {
        return userRepository.existsByUsernameOrEmailAtauHp(teks(data, "username"), teks(data, "email_atau_hp"));
    }

    private String teks(Map<String, Object> data, String kunci) {
        Object nilai = data.get(kunci);
        return nilai == null ? "" : nilai.toString();
    }
}
