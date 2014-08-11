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

	public static <C, T extends C> C[] toArray(java.util.List<T> list, Class<C> componentType) {
		@SuppressWarnings("unchecked")
		C[] array = (C[]) Array.newInstance(componentType, list.size());
		return list.toArray(array);
	}

	static void watch(File dir, List<WatchEvent.Kind<Path>> list) {
		P1<Stream<WatchEvent<Path>>> p = stream(dir, list);
		Stream<WatchEvent<Path>> s = p._1();
		List<WatchEvent<Path>> eventList = s.take(5).toList();
		log.info("size: " + eventList.length() + " " + eventList.map(we -> String.format("Service event, kind: %s path: %s", we.kind(), we.context())).toString());
	}

	static List<WatchEvent.Kind<Path>> ALL_EVENTS = List.list(ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

	public static P1<Stream<WatchEvent<Path>>> stream(File dir, List<WatchEvent.Kind<Path>> list) {

		try {
			WatchService s = getDefault().newWatchService();
			return P.<Stream<WatchEvent<Path>>>lazy(u -> {
				try {
					Path dirPath = dir.toPath();
					for (WatchEvent.Kind<Path> k : list) {
						dirPath.register(s, k);
					}
//					P1<Stream<WatchEvent<Path>>> stream = stream(s);
					return stream(s)._1();
				} catch (IOException e) {
					log.error(e.getMessage(), e);
				}
				return Stream.<WatchEvent<Path>>nil();
			});

		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
		return null;

	}

	public static P1<Stream<WatchEvent<Path>>> stream(final WatchService s) {
		return P.lazy(u -> {
			Stream<WatchEvent<Path>> result = Stream.nil();
			try {
				log.info("Polling WatchService events...");
				WatchKey key = s.take();
				log.info("Finished polling.");

				for (WatchEvent<?> event : key.pollEvents()) {
					WatchEvent<Path> we = (WatchEvent<Path>) event;
					result = result.snoc(we);
				}

				boolean b = key.reset();
				if (!b) {
					log.error(String.format("Key %s is invalid"), key);

				}
//				return Stream.stream(key.pollEvents().toArray(new WatchEvent[0])).append(stream(s));
				result = result.append(stream(s));

			} catch (InterruptedException e) {
				log.error(e.getMessage(), e);
			}
			return result;
		});
	}


	public static Unit watch2(File dir, Map<WatchEvent.Kind<Path>, F<Path, Unit>> m) {
//		WatchService s = null;
		try (WatchService s = getDefault().newWatchService()) {
//			s = FileSystems.getDefault().newWatchService();
			Path dirPath = dir.toPath();
			for (WatchEvent.Kind<Path> k: m.keySet()) {
				dirPath.register(s, k);
			}
			while (true) {
				WatchKey key = s.take();
				for (WatchEvent<?> event: key.pollEvents()) {
					WatchEvent<Path> we = (WatchEvent<Path>) event;
					Path p1 = we.context();
					log.info(String.format("Service event, kind: %s path: %s", we.kind(), p1));
					Option<F<Path, Unit>> o = Option.fromNull(m.get(we.kind()));
					o.map(f -> f.f(p1));
				}
				key.reset();
			}
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		} catch (InterruptedException e) {
			log.error(e.getMessage(), e);
		} finally {
//			try {
//				if (s != null) {
//					s.close();
//				}
//			} catch (IOException e) {
//				log.error(e.getMessage(), e);
//			}
		}
		return unit();
	}

	public static Unit watch() {
		watch(DEFAULT_DIR, ALL_EVENTS);
		return unit();
	}


	public static Unit watch1() {
		WatchService s = null;
		try {
			s = getDefault().newWatchService();
			Path p = getDefault().getPath(DEFAULT_PATH);
			p.register(s, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

			int n = 10;
			for (int i = 0; i < n; i++) {
//				log.info("Hello world");
//				Thread.sleep(1000);
				WatchKey key = s.take();
//				WatchKey key = null;
				for (WatchEvent<?> event: key.pollEvents()) {
					WatchEvent.Kind<?> kind = event.kind();
					Path p1 = ((WatchEvent<Path>) event).context();
					log.info(String.format("Service event, kind: %s path: %s", kind, p1));
					if (kind == ENTRY_CREATE) {

					} else {

					}
				}
				key.reset();
			}


		} catch (IOException e) {
			log.error(e.getMessage(), e);

		} catch (InterruptedException e) {
			log.error(e.getMessage(), e);

		} finally {

			try {
				s.close();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
//				e.printStackTrace();
			}
		}
		return unit();

	}


}
