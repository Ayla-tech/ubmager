package com.example.ubmager.repository;

import com.example.ubmager.entity.DataOTP;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DataOTPRepository extends JpaRepository<DataOTP, String> {
    @Query("SELECT o FROM DataOTP o WHERE o.emailAtauHp = :email AND o.kodeOtp = :kode "
    + "AND o.statusOtp = 'menunggu' AND o.waktuKadaluarsa > :sekarang ORDER BY o.waktuDibuat DESC")
    List<DataOTP> cariOtpValid(@Param("email") String email, @Param("kode") String kode, @Param("sekarang") LocalDateTime sekarang);

    @Modifying
    @Query("UPDATE DataOTP o SET o.jumlahSalah = o.jumlahSalah + 1 WHERE o.emailAtauHp = :email AND o.statusOtp = 'menunggu'")
    void tambahJumlahSalah(@Param("email") String email);

    @Modifying
    @Query("DELETE FROM DataOTP o WHERE o.emailAtauHp = :email")
    void hapusByEmail(@Param("email") String email);
}
