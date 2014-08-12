package com.github.mperry.watch;

import fj.*;
import fj.data.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import static fj.Unit.unit;
import static java.nio.file.FileSystems.*;

/**
 * Created by mperry on 11/08/2014.
 */
public class Watcher {


	static Logger log = LoggerFactory.getLogger(Watcher.class);


	static void watch(File dir, List<WatchEvent.Kind<Path>> list) {
//		P2<WatchService, P1<Stream<WatchEvent<Path>>>> p = stream(dir, list);
//		Stream<WatchEvent<Path>> s = p._2()._1();
//		List<WatchEvent<Path>> eventList = s.take(5).toList();
//		log.info("size: " + eventList.length() + " " + eventList.map(we -> String.format("Service event, kind: %s path: %s", we.kind(), we.context())).toString());
	}

//	static List<WatchEvent.Kind<Path>> ALL_EVENTS = List.list(ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);


	public static Unit watch() {
		watch(Rx.DEFAULT_DIR, Util.ALL_EVENTS);
		return unit();
	}




}
