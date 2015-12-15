package org.embulk.parser.grok;

import java.io.Closeable;
import java.util.Map;

import oi.thekraken.grok.api.Grok;
import oi.thekraken.grok.api.Match;
import oi.thekraken.grok.api.exception.GrokException;

import org.embulk.spi.Exec;
import org.embulk.spi.util.LineDecoder;
import org.slf4j.Logger;

public class GrokRecordIterator implements Closeable {

    private LineDecoder decoder;
    private Grok grok = null;

    private String currentLine = null;
    private long currentLineNumber = 0;

    private final Logger logger = Exec.getLogger(GrokRecordIterator.class.getName());

    public GrokRecordIterator(LineDecoder decoder, GrokParserPlugin.PluginTask task) {
        this.decoder = decoder;
        try {
            this.grok = new Grok();
            for (String file : task.getGrokPatternFiles()) {
                this.grok.addPatternFromFile(file);
            }
            this.grok.compile(task.getGrokPattern());
        } catch (GrokException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean nextFile() {
        currentLine = null;
        currentLineNumber = 0;
        return decoder.nextFile();
    }

    public boolean nextLine() {
        currentLine = decoder.poll();
        currentLineNumber++;
        return currentLine != null;
    }

    @Override
    public void close() {
        decoder.close();
    }

    public Map<String, Object> getCurrentRecord() {
        Match gm = grok.match(currentLine);
        gm.captures();
        if (gm.isNull()) {
            throw new GrokRecordValidateException("Couldn't parse line");
        }
        return gm.toMap();
    }

    public String getCurrentLine() {
        return currentLine;
    }

    public long getCurrentLineNumber() {
        return currentLineNumber;
    }
}

