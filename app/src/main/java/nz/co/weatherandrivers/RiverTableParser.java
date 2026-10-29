package nz.co.weatherandrivers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Small dependency-free parser for the council's published river-flow HTML table. */
final class RiverTableParser {
    private static final Pattern ROW = Pattern.compile("(?is)<tr\\b[^>]*>(.*?)</tr>");
    private static final Pattern CELL = Pattern.compile("(?is)<t[dh]\\b[^>]*>(.*?)</t[dh]>");
    private static final Pattern FLOW = Pattern.compile("[0-9]+(?:\\.[0-9]+)?");

    static final class Reading {
        final String site;
        final String observedAt;
        final String flow;
        Reading(String site, String observedAt, String flow) {
            this.site = site;
            this.observedAt = observedAt;
            this.flow = flow;
        }
    }

    private RiverTableParser() {}

    static List<Reading> parse(String html, String[] requestedSites) {
        if (html == null || requestedSites == null || requestedSites.length == 0) {
            return Collections.emptyList();
        }
        ArrayList<Reading> readings = new ArrayList<>();
        Matcher rows = ROW.matcher(html);
        while (rows.find()) {
            Matcher cells = CELL.matcher(rows.group(1));
            ArrayList<String> values = new ArrayList<>();
            while (cells.find()) values.add(clean(cells.group(1)));
            if (values.size() < 5) continue;

            String site = values.get(0);
            String normalized = normalize(site);
            boolean wanted = false;
            for (String request : requestedSites) {
                if (normalize(request).equals(normalized)) {
                    wanted = true;
                    break;
                }
            }
            if (!wanted) continue;

            String flow = values.get(4).replace(",", "").trim();
            if (!FLOW.matcher(flow).matches()) continue;
            readings.add(new Reading(site, values.get(1), flow));
        }
        return readings;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replace("ā", "a").replace("ē", "e").replace("ī", "i")
                .replace("ō", "o").replace("ū", "u")
                .replaceAll("\\s+", " ");
    }

    private static String clean(String html) {
        return html.replaceAll("(?is)<(script|style)\\b[^>]*>.*?</\\1>", " ")
                .replaceAll("(?is)<[^>]+>", " ")
                .replace("&nbsp;", " ").replace("&#160;", " ")
                .replace("&amp;", "&").replace("&lt;", "<")
                .replace("&gt;", ">").replace("&quot;", "\"")
                .replace("&#39;", "'").replace("&#039;", "'")
                .replaceAll("\\s+", " ").trim();
    }
}