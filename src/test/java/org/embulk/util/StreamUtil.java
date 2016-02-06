package org.embulk.util;

import org.jruby.ir.Tuple;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StreamUtil {
    public static <T, U> Stream<Tuple<T, U>> zip(Stream<T> first, Stream<U> second) {
        final Iterator<T> firstIt = first.iterator();
        final Iterator<U> secondIt = second.iterator();
        Iterator<Tuple<T, U>> iterator = new Iterator<Tuple<T, U>>() {
            @Override
            public boolean hasNext() {
                return firstIt.hasNext() && secondIt.hasNext();
            }

            @Override
            public Tuple<T, U> next() {
                return new Tuple<>(firstIt.next(), secondIt.next());
            }
        };

        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.NONNULL | Spliterator.ORDERED), false);
    }

    protected StreamUtil() {
        // prevents calls from subclass
        throw new UnsupportedOperationException();
    }
}
