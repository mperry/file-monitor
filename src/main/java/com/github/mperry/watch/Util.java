package com.github.mperry.watch;

import fj.F;
import fj.P2;
import fj.Unit;
import fj.data.List;
import fj.data.Option;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.HashMap;
import java.util.Map;

import static fj.Unit.unit;
import static fj.data.Option.none;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

/**
 * Created by MarkPerry on 11/08/2014.
 */
public class Util {

    public static final Logger log = logger(Util.class);

	public static final List<WatchEvent.Kind<Path>> ALL_EVENTS = List.list(ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

    public static <K, V> Map<K, V> create(P2<K, V>... args) {
        Map<K, V> map = new HashMap<>();
        for (P2<K, V> p: args) {
            map.put(p._1(), p._2());
        }
        return map;
    }

    public static Logger logger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }

    public static void printThread() {
        log.info("Thread id: " + Thread.currentThread().getId());
    }

    public static long threadId() {
        return Thread.currentThread().getId();
    }


    public static void generateEventsAsync(int n, Option<Integer> option) {
        Runnable r = () -> {
//            option.forEach(i -> sleep(i));
            generateEvents(n, option);
        };
        new Thread(r).start();
    }

    public static void sleep(int n) {
        try {
            Thread.sleep(n);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }


    public static void generateEvents(int n, Option<Integer> optionSleep) {
        for (int i = 0; i < n; i++) {
            optionSleep.forEach(t -> sleep(t));
            createEvent();
        }
    }

    public static void generateEvents(int n) {
        generateEvents(n, none());
    }

    public static final String EVENT_DIR_PATH = "etc/events";
    public static final File EVENT_DIR = new File(EVENT_DIR_PATH);
    public static final File EVENT_FILE = new File(EVENT_DIR_PATH, "event.log");

    public static void createEvent() {
        try {
            append(EVENT_FILE, "event\n");
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public static void append(File f, String text) throws IOException {
        FileUtils.writeStringToFile(f, text, true);
    }


    public static F<WatchEvent<Path>, Unit> printWatchEvent() {
        return we -> {
            printWatchEvent(we);
            return unit();
        };
    }

    public static F<Option<WatchEvent<Path>>, Unit> printOptionWatchEvent() {
        return o -> {
            printOWE(o);
            return unit();
        };
    }

    public static void printWatchEvent(WatchEvent<Path> we) {
        log.info(String.format("thread: %d, kind: %s, context: %s", Util.threadId(), we.kind(), we.context()));
    }

    public static void printOWE(Option<WatchEvent<Path>> option) {
        if (option.isNone()) {
            log.info("Option is none");
        }
        option.map(we -> {
            printWatchEvent(we);
            return we;
        });
    }

}
