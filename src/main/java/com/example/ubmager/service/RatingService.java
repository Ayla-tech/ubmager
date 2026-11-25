package com.example.ubmager.service;

import com.example.ubmager.repository.DataReviewRepository;
import com.example.ubmager.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RatingService {

    private final UserRepository userRepository;
    private final DataReviewRepository dataReviewRepository;

    public RatingService(UserRepository userRepository, DataReviewRepository dataReviewRepository) {
        this.userRepository = userRepository;
        this.dataReviewRepository = dataReviewRepository;
    }

    // Hitung ulang rata-rata rating dan badge. Review yang laporannya disetujui tidak dihitung.
    @Transactional
    public void hitungUlang(String idTarget) {
        userRepository.findById(idTarget).ifPresent(user -> {
            Double rataMentah = dataReviewRepository.hitungRataRata(idTarget);
            double rata = rataMentah == null ? 0.0 : Math.round(rataMentah * 10) / 10.0;
            user.setRataRataRating((float) rata);
            user.setBadgeKepercayaan(rata >= 4.5 ? "Penjual Terpercaya" : "Pengguna Terpercaya Baru");
            userRepository.save(user);
        });
    }
}
