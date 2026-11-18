package com.f52124014.todoozi.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Tenggat disimpan sebagai teks ISO "yyyy-MM-dd" agar bisa diurutkan dan dikirim ke server. */
public final class DateUtil {

    private static final String ISO = "yyyy-MM-dd";

    private DateUtil() {}

    public static boolean isEmpty(String iso) {
        return iso == null || iso.trim().isEmpty() || "null".equalsIgnoreCase(iso.trim());
    }

    private static boolean isIso(String value) {
        return value != null && value.matches("\\d{4}-\\d{2}-\\d{2}");
    }

    public static String todayIso() {
        return new SimpleDateFormat(ISO, Locale.US).format(new Date());
    }

    /** True jika tanggal tenggat sudah lewat dari hari ini. */
    public static boolean isOverdue(String iso) {
        return isIso(iso) && iso.compareTo(todayIso()) < 0;
    }

    /** "2026-11-18" -> "18 Nov 2026" (format Indonesia). */
    public static String formatDisplay(String iso) {
        if (isEmpty(iso)) return "";
        if (!isIso(iso)) return iso;
        try {
            Date date = new SimpleDateFormat(ISO, Locale.US).parse(iso);
            return new SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("id-ID")).format(date);
        } catch (ParseException e) {
            return iso;
        }
    }

    /** Tanggal ISO -> milidetik UTC (format yang dipakai MaterialDatePicker). */
    public static long toUtcMillis(String iso) {
        try {
            SimpleDateFormat format = new SimpleDateFormat(ISO, Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = format.parse(iso);
            return date != null ? date.getTime() : System.currentTimeMillis();
        } catch (ParseException e) {
            return System.currentTimeMillis();
        }
    }

    /** Milidetik UTC dari MaterialDatePicker -> tanggal ISO. */
    public static String fromUtcMillis(long millis) {
        SimpleDateFormat format = new SimpleDateFormat(ISO, Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date(millis));
    }
}
