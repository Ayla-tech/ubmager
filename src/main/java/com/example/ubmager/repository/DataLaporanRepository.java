package com.example.ubmager.repository;

import com.example.ubmager.entity.DataLaporan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DataLaporanRepository extends JpaRepository<DataLaporan, String> {
    @Modifying
    @Query("DELETE FROM DataLaporan l WHERE l.idPelapor = :idUser OR l.idReview IN "
    + "(SELECT r.idReview FROM DataReview r WHERE r.idReviewer = :idUser OR r.idTarget = :idUser)")
    void hapusByUser(@Param("idUser") String idUser);
}
