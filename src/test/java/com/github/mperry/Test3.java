package com.github.mperry;

import com.github.mperry.watch.Rx;
import com.github.mperry.watch.Util;
import fj.P1;
import fj.data.Option;
import fj.data.Stream;
import org.junit.Test;
import org.slf4j.Logger;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;


/**
 * Created by mperry on 12/08/2014.
 */
public class Test3 {

    static final Logger log = Util.logger(Test3.class);

    void println(Object o) {
        System.out.println(o);
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

//    @Test
    // WARNING: does not end without input from user
    public void test2() {
        try {
            log.info("create observable...");
            P1<Observable<WatchEvent<Path>>> o1 = Rx.observable(Rx.register(Rx.DEFAULT_DIR, Util.ALL_EVENTS));
            log.info("set subscribe on...");
            Observable<WatchEvent<Path>> o2  = o1._1().subscribeOn(Schedulers.io());
            log.info("subscribing...");
            Subscription s = o2.subscribe(we -> {
                println(String.format("context: %s, kind: %s", we.context(), we.kind()));
            });
            log.info("subscribed");
            log.info("sleeping...");
            Thread.sleep(5000);
            log.info("done sleeping");
            s.unsubscribe();
            log.info("unsubscribed");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }

    }


    @Test
    public void test3() {
        try {
            log.info("create observable...");
            P1<Observable<Option<WatchEvent<Path>>>> o1 = Rx.observableOpt(Rx.register(Rx.DEFAULT_DIR, Util.ALL_EVENTS));
            log.info("set subscribe on...");
            Observable<Option<WatchEvent<Path>>> o2  = o1._1().filter(opt -> opt.isSome()).subscribeOn(Schedulers.io());
            log.info("subscribing...");
            Subscription s = o2.subscribe(option -> {
                if (option.isNone()) {
                    println("Option is none");
                }
                option.map(we -> {
                    println(String.format("context: %s, kind: %s", we.context(), we.kind()));
                    return we;
                });
            });
            log.info("subscribed");
            log.info("sleeping...");
            sleep(2000);
//            Thread.sleep(5000);
            log.info("done sleeping");
//            s.unsubscribe();
            log.info("unsubscribed");
        } catch (IOException e) {
            e.printStackTrace();
//        } catch (InterruptedException e) {
//            log.error(e.getMessage(), e);
        }

    }

}
