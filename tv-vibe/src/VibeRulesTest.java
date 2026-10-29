package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class VibeRulesTest {
    @Test public void aqiThresholdsUseUsScale() {
        assertEquals("Good", VibeRules.aqi(50)); assertEquals("Moderate", VibeRules.aqi(51));
        assertEquals("Moderate", VibeRules.aqi(100)); assertEquals("Unhealthy for sensitive groups", VibeRules.aqi(101));
        assertEquals("Unhealthy", VibeRules.aqi(151)); assertEquals("Very unhealthy", VibeRules.aqi(201)); assertEquals("Hazardous", VibeRules.aqi(301));
    }
    @Test public void renewalNeverClaimsConfirmedPayment() { assertEquals("Renewal date passed · update manually", VibeRules.due(-1,true)); }
    @Test public void dueDatesDistinguishTodayAndFuture() { assertEquals("Expires today", VibeRules.due(0,false)); assertEquals("Renews in 1 day",VibeRules.due(1,true)); }
    @Test(expected=java.time.DateTimeException.class) public void impossibleDateRejected() { VibeRules.date("2027-02-29"); }
    @Test(expected=IllegalArgumentException.class) public void ambiguousDateRejected() { VibeRules.date("11/12/26"); }
    @Test public void validLeapDayAccepted() { assertEquals(29,VibeRules.date("2028-02-29").getDayOfMonth()); }
}
