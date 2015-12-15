package org.embulk.parser.grok;


import com.google.common.base.Optional;
import org.embulk.config.*;
import org.embulk.spi.*;
import org.embulk.spi.time.TimestampParser;
import org.embulk.spi.util.LineDecoder;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

public class GrokParserPlugin implements ParserPlugin {

    public final Logger logger = Exec.getLogger(GrokParserPlugin.class.getName());

    public interface PluginTask
            extends Task, LineDecoder.DecoderTask, TimestampParser.Task {

        @Config("grok_pattern")
        String getGrokPattern();

        @Config("first_line_pattern")
        @ConfigDefault("null")
        Optional<String> getFirstLinePattern();

        @Config("grok_pattern_files")
        List<String> getGrokPatternFiles();

        @Config("timestamp_parser")
        @ConfigDefault("\"ruby\"")
        String getTimestampParser();

        @Config("columns")
        SchemaConfig getColumns();

        @Config("stop_on_invalid_record")
        @ConfigDefault("false")
        boolean getStopOnInvalidRecord();
    }

    @Override
    public void transaction(ConfigSource config, ParserPlugin.Control control) {
        PluginTask task = config.loadConfig(PluginTask.class);
        Schema schema = task.getColumns().toSchema();

        control.run(task.dump(), schema);
    }

    @Override
    public void run(TaskSource taskSource, Schema schema, FileInput input, PageOutput output) {
        GrokParserPlugin.PluginTask task = taskSource.loadTask(GrokParserPlugin.PluginTask.class);

        LineDecoder decoder;
        if (task.getFirstLinePattern().isPresent()) {
            decoder = new MultipleLineDecoder(input, task);
        } else {
            decoder = new LineDecoder(input, task);
        }

        try (GrokRecordIterator iterator = new GrokRecordIterator(decoder, task)) {
            final List<DateParser> timestampParsers = TimestampParserFactory.create(task);
            PageBuilder pageBuilder = new PageBuilder(Exec.getBufferAllocator(), schema, output);
            while (true) {
                if (!iterator.nextFile()) {
                    break;
                }
                while (true) {
                    if (!iterator.nextLine()) {
                        break;
                    }

                    try {
                        Map<String, Object> record = iterator.getCurrentRecord();
                        if (record.keySet().size() != 0) {
                            schema.visitColumns(new GrokColumnVisitor(record, pageBuilder, timestampParsers));
                            pageBuilder.addRecord();
                        }
                    } catch (GrokRecordValidateException e) {
                        String skippedLine = iterator.getCurrentLine();
                        long lineNumber = iterator.getCurrentLineNumber();
                        if (task.getStopOnInvalidRecord()) {
                            throw new DataException(String.format("Invalid record at line %d: %s", lineNumber, skippedLine), e);
                        }
                        logger.warn(String.format("Skipped line %d (%s): %s", lineNumber, e.getMessage(), skippedLine));
                    }
                }
            }
            pageBuilder.finish();
        }
    }

}
