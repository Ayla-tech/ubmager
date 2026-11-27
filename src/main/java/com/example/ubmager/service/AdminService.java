package com.example.ubmager.service;

import com.example.ubmager.dto.Hasil;
import com.example.ubmager.entity.DataLaporan;
import com.example.ubmager.repository.DataLaporanRepository;
import com.example.ubmager.repository.DataReviewRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final DataLaporanRepository dataLaporanRepository;
    private final DataReviewRepository dataReviewRepository;
    private final RatingService ratingService;

    public AdminService(DataLaporanRepository dataLaporanRepository, DataReviewRepository dataReviewRepository,
                        RatingService ratingService) {
        this.dataLaporanRepository = dataLaporanRepository;
        this.dataReviewRepository = dataReviewRepository;
        this.ratingService = ratingService;
    }

    @Transactional
    public Hasil ubahStatus(String idLaporan, String statusBaru) {
        if (!List.of("diproses", "disetujui", "ditolak").contains(statusBaru)) {
            return new Hasil(false, "Status tidak valid", null);
        }
        DataLaporan laporan = dataLaporanRepository.findById(idLaporan).orElse(null);
        if (laporan == null) {
            return new Hasil(false, "Laporan tidak ditemukan", null);
        }
        String lama = laporan.getStatusLaporan();
        if (lama.equals("disetujui") || lama.equals("ditolak")) {
            return new Hasil(false, "Laporan sudah selesai diproses", null);
        }
        if (lama.equals(statusBaru)) {
            return new Hasil(false, "Laporan sudah berstatus " + statusBaru, null);
        }

        laporan.setStatusLaporan(statusBaru);
        dataLaporanRepository.saveAndFlush(laporan);

        if (statusBaru.equals("disetujui")) {
            dataReviewRepository.findById(laporan.getIdReview())
                    .ifPresent(review -> ratingService.hitungUlang(review.getIdTarget()));
        }
        return new Hasil(true, "Status laporan diubah menjadi " + statusBaru, null);
    }
}
