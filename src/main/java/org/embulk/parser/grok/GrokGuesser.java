package org.embulk.parser.grok;

import com.google.common.collect.ImmutableMap;
import oi.thekraken.grok.api.Grok;
import oi.thekraken.grok.api.Match;
import oi.thekraken.grok.api.exception.GrokException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class GrokGuesser {

    private List<String> guessPatterns;
    private List<String> patternFiles;

    public GrokGuesser(List<String> guessPatterns, List<String> patternFiles) {
        this.guessPatterns = guessPatterns;
        this.patternFiles = patternFiles;
    }

    public String guessPattern(List<String> sampleLines) throws GrokException {
        for (String guessPattern : guessPatterns) {
            Grok grok = new Grok();
            for (String file : patternFiles) {
                grok.addPatternFromFile(file);
            }
            try {
                grok.compile(guessPattern);
            } catch (GrokException e) {
                continue;
            }

            boolean allMatch = sampleLines.stream().allMatch(line -> {
                Match m = grok.match(line);
                m.captures();
                return !m.isNull();
            });
            if (allMatch) {
                return guessPattern;
            }
        }

        throw new GrokException("Patterns not matched");
    }

    public List<Map<String, Object>> guessColumns(List<String> sampleLines, String pattern) throws GrokException {

        Grok grok = new Grok();
        for (String file : patternFiles) {
            grok.addPatternFromFile(file);
        }
        grok.compile(pattern);

        List<Map<String, Object>> records = sampleLines.stream().map(line -> {
            Match m = grok.match(line);
            m.captures();
            return m.toMap();
        }).collect(Collectors.toList());

        return guessTypesFromRecords(records);
    }


    private List<Map<String, Object>> guessTypesFromRecords(List<Map<String, Object>> samples) {
        Map<String, ColumnType> types = new HashMap<>();
        for (Map<String, Object> record : samples) {
            for (Map.Entry<String, Object> entry : record.entrySet()) {
                ColumnType currentType = guessType(entry.getValue());
                if (types.containsKey(entry.getKey())) {
                    types.put(entry.getKey(), mergeType(currentType, types.get(entry.getKey())));
                } else {
                    types.put(entry.getKey(), currentType);
                }
            }
        }
        return types.entrySet().stream().map(entry -> {
            Map<String, Object> val = new HashMap<>();
            val.put("name", entry.getKey());
            val.put("type", entry.getValue().getType());
            if (entry.getValue().getType().equals("timestamp")) {
                val.put("format", entry.getValue().getFormat());
            }
            return val;
        }).collect(Collectors.toList());
    }

    private Map<String, SimpleDateFormat> timestampFormats = ImmutableMap.of(
            "%d/%b/%Y:%T %z", new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss z", Locale.ENGLISH),
            "%Y-%m-%d %H:%M:%S", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),
            "%Y-%m-%d %H:%M:%S.%N", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"),
            "%Y-%m-%d %H:%M:%S.%N %z", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z"),
            "%Y-%m-%dT%H:%M:%S.%N%z", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSz")
    );

    private ColumnType guessType(Object value) {

        if (value == null) {
            return new ColumnType("string");
        } else if (value instanceof Integer) {
            return new ColumnType("long");
        } else if (value instanceof Double) {
            return new ColumnType("double");
        } else {
            Optional<String> dateFormat = timestampFormats.entrySet().stream().filter(e -> {
                try {
                    return e.getValue().parse(value.toString()) != null;
                } catch (ParseException e1) {
                    return false;
                }
            }).map(Map.Entry::getKey).findFirst();
            if (dateFormat.isPresent()) {
                return new ColumnType("timestamp", dateFormat.get());
            } else {
                return new ColumnType("string");
            }
        }
    }

    private ColumnType mergeType(ColumnType t1, ColumnType t2) {
        if (t1.equals(t2)) {
            return t1;
        }

        if (t1.getType().equals("string") || t2.getType().equals("string")) {
            return new ColumnType("string");
        }

        if (t1.getType().equals("timestamp") || t2.getType().equals("timestamp")) {
            return new ColumnType("string");
        }

        if ((t1.getType().equals("long") && t2.getType().equals("double"))
                || (t1.getType().equals("double") && t2.getType().equals("long"))) {
            return new ColumnType("double");
        }

        return new ColumnType("string");
    }

    static class ColumnType {
        private String type;
        private String format;

        public ColumnType(String type) {
            this.type = type;
            this.format = null;
        }

        public ColumnType(String type, String format) {
            this.type = type;
            this.format = format;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ColumnType that = (ColumnType) o;

            if (type != null ? !type.equals(that.type) : that.type != null) return false;
            return format != null ? format.equals(that.format) : that.format == null;

        }

        @Override
        public int hashCode() {
            int result = type.hashCode();
            result = 31 * result + format.hashCode();
            return result;
        }
    }
}

