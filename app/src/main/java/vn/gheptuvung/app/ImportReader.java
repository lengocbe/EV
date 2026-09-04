package vn.gheptuvung.app;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Imports two-column vocabularies: English in column A and Vietnamese in column B. */
public final class ImportReader {
    private ImportReader() { }

    public static List<WordItem> read(ContentResolver resolver, Uri uri, String fileName) throws IOException {
        String lower = fileName == null ? "" : fileName.toLowerCase();
        if (lower.endsWith(".csv")) return readCsv(resolver.openInputStream(uri));
        if (lower.endsWith(".xlsx")) return readXlsx(resolver.openInputStream(uri));
        throw new IOException("Chỉ hỗ trợ file .csv hoặc .xlsx.");
    }

    private static List<WordItem> readCsv(InputStream stream) throws IOException {
        if (stream == null) throw new IOException("Không mở được file.");
        List<WordItem> words = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) { line = line.replace("\uFEFF", ""); first = false; }
                List<String> columns = parseCsvLine(line);
                if (columns.size() < 2) continue;
                addIfValid(words, columns.get(0), columns.get(1));
            }
        }
        return removeHeader(words);
    }

    private static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') { current.append(c); i++; }
                else quoted = !quoted;
            } else if (c == ',' && !quoted) {
                values.add(current.toString().trim()); current.setLength(0);
            } else current.append(c);
        }
        values.add(current.toString().trim());
        return values;
    }

    private static List<WordItem> readXlsx(InputStream stream) throws IOException {
        if (stream == null) throw new IOException("Không mở được file.");
        byte[] shared = null;
        byte[] sheet = null;
        try (ZipInputStream zip = new ZipInputStream(stream)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().equals("xl/sharedStrings.xml")) shared = readAll(zip);
                if (entry.getName().equals("xl/worksheets/sheet1.xml")) sheet = readAll(zip);
                zip.closeEntry();
            }
        }
        if (sheet == null) throw new IOException("File Excel không có trang dữ liệu đầu tiên.");
        List<String> sharedStrings = shared == null ? new ArrayList<>() : readSharedStrings(shared);
        List<WordItem> result = readSheet(sheet, sharedStrings);
        return removeHeader(result);
    }

    private static byte[] readAll(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int count;
        while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        return output.toByteArray();
    }

    private static List<String> readSharedStrings(byte[] xml) throws IOException {
        List<String> values = new ArrayList<>();
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(new ByteArrayInputStream(xml), "UTF-8");
            StringBuilder current = null;
            int event;
            while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && "si".equals(parser.getName())) current = new StringBuilder();
                else if (event == XmlPullParser.START_TAG && "t".equals(parser.getName()) && current != null) current.append(parser.nextText());
                else if (event == XmlPullParser.END_TAG && "si".equals(parser.getName()) && current != null) { values.add(current.toString()); current = null; }
            }
        } catch (Exception error) { throw new IOException("Không đọc được dữ liệu Excel.", error); }
        return values;
    }

    private static List<WordItem> readSheet(byte[] xml, List<String> shared) throws IOException {
        List<WordItem> words = new ArrayList<>();
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(new ByteArrayInputStream(xml), "UTF-8");
            Map<String, String> row = new HashMap<>();
            String ref = null;
            String type = null;
            int event;
            while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && "row".equals(parser.getName())) row.clear();
                else if (event == XmlPullParser.START_TAG && "c".equals(parser.getName())) {
                    ref = parser.getAttributeValue(null, "r"); type = parser.getAttributeValue(null, "t");
                } else if (event == XmlPullParser.START_TAG && ("v".equals(parser.getName()) || "t".equals(parser.getName())) && ref != null) {
                    String value = parser.nextText();
                    if ("s".equals(type)) {
                        try { value = shared.get(Integer.parseInt(value)); } catch (Exception ignored) { }
                    }
                    row.put(ref.replaceAll("[0-9]", ""), value);
                } else if (event == XmlPullParser.END_TAG && "row".equals(parser.getName())) {
                    addIfValid(words, row.get("A"), row.get("B"));
                } else if (event == XmlPullParser.END_TAG && "c".equals(parser.getName())) { ref = null; type = null; }
            }
        } catch (Exception error) { throw new IOException("Không đọc được nội dung Excel.", error); }
        return words;
    }

    private static void addIfValid(List<WordItem> words, String english, String vietnamese) {
        if (english != null && vietnamese != null && !english.trim().isEmpty() && !vietnamese.trim().isEmpty())
            words.add(new WordItem(0, english.trim(), vietnamese.trim()));
    }

    private static List<WordItem> removeHeader(List<WordItem> words) {
        if (!words.isEmpty() && words.get(0).english.equalsIgnoreCase("english")
                && (words.get(0).vietnamese.equalsIgnoreCase("vietnamese") || words.get(0).vietnamese.equalsIgnoreCase("tiếng việt")))
            words.remove(0);
        return words;
    }
}
