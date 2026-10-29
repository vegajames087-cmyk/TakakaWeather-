package nz.co.weatherandrivers;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class RiverTableParserTest {
    private static final String SAMPLE =
            "<table><tr><th>Site Name</th><th>Date/Time</th><th>Water Level</th>" +
            "<th>Level Change</th><th>Flow (m3/s)</th><th>Peak Flow</th></tr>" +
            "<tr><td>Aorere at Devils Boots</td><td>30/10/2026 10:00</td><td>1513</td>" +
            "<td>-1</td><td>46.3</td><td>447</td></tr>" +
            "<tr><td>Fish Creek at Pupu Springs</td><td>30/10/2026 10:00</td><td>460</td>" +
            "<td>0</td><td>2.52</td><td>3.02</td></tr></table>";

    @Test public void parsesRequestedRowsAndUsesFlowColumn() {
        List<RiverTableParser.Reading> rows = RiverTableParser.parse(SAMPLE,
                new String[]{"Aorere at Devils Boots", "Fish Creek at Pupu Springs"});
        assertEquals(2, rows.size());
        assertEquals("Aorere at Devils Boots", rows.get(0).site);
        assertEquals("30/10/2026 10:00", rows.get(0).observedAt);
        assertEquals("46.3", rows.get(0).flow);
        assertEquals("Fish Creek at Pupu Springs", rows.get(1).site);
        assertEquals("2.52", rows.get(1).flow);
    }

    @Test public void ignoresUnrequestedSites() {
        List<RiverTableParser.Reading> rows = RiverTableParser.parse(SAMPLE,
                new String[]{"Takaka at Kotinga"});
        assertTrue(rows.isEmpty());
    }

    @Test public void ignoresRowsWithoutNumericFlow() {
        String html = SAMPLE.replace("<td>46.3</td>", "<td>not available</td>");
        List<RiverTableParser.Reading> rows = RiverTableParser.parse(html,
                new String[]{"Aorere at Devils Boots", "Fish Creek at Pupu Springs"});
        assertEquals(1, rows.size());
        assertEquals("Fish Creek at Pupu Springs", rows.get(0).site);
    }
}