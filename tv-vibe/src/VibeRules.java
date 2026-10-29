package com.gathin.tvhome;

import java.time.LocalDate;
final class VibeRules {
    static String aqi(int n) {
        if (n <= 50) return "Good";
        if (n <= 100) return "Moderate";
        if (n <= 150) return "Unhealthy for sensitive groups";
        if (n <= 200) return "Unhealthy";
        if (n <= 300) return "Very unhealthy";
        return "Hazardous";
    }
    static LocalDate date(String value) {
        if (!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) throw new IllegalArgumentException("ISO date required");
        return LocalDate.parse(value);
    }
    static String due(long days, boolean auto) {
        if (days < 0) return auto ? "Renewal date passed · update manually" : "Expiry date passed · check account";
        return (auto ? "Renews " : "Expires ") + (days == 0 ? "today" : "in " + days + (days == 1 ? " day" : " days"));
    }
}
