package com.github.mperry.rx;

import com.github.mperry.watch.Watcher;
import fj.P;
import fj.P2;
import fj.data.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Iterator;

import static java.lang.String.format;
//import java.util.List;

/**
 * Created by MarkPerry on 11/08/2014.
 */
public class Watcher2 {

	static Logger log = LoggerFactory.getLogger(Watcher2.class);

	public static WatchService register(File dir, List<WatchEvent.Kind<Path>> list) throws IOException {

		WatchService s = FileSystems.getDefault().newWatchService();
		Path p = dir.toPath();
		for (WatchEvent.Kind<Path> k: list) {
			p.register(s, k);
		}
		return s;

	}

	public static P2<WatchService, Observable<WatchEvent<Path>>> create(File dir, List<WatchEvent.Kind<Path>> list) throws IOException {
		WatchService s = FileSystems.getDefault().newWatchService();
		Path p = dir.toPath();
		for (WatchEvent.Kind<Path> k: list) {
			p.register(s, k);
		}
		return P.p(s, create(s));

	}

	public static Observable<WatchEvent<Path>> create(final WatchService s) {
		Observable.OnSubscribe<WatchEvent<Path>> os = sub -> {
			try {
				while (true) {
					WatchKey k = s.take();

					for (WatchEvent<?> e: k.pollEvents()) {
						WatchEvent<Path> we = (WatchEvent<Path>) e;
						if (sub.isUnsubscribed()) {
							return;
						}
						sub.onNext(we);
					}
					boolean b = k.reset();
					if (!b) {
						log.error(format("Key %s is invalid"), k);

					}
				}
			} catch (InterruptedException e) {
				sub.onError(e);
//				e.printStackTrace();
			}
		};
		return Observable.create(os);

//		return null;
	}

	public static Observable<WatchEvent<Path>> create2(final WatchService s) {
		return Observable.from(Watcher.stream(s)._1());
	}

}

