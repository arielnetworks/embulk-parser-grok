package org.embulk.parser.grok;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import oi.thekraken.grok.api.exception.GrokException;
import org.embulk.config.*;
import org.embulk.spi.Buffer;
import org.embulk.spi.Exec;
import org.embulk.spi.GuessPlugin;
import org.embulk.spi.util.LineDecoder;
import org.embulk.spi.util.ListFileInput;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GrokGuessPlugin implements GuessPlugin {
    public final Logger logger = Exec.getLogger(GrokGuessPlugin.class.getName());

    public interface PluginTask
            extends Task, LineDecoder.DecoderTask {

        @Config("grok_pattern_files")
        List<String> getGrokPatternFiles();

        @Config("guess_patterns")
        @ConfigDefault("[]")
        List<String> getGuessPatterns();
    }

    @Override
    public ConfigDiff guess(ConfigSource config, Buffer sample) {

        GrokGuessPlugin.PluginTask task = config.getNested("parser").loadConfig(GrokGuessPlugin.PluginTask.class);

        LineDecoder.DecoderTask decoderTask = config.loadConfig(LineDecoder.DecoderTask.class);
        LineDecoder decoder = new LineDecoder(new ListFileInput(ImmutableList.of(ImmutableList.of((sample)))), decoderTask);

        List<String> sampleLines = new ArrayList<>();
        while (true) {
            if (!decoder.nextFile()) {
                break;
            }
            while (true) {
                String line = decoder.poll();
                if (line == null) {
                    break;
                }
                sampleLines.add(line);
            }
        }

        GrokGuesser guesser = new GrokGuesser(
                task.getGuessPatterns(),
                task.getGrokPatternFiles()
        );
        try {
            String pattern = guesser.guessPattern(sampleLines);
            List<Map<String, Object>> columns = guesser.guessColumns(sampleLines, pattern);
            return Exec.newConfigDiff().set(
                    "parser", ImmutableMap.of("grok_pattern", pattern, "columns", columns));
        } catch (GrokException e) {
            return Exec.newConfigDiff();
        }

    }
}
