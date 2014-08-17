package com.github.mperry;

import com.github.mperry.watch.Rx;
import com.github.mperry.watch.Util;
import fj.P1;
import fj.P2;
import fj.data.Option;
import fj.data.Stream;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.slf4j.Logger;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;


/**
 * Created by mperry on 12/08/2014.
 */
public class Test3 {

    static final Logger log = Util.logger(Test3.class);

    void println(Object o) {
		log.info(o.toString());
//        System.out.println(o);
    }

    @Test
    public void test1() {
        println("Thread: " + Thread.currentThread().getId());
        Observable<Integer> o = Observable.from(Stream.range(1));
        println("subscribing...");
        Observable<Integer> o2 = o.subscribeOn(Schedulers.computation());
        Subscription s = o2.subscribe(i -> {
            println(String.format("thread: %s, num: " + i, Thread.currentThread().getId()));
        });
        println("subscribed.");
//        s.unsubscribe();
        println("sleeping...");
        sleep(100);
        s.unsubscribe();
        println("done");

    }

    public void sleep(int n) {
        try {
            Thread.sleep(n);
        } catch (InterruptedException e) {
//            e.printStackTrace();
            log.error(e.getMessage(), e);
        }
//        println("done");
    }

	// WARNING: does not end without input from user
    @Test
    public void test2() {
        try {
            log.info("create observable...");
			P2<WatchService, WatchKey> p = Rx.register(Rx.DEFAULT_DIR, Util.ALL_EVENTS);
			P1<Observable<WatchEvent<Path>>> o1 = Rx.observableInactive(p._1(), p._2());
            log.info("set subscribe on...");
            Observable<WatchEvent<Path>> o2  = o1._1().subscribeOn(Schedulers.io());
            log.info("subscribing...");
            Subscription s = o2.subscribe(we -> {
                println(String.format("kind: %s, context: %s", we.kind(), we.context()));
            });
            log.info("subscribed");
            log.info("sleeping...");
            Thread.sleep(5000);
            log.info("done sleeping");
            s.unsubscribe();
            log.info("unsubscribed");
//			Thread.sleep(10000);
        } catch (IOException e) {
			log.error(e.getMessage(), e);
//            e.printStackTrace();
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }

    }



    @Test
    public void unsubscribeObservableOption() {
        try {
            log.info("create observable...");
			P2<WatchService, WatchKey> p = Rx.register(Rx.DEFAULT_DIR, Util.ALL_EVENTS);
			P1<Observable<Option<WatchEvent<Path>>>> o1 = Rx.observableOptions(p._1(), p._2());
            log.info("set subscribe on...");
            Observable<Option<WatchEvent<Path>>> o2  = o1._1().subscribeOn(Schedulers.io());
            log.info("subscribing...");
            Subscription s = o2.subscribe(option -> printOWE(option));
            log.info("subscribed");
            log.info("sleeping...");
            sleep(5000);
            log.info("done sleeping");
            s.unsubscribe();
            log.info("unsubscribed");
        } catch (IOException e) {
			log.error(e.getMessage(), e);
//            e.printStackTrace();
//        } catch (InterruptedException e) {
//            log.error(e.getMessage(), e);
        }

    }

	void printOWE(Option<WatchEvent<Path>> option) {
		if (option.isNone()) {
			println("Option is none");
		}
		option.map(we -> {
			println(String.format("thread: %d, kind: %s, context: %s", Util.threadId(), we.kind(), we.context()));
			return we;
		});
	}

	@Test
	public void takeObservableOption() {
		try {
			log.info("create observable...");
			P2<WatchService, WatchKey> p = Rx.register(Rx.DEFAULT_DIR, Util.ALL_EVENTS);
			P1<Observable<Option<WatchEvent<Path>>>> po = Rx.observableOptions(p._1(), p._2());
			log.info("set subscribe on...");
			Observable<Option<WatchEvent<Path>>> obs = po._1().subscribeOn(Schedulers.io());
//			Observable<Option<WatchEvent<Path>>> obs = po._1();
			log.info("subscribing...");
			Subscription s = obs.subscribe(option -> printOWE(option));
			log.info("subscribed");
			generateEvents(10);
			Observable<Option<WatchEvent<Path>>> obs2 = obs.take(3);
			println("about to print count...");
			obs2.count().take(1).forEach(i-> println("count2: " + i));
			sleep(2000);
			log.info("sleeping...");
//			sleep(8000);
			log.info("done sleeping");
			s.unsubscribe();
			log.info("unsubscribed");
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}

	}

	@Test
	public void testGenerateEvents() {
		generateEvents(3);

	}

	void generateEventsAsync(int n) {
		Runnable r = () -> {
			generateEvents(n);
		};
		r.run();
	}

	void generateEvents(int n) {
		for (int i = 0; i < n; i++) {
			createEvent();
		}
	}

	void createEvent() {
		try {
			append(new File("event.log"), "event\n");
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}

	void append(File f, String text) throws IOException {
		FileUtils.writeStringToFile(f, text, true);

	}

	@Test
	public void testSimple() {
		log.info("Logging test simple");
	}

}
