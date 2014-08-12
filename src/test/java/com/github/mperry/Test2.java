package com.github.mperry;

import com.github.mperry.watch.Rx;
import com.github.mperry.watch.Util;
import fj.P2;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import static java.lang.Thread.sleep;

/**
 * Created by MarkPerry on 11/08/2014.
 */
public class Test2 {

	static Logger log = Util.logger(Test2.class);

	void println(Object o) {
		log.info(o.toString());
	}

	@Test
	public void test1() {
		Observable.from(1, 2, 3).subscribe(i -> println(i));
	}

	P2<WatchService, Observable<WatchEvent<Path>>> create() throws IOException {
		File dir = new File(".");
		println(String.format("monitoring dir: %s", dir.getAbsolutePath()));
		return Rx.create(dir, Util.ALL_EVENTS);
	}

//	@Test
	public void test2() throws IOException {
		P2<WatchService, Observable<WatchEvent<Path>>> p = create();

		p._2().take(5).forEach(we -> {
			println(String.format("Watch event, context: %s kind: %s", we.context(), we.kind()));
		});
		p._1().close();
	}

	void printWatchEvent(WatchEvent<Path> we) {
		println(String.format("Watch event, kind: %s, context: %s", we.kind(), we.context()));
	}

//	@Test
	public void test3() {
		try {
			P2<WatchService, Observable<WatchEvent<Path>>> p = create();
			println("subscribing...");
			Subscription s = p._2().subscribeOn(Schedulers.computation()).subscribe(we -> printWatchEvent(we), t -> {
				log.info("Observable error...");
				log.error(t.getMessage(), t);
			}, () -> println("completed"));
			println("subscribed");
			// does not work
			p._2().take(5).forEach(we -> printWatchEvent(we));
			println("sleeping");
			sleep(3000);
			println("awake again");
			s.unsubscribe();
			println("unsubscribed");
			p._1().close();
		} catch (InterruptedException e) {
			log.error(e.getMessage(), e);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}

	}

//	@Test
	public void test4() throws IOException, InterruptedException {
		File dir = new File(".");
		println(String.format("monitoring dir: %s", dir.getAbsolutePath()));
//		WatchService s = FileSystems.getDefault().newWatchService();
		WatchService s = Rx.register(dir, Util.ALL_EVENTS);
		Observable<WatchEvent<Path>> o = Rx.observable(s)._1();

		println("subscribing...");
		o.subscribe(we -> printWatchEvent(we), t -> println(t), () -> println("completed"));
		println("subscribed");


		// does not work
		o.take(5).forEach(we -> printWatchEvent(we));
		sleep(9000);

		s.close();
	}

}
