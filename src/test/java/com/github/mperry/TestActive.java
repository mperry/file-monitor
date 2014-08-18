package com.github.mperry;

import com.github.mperry.watch.Rx;
import com.github.mperry.watch.Util;
import fj.F;
import fj.P2;
import fj.P3;
import fj.Unit;
import org.junit.Test;
import org.slf4j.Logger;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import static com.github.mperry.watch.Util.generateEvents;
import static fj.data.Option.some;
import static java.lang.String.format;
import static java.lang.Thread.sleep;

/**
 * Created by MarkPerry on 11/08/2014.
 */
public class TestActive {

	static final Logger log = Util.logger(TestActive.class);

	void println(Object o) {
		log.info(o.toString());
	}



	P3<WatchService, WatchKey, Observable<WatchEvent<Path>>> createActive() throws IOException {
		File dir = new File(".");
		println(format("monitoring dir: %s", dir.getAbsolutePath()));
		return Rx.createDirect(dir, Util.ALL_EVENTS);
	}


    public <A> void createOnNewThread(F<Observable<WatchEvent<Path>>, Unit> f) {
        Runnable r = () -> {
            try {
                Util.printThread();
                f.f(createActive()._3());
                log.info("called func");

            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        };
        new Thread(r).start();
//        r.run();
    }

    public <A> void createOnNewTread() {
        createOnNewThread(o -> Unit.unit());
    }


    @Test
	public void testCreateActive() throws IOException {
        Util.printThread();
		createOnNewThread(o -> {
            log.info(format("Receiving on id: %s", Util.threadId()));
            o.take(5).forEach(we -> Util.printWatchEvent(we));
            return Unit.unit();
        });
        Util.sleep(1000);
        generateEvents(10, some(500));
        Util.sleep(1000);
	}

	void printWatchEvent(WatchEvent<Path> we) {
		println(format("Watch event, kind: %s, context: %s", we.kind(), we.context()));
	}

//	@Test
	public void test3() {
		try {
			P3<WatchService, WatchKey, Observable<WatchEvent<Path>>> p = createActive();
			println("subscribing...");
			Subscription s = p._3().subscribeOn(Schedulers.computation()).subscribe(we -> printWatchEvent(we), t -> {
				log.info("Observable error...");
				log.error(t.getMessage(), t);
			}, () -> println("completed"));
			println("subscribed");
			// does not work
			p._3().take(5).forEach(we -> printWatchEvent(we));
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
		println(format("monitoring dir: %s", dir.getAbsolutePath()));
//		WatchService s = FileSystems.getDefault().newWatchService();
		P2<WatchService, WatchKey> s = Rx.register(dir, Util.ALL_EVENTS);
		Observable<WatchEvent<Path>> o = Rx.observableActive(s._1(), s._2())._1();

		println("subscribing...");
		o.subscribe(we -> printWatchEvent(we), t -> println(t), () -> println("completed"));
		println("subscribed");


		// does not work
		o.take(5).forEach(we -> printWatchEvent(we));
		sleep(9000);

		s._1().close();
	}

}
