package com.example.ubmager.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class Teks {
    private static final Locale ID = Locale.forLanguageTag("id-ID");
    private static final DateTimeFormatter TANGGAL = DateTimeFormatter.ofPattern("d MMM yyyy", ID);

    private Teks() {}

    public static String inisial(String nama) {
        if (nama == null || nama.isBlank()) {
            return "?";
        }
        String[] kata = nama.trim().split("\\s+");
        String hasil = kata[0].substring(0, 1);
        if (kata.length > 1) {
            hasil += kata[1].substring(0, 1);
        }
        return hasil.toUpperCase();
    }

    public static String rupiah(BigDecimal nilai) {
        if (nilai == null) {
            return "-";
        }
        return "Rp " + NumberFormat.getIntegerInstance(ID).format(nilai.longValue());
    }

    public static String tanggal(LocalDateTime waktu) {
        return waktu == null ? "-" : TANGGAL.format(waktu);
    }

    public static String rating(float nilai) {
        return String.format(Locale.US, "%.1f", nilai);
    }
}
