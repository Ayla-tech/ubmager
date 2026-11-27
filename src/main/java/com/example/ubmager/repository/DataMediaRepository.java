package com.example.ubmager.repository;

import com.example.ubmager.entity.DataMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DataMediaRepository extends JpaRepository<DataMedia, String> {
    List<DataMedia> findByIdPostingan(String idPostingan);
    void deleteByIdPostingan(String idPostingan);
}