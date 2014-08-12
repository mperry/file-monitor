package com.github.mperry.watch;

import fj.P2;
import fj.data.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

/**
 * Created by MarkPerry on 11/08/2014.
 */
public class Util {

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


}
