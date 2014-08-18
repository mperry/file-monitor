package com.github.mperry;

import com.github.mperry.watch.Rx;
import com.github.mperry.watch.Util;
import fj.*;
import fj.data.Option;
import fj.data.Stream;
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

import static com.github.mperry.watch.Util.generateEventsAsync;
import static com.github.mperry.watch.Util.sleep;


/**
 * Created by mperry on 12/08/2014.
 */
public class TestInactive {

    static final Logger log = Util.logger(TestInactive.class);

    void println(Object o) {
		log.info(o.toString());
    }

    @Test
    public void testSimpleSubscribe() {
        Observable.from(1, 2, 3).subscribe(i -> println(i));
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
            log.info("createActive observable...");
			P2<WatchService, WatchKey> p = Rx.register(Rx.DEFAULT_DIR, Util.ALL_EVENTS);
			P1<Observable<WatchEvent<Path>>> o1 = Rx.observableInactive(p._1(), p._2());
            runObservable(o1._1(), Util.printWatchEvent());
        } catch (IOException e) {
			log.error(e.getMessage(), e);
        }
    }

    @Test
    public void testObservableOptions() {
        try {
            log.info("createActive observable...");
			P2<WatchService, WatchKey> p = Rx.register(Rx.DEFAULT_DIR, Util.ALL_EVENTS);
			P1<Observable<Option<WatchEvent<Path>>>> o1 = Rx.observableOptions(p._1(), p._2());
            runObservable(o1._1(), Util.printOptionWatchEvent());
        } catch (IOException e) {
			log.error(e.getMessage(), e);
        }
    }

    // WARNING: Direct subscribe does not work for full subscribe on another thread
//    @Test
    public void testFullSubscribe() {
        try {
            P2<WatchService, WatchKey> p2 = Rx.register(Util.EVENT_DIR, Util.ALL_EVENTS);
            Observable<WatchEvent<Path>> obs = Rx.observableInactive(p2._1(), p2._2())._1();
            println("subscribing...");
            Observable<WatchEvent<Path>> obs2 = obs.subscribeOn(Schedulers.io());
            Subscription s =  obs2.subscribe(we -> Util.printWatchEvent(we), t -> {
                log.info("Observable error...");
                log.error(t.getMessage(), t);
            }, () -> println("completed"));
            println("subscribed");
            // does not work
            generateEventsAsync(100, Option.some(500));
            log.info("Take 5...");
            obs2.take(5).toBlocking().forEach(we -> Util.printWatchEvent(we));
            println("sleeping");
            Thread.sleep(3000);
            println("awake again");
            s.unsubscribe();
            println("unsubscribed");
            p2._1().close();
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

    }


}
