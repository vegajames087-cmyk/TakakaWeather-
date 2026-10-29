package nz.co.takakaweather;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RiverDataTest {
    private static final String HEAD =
        "<table><tr><th>Site Name</th><th>Date/Time (NZST)</th><th>Water Level (mm)</th>" +
        "<th>Level Change (mm/hr)</th><th>Flow (m3/s)</th><th>Peak Flow (m3/s)</th>" +
        "<th>Peak Water Level (mm)</th><th>Date of Peak</th></tr>";

    private static final String KOTINGA =
        "<tr><td><a>Takaka at Kotinga</a></td><td>30/10/2026 10:00</td><td>1472</td>" +
        "<td>-4</td><td>11.5</td><td>15.9</td><td>1572</td><td>27/10/2026 13:15</td></tr>";

    private static final String HARWOODS =
        "<tr><td><a>Takaka at Harwoods</a></td><td>30/10/2026 10:00</td><td>1025</td>" +
        "<td>-9</td><td>9.89</td><td>12.4</td><td>1086</td><td>29/10/2026 22:50</td></tr>";

    @Test public void parsesBothTakakaGaugeRowsFromCouncilTable() throws Exception {
        JSONObject result = RiverData.parseReportHtml(HEAD + KOTINGA + HARWOODS + "</table>");
        JSONObject kotinga = result.getJSONObject("kotinga");
        JSONObject harwoods = result.getJSONObject("harwoods");

        assertEquals(11.5, kotinga.getDouble("flow"), 0.001);
        assertEquals(1472.0, kotinga.getDouble("level"), 0.001);
        assertEquals(-4.0, kotinga.getDouble("delta"), 0.001);
        assertEquals(15.9, kotinga.getDouble("peak"), 0.001);
        assertEquals(1572.0, kotinga.getDouble("peakLevel"), 0.001);
        assertEquals("27/10/2026 13:15", kotinga.getString("peakTime"));
        assertEquals("30/10/2026 10:00 NZST", kotinga.getString("time"));

        assertEquals(9.89, harwoods.getDouble("flow"), 0.001);
        assertEquals(1025.0, harwoods.getDouble("level"), 0.001);
        assertEquals(-9.0, harwoods.getDouble("delta"), 0.001);
        assertNotNull(result.getString("source"));
    }

    @Test public void rejectsReportWithoutBothGaugeRows() throws Exception {
        try {
            RiverData.parseReportHtml(HEAD + KOTINGA + "</table>");
        } catch (Exception expected) {
            return;
        }
        throw new AssertionError("Missing Harwoods data should fail instead of displaying fake values");
    }

    @Test public void parsesPipeDelimitedFallback() throws Exception {
        String fallback = "Takaka at Kotinga|30/10/2026 10:00|1472|-4|11.5|15.9|1572|27/10/2026 13:15\n" +
            "Takaka at Harwoods|30/10/2026 10:00|1025|-9|9.89|12.4|1086|29/10/2026 22:50";
        JSONObject result = RiverData.parseReportHtml(fallback);
        assertEquals(11.5, result.getJSONObject("kotinga").getDouble("flow"), 0.001);
        assertEquals(9.89, result.getJSONObject("harwoods").getDouble("flow"), 0.001);
    }
}