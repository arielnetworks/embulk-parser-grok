package org.embulk.parser.grok;

import com.google.common.collect.ImmutableSet;
import org.embulk.spi.Column;
import org.embulk.spi.ColumnVisitor;
import org.embulk.spi.Exec;
import org.embulk.spi.PageBuilder;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

public class GrokColumnVisitor implements ColumnVisitor {

    private Map<String, Object> record;
    private PageBuilder pageBuilder;
    private final List<DateParser> timestampParsers;
    private final Logger logger = Exec.getLogger(GrokColumnVisitor.class.getName());

    private static final ImmutableSet<String> TRUE_STRINGS =
            ImmutableSet.of(
                    "true", "True", "TRUE",
                    "yes", "Yes", "YES",
                    "t", "T", "y", "Y",
                    "on", "On", "ON",
                    "1");

    public GrokColumnVisitor(Map<String, Object> record, PageBuilder pageBuilder, List<DateParser> timestampParsers) {
        this.record = record;
        this.pageBuilder = pageBuilder;
        this.timestampParsers = timestampParsers;
    }

    @Override
    public void booleanColumn(Column column) {
        if (record.get(column.getName()) == null) {
            pageBuilder.setNull(column);
        } else {
            pageBuilder.setBoolean(column, TRUE_STRINGS.contains(record.get(column.getName()).toString()));
        }
    }

    @Override
    public void longColumn(Column column) {
        Object longNum = record.get(column.getName());
        if (longNum == null || longNum.toString().equals("null")) {
            pageBuilder.setNull(column);
        } else {
            try {
                pageBuilder.setLong(column, Long.parseLong(longNum.toString()));
            } catch (NumberFormatException e) {
                logger.error("This column is not Long:" + longNum.toString(), e);
                throw new GrokRecordValidateException(e);
            }
        }
    }

    @Override
    public void doubleColumn(Column column) {
        Object dbl = record.get(column.getName());
        if (dbl == null) {
            pageBuilder.setNull(column);
        } else {
            try {
                pageBuilder.setDouble(column, Double.parseDouble(dbl.toString()));
            } catch (NumberFormatException e) {
                logger.error("This column is not Double:" + dbl.toString(), e);
                throw new GrokRecordValidateException(e);
            }
        }
    }

    @Override
    public void stringColumn(Column column) {
        if (record.get(column.getName()) == null)
            pageBuilder.setNull(column);
        else {
            pageBuilder.setString(column, record.get(column.getName()).toString());
        }
    }

    @Override
    public void jsonColumn(Column column) {
        throw new UnsupportedOperationException("This plugin doesn't support json type. Please try to upgrade version of the plugin using 'embulk gem update' command. If the latest version still doesn't support json type, please contact plugin developers, or change configuration of input plugin not to use json type.");
    }

    @Override
    public void timestampColumn(Column column) {
        Object time = record.get(column.getName());

        if (time == null) {
            pageBuilder.setNull(column);
        } else {
            String timeString = time.toString();
            try {
                pageBuilder.setTimestamp(column, timestampParsers.get(column.getIndex()).parse(timeString));
            } catch (RuntimeException e) {
                logger.error("TimestampParseError:" + column.getName() + ", timeString:" + timeString + ", getIndex:" + column.getIndex(), e);
                throw new GrokRecordValidateException(e);
            }
        }
    }
}
