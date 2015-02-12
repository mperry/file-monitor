package com.github.mperry;

import com.github.mperry.watch.FileMonitor;
import com.github.mperry.watch.Util;
import fj.P1;
import fj.P2;
import fj.data.Stream;
import org.junit.Test;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import static fj.data.Option.some;

/**
 * Created by mperry on 18/08/2014.
 */
public class TestStream {

    static Logger log = Util.logger(TestStream.class);

    @Test
    public void testStream() {

        try {
            P2<WatchService, WatchKey> p = FileMonitor.register(Util.EVENT_DIR, FileMonitor.ALL_EVENTS);
            P1<Stream<WatchEvent<Path>>> streamP1 = FileMonitor.streamEvents(p._1(), p._2());
            log.info("generating events...");
            Util.generateEventsAsync(100, some(500));
            log.info("getting stream...");
            Stream<WatchEvent<Path>> s = streamP1._1();
            s.take(5).foreachDoEffect(we -> Util.printWatchEvent(we));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

    }

    @Test
    public void testIterable() {
        try {
            P2<WatchService, WatchKey> p = FileMonitor.register(Util.EVENT_DIR, FileMonitor.ALL_EVENTS);
            P1<Stream<WatchEvent<Path>>> streamP1 = FileMonitor.streamEvents(p._1(), p._2());
            log.info("generating events...");
            Util.generateEventsAsync(100, some(500));
            log.info("getting stream...");
            Stream<WatchEvent<Path>> s = streamP1._1();
            for(WatchEvent<Path> we: s.take(5)) {
                Util.printWatchEvent(we);
            }
//            s.take(5).forEach(we -> Util.printWatchEvent(we));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }


    }

}
