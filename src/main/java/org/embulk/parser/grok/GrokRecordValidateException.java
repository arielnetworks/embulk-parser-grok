package org.embulk.parser.grok;

public class GrokRecordValidateException extends RuntimeException {
    GrokRecordValidateException(String message) {
        super(message);
    }

    GrokRecordValidateException(Throwable cause) {
        super(cause);
    }
}
