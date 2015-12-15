package org.embulk.parser.grok;

import org.embulk.spi.time.Timestamp;

@FunctionalInterface
public interface DateParser {
    Timestamp parse(String date);
}
