package com.example.ubmager.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class IdGenerator {
    @PersistenceContext
    private EntityManager entityManager;

    public String buat(String prefix, String tabel, String kolom) {
        List<?> hasil = entityManager.createNativeQuery(
        "SELECT " + kolom + " FROM " + tabel
        + " ORDER BY LENGTH(" + kolom + ") DESC, " + kolom + " DESC LIMIT 1").getResultList();

        int nomor = hasil.isEmpty() ? 1 : Integer.parseInt(hasil.get(0).toString().substring(3)) + 1;
        return prefix + String.format("%03d", nomor);
    }
}
