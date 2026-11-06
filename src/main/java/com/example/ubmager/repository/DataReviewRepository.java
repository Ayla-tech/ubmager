package com.example.ubmager.repository;

import com.example.ubmager.entity.DataReview;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DataReviewRepository extends JpaRepository<DataReview, String> {
    boolean existsByIdPesananAndIdReviewer(String idPesanan, String idReviewer);
    List<DataReview> findByIdTargetOrderByTanggalReviewDesc(String idTarget);

    @Query("SELECT AVG(r.rating) FROM DataReview r WHERE r.idTarget = :idTarget")
    Double hitungRataRata(@Param("idTarget") String idTarget);

    @Modifying
    @Query("DELETE FROM DataReview r WHERE r.idReviewer = :idUser OR r.idTarget = :idUser")
    void hapusByUser(@Param("idUser") String idUser);
}