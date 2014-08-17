package com.github.mperry;

import com.github.mperry.watch.Rx;
import com.github.mperry.watch.Util;
import fj.F;
import fj.P1;
import fj.P2;
import fj.Unit;
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

import static fj.Unit.unit;


/**
 * Created by mperry on 12/08/2014.
 */
public class Test3 {

    static final Logger log = Util.logger(Test3.class);

    void println(Object o) {
		log.info(o.toString());
    }

    @Test
    public void testStreamIntegers() {
        Util.printThread();
        Observable<Integer> o = Observable.from(Stream.range(1));
        Observable<Integer> o2 = o.subscribeOn(Schedulers.computation());
        Subscription s = o2.subscribe(i -> {
            println(String.format("thread: %s, num: " + i, Thread.currentThread().getId()));
        });
        println("subscribed.");
        println("sleeping...");
        sleep(100);
        s.unsubscribe();
        println("done");
    }

    public void sleep(int n) {
        try {
            Thread.sleep(n);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }

    public <A> void runObservable(Observable<A> obs, F<A, Unit> print) {
        log.info("set subscribe on...");
        Observable<A> o2  = obs.subscribeOn(Schedulers.io());
        log.info("subscribing...");
        Subscription s = o2.subscribe((A a) -> {
            print.f(a);
        });
        log.info("subscribed");
        log.info("sleeping...");
        sleep(2000);
        log.info("done sleeping");
        s.unsubscribe();
        log.info("unsubscribed");
    }

    @Test
    public void testObservableInactive() {
        try {
            log.info("create observable...");
			P2<WatchService, WatchKey> p = Rx.register(Rx.DEFAULT_DIR, Util.ALL_EVENTS);
			P1<Observable<WatchEvent<Path>>> o1 = Rx.observableInactive(p._1(), p._2());
            runObservable(o1._1(), printWatchEvent());
        } catch (IOException e) {
			log.error(e.getMessage(), e);
        }
    }

    @Test
    public void testObservableOptions() {
        try {
            log.info("create observable...");
			P2<WatchService, WatchKey> p = Rx.register(Rx.DEFAULT_DIR, Util.ALL_EVENTS);
			P1<Observable<Option<WatchEvent<Path>>>> o1 = Rx.observableOptions(p._1(), p._2());
            runObservable(o1._1(), printOptionWatchEvent());
        } catch (IOException e) {
			log.error(e.getMessage(), e);
        }
    }

    F<WatchEvent<Path>, Unit> printWatchEvent() {
        return we -> {
            printWatchEvent(we);
            return unit();
        };
    }

    F<Option<WatchEvent<Path>>, Unit> printOptionWatchEvent() {
        return o -> {
            printOWE(o);
            return unit();
        };
    }

    void printWatchEvent(WatchEvent<Path> we) {
        println(String.format("thread: %d, kind: %s, context: %s", Util.threadId(), we.kind(), we.context()));
    }

	void printOWE(Option<WatchEvent<Path>> option) {
		if (option.isNone()) {
			println("Option is none");
		}
		option.map(we -> {
            printWatchEvent(we);
            return we;
		});
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


}
