package com.github.mperry.watch;

import fj.*;
import fj.data.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.Class;
import java.lang.reflect.Array;
import java.nio.file.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static fj.Unit.unit;
import static java.nio.file.FileSystems.*;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

/**
 * Created by mperry on 11/08/2014.
 */
public class Watcher {

	public static final String DEFAULT_PATH = "/Users/mperry/repositories/file-monitoring";
	static final File DEFAULT_DIR = new File(DEFAULT_PATH);

	static Logger log = LoggerFactory.getLogger(Watcher.class);

	public static <K, V> Map<K, V> create(P2<K, V>... args) {
		Map m = new HashMap<K, V>();
		for (P2<K, V> p: args) {
			m.put(p._1(), p._2());
		}
		return m;
	}

	static void watch(File dir, List<WatchEvent.Kind<Path>> list) {
		P2<WatchService, P1<Stream<WatchEvent<Path>>>> p = stream(dir, list);

		Stream<WatchEvent<Path>> s = p._2()._1();
		List<WatchEvent<Path>> eventList = s.take(5).toList();
		log.info("size: " + eventList.length() + " " + eventList.map(we -> String.format("Service event, kind: %s path: %s", we.kind(), we.context())).toString());
	}

	static List<WatchEvent.Kind<Path>> ALL_EVENTS = List.list(ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

	public static P2<WatchService, P1<Stream<WatchEvent<Path>>>> stream(File dir, List<WatchEvent.Kind<Path>> list) {

		try {
			WatchService s = getDefault().newWatchService();
			return P.p(s, P.lazy(u -> {
				try {
					Path dirPath = dir.toPath();
					for (WatchEvent.Kind<Path> k : list) {
						dirPath.register(s, k);
					}
					return stream(s)._1();
				} catch (IOException e) {
					log.error(e.getMessage(), e);
				}
				return Stream.<WatchEvent<Path>>nil();
			}));

		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	public static P1<Stream<WatchEvent<Path>>> stream(final WatchService s) {
		return P.lazy(u -> {
			Stream<WatchEvent<Path>> empty = Stream.nil();
//			try {
				log.info("Polling WatchService events...");
//				Option<WatchKey> optKey = Option.fromNull(s.take());
				Option<WatchKey> optKey = Option.fromNull(s.poll());
				log.info("Finished polling.");
				return optKey.map(key -> {
					Stream<WatchEvent<Path>> result = empty;
					for (WatchEvent<?> event : key.pollEvents()) {
						WatchEvent<Path> we = (WatchEvent<Path>) event;
						result = result.snoc(we);
					}
					boolean b = key.reset();
					if (!b) {
						log.error(String.format("Key %s is invalid"), key);
					}
					return result.append(stream(s));
//					return result;
				}).orSome(empty);
//				return Stream.stream(key.pollEvents().toArray(new WatchEvent[0])).append(stream(s));

//			} catch (InterruptedException e) {
//				log.info(e.getMessage(), e);
//				return Stream.<WatchEvent<Path>>nil();
//			}
//			return empty;
		});
	}

	public static Unit watch() {
		watch(DEFAULT_DIR, ALL_EVENTS);
		return unit();
	}



}
