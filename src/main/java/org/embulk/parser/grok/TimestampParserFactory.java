package org.embulk.parser.grok;

import com.google.common.collect.ImmutableMap;
import org.embulk.config.Task;
import org.embulk.spi.ColumnConfig;
import org.embulk.spi.time.Timestamp;
import org.embulk.spi.time.TimestampParser;
import org.embulk.spi.type.TimestampType;
import org.embulk.spi.util.Timestamps;
import org.joda.time.DateTimeZone;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class TimestampParserFactory {
    private static Map<String, String> formatMap = ImmutableMap.<String, String>builder()
            .put("%Y", "yyyy")
            .put("%m", "MM")
            .put("%d", "dd")
            .put("%H", "HH")
            .put("%M", "mm")
            .put("%S", "ss")
            .put("%z", "z")
            .put("%T", "HH:mm:ss")
            .put("%b", "MMM")
            .put("%N", "SSS")
            .put("%6N", "SSS")
            .put("T", "'T'")
            .build();

    private interface TimestampColumnOption extends Task, TimestampParser.TimestampColumnOption {
    }

    private static String convertToJavaDateFormat(String rubyFormat) {
        String current = rubyFormat;

        for (Map.Entry<String, String> entry : formatMap.entrySet()) {
            current = current.replace(entry.getKey(), entry.getValue());
        }

        return current;
    }

    public static List<DateParser> create(GrokParserPlugin.PluginTask task) {
        switch (task.getTimestampParser().toLowerCase()) {
            case "ruby":
                TimestampParser[] ps = Timestamps.newTimestampColumnParsers(task, task.getColumns());
                return Arrays.stream(ps)
                        .map(parser -> (DateParser) (text) -> parser.parse(text))
                        .collect(Collectors.toList());
            case "epoch":
                return task.getColumns().getColumns().stream()
                        .map(x -> (DateParser) (text) -> Timestamp.ofEpochMilli(Long.parseLong(text)))
                        .collect(Collectors.toList());
            case "sdf":
            case "simpledateformat":
            default:
                SimpleDateFormat[] parsers = new SimpleDateFormat[task.getColumns().getColumnCount()];
                int i = 0;
                for (ColumnConfig column : task.getColumns().getColumns()) {
                    if (column.getType() instanceof TimestampType) {
                        TimestampColumnOption option = column.getOption().loadConfig(TimestampColumnOption.class);
                        String format = convertToJavaDateFormat(option.getFormat().or("yyyy-MM-dd HH:MM:ss.SSS z"));
                        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.ENGLISH);
                        sdf.setTimeZone(option.getTimeZone().or(DateTimeZone.UTC).toTimeZone());
                        parsers[i] = sdf;
                    }
                    i++;
                }
                return Arrays.stream(parsers).map(parser ->
                        (DateParser) (String date) -> {
                            try {
                                return Timestamp.ofEpochMilli(parser.parse(date).getTime());
                            } catch (ParseException e) {
                                throw new GrokRecordValidateException(e);
                            }
                        }).collect(Collectors.toList());
        }
    }
}
