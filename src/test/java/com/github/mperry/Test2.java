package com.github.mperry;

import com.github.mperry.watch.Rx;
import com.github.mperry.watch.Util;
import fj.P2;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

/**
 * Created by MarkPerry on 11/08/2014.
 */
public class Test2 {

	static Logger log = LoggerFactory.getLogger(Test2.class);

	void println(Object o) {
//		System.out.println(o);
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
		println(String.format("Watch event, context: %s kind: %s", we.context(), we.kind()));
	}

	@Test
	public void test3() throws IOException {
		P2<WatchService, Observable<WatchEvent<Path>>> p = create();
		println("subscribing...");

		p._2().subscribeOn(Schedulers.computation()).subscribe(we -> printWatchEvent(we), t -> println(t), () -> println("completed"));
		println("subscribed");


		// does not work
		p._2().take(5);//.forEach(we -> printWatchEvent(we));
		p._1().close();
	}

//	@Test
	public void test4() throws IOException {
		File dir = new File(".");
		println(String.format("monitoring dir: %s", dir.getAbsolutePath()));
//		WatchService s = FileSystems.getDefault().newWatchService();
		WatchService s = Rx.register(dir, Util.ALL_EVENTS);
		Observable<WatchEvent<Path>> o = Rx.observable(s)._1();

		println("subscribing...");
		o.subscribe(we -> printWatchEvent(we), t -> println(t), () -> println("completed"));
		println("subscribed");


		// does not work
		o.take(5);//.forEach(we -> printWatchEvent(we));
		s.close();
	}

}
