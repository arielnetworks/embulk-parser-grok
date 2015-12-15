package org.embulk.parser.grok;

import com.google.common.base.Strings;
import oi.thekraken.grok.api.Grok;
import oi.thekraken.grok.api.Match;
import oi.thekraken.grok.api.exception.GrokException;
import org.embulk.spi.FileInput;
import org.embulk.spi.util.LineDecoder;

public class MultipleLineDecoder extends LineDecoder {

    private StringBuilder buildLines;
    private Grok grok = null;

    public MultipleLineDecoder(FileInput in, GrokParserPlugin.PluginTask task) {
        super(in, task);

        try {
            this.grok = new Grok();
            for (String file : task.getGrokPatternFiles()) {
                this.grok.addPatternFromFile(file);
            }
            this.grok.compile(task.getFirstLinePattern().get());
        } catch (GrokException e) {
            throw new RuntimeException(e);
        }
        this.buildLines = new StringBuilder();
    }

    @Override
    public boolean nextFile() {
        return super.nextFile();
    }

    @Override
    public void close() {
        super.close();
    }

    @Override
    public String poll() {
        String currentLine;
        while ((currentLine = super.poll()) != null) {
            Match gm = grok.match(currentLine);
            gm.captures();

            if (!gm.isNull()) {
                String fullLog = this.buildLines.toString();
                if (!Strings.isNullOrEmpty(fullLog)) {
                    this.buildLines.setLength(0);
                    this.buildLines.append(currentLine).append(System.lineSeparator());
                    return fullLog;
                }
            }
            this.buildLines.append(currentLine).append(System.lineSeparator());
        }

        String fullLog = this.buildLines.toString();
        if (!Strings.isNullOrEmpty(fullLog)) {
            this.buildLines.setLength(0);
            return fullLog;
        }

        return null;
    }
}
