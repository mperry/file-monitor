package com.github.mperry;

import com.github.mperry.watch.FileMonitor;
import com.github.mperry.watch.Util;
import fj.F;
import fj.P2;
import fj.P3;
import fj.Unit;
import org.junit.Test;
import org.slf4j.Logger;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import static com.github.mperry.watch.Util.generateEvents;
import static com.github.mperry.watch.Util.generateEventsAsync;
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



	P3<WatchService, WatchKey, Observable<WatchEvent<Path>>> createDirect() throws IOException {
		File dir = Util.EVENT_DIR;
		println(format("monitoring dir: %s", dir.getAbsolutePath()));
		return FileMonitor.createDirect(dir, FileMonitor.ALL_EVENTS);
	}


    public <A> void createOnNewThread(F<Observable<WatchEvent<Path>>, Unit> f) {
        Runnable r = () -> {
            try {
                Util.printThread();
                f.f(createDirect()._3());
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


    // WARNING: Direct subscribe does not work for full subscribe on another thread
//	@Test
	public void test4() throws IOException, InterruptedException {
		File dir = Util.EVENT_DIR;
		println(format("monitoring dir: %s", dir.getAbsolutePath()));
//		WatchService s = FileSystems.getDefault().newWatchService();
		P2<WatchService, WatchKey> s = FileMonitor.register(dir, FileMonitor.ALL_EVENTS);
        generateEventsAsync(100, some(500));
		Observable<WatchEvent<Path>> o = FileMonitor.observableActive(s._1(), s._2())._1();
		println("subscribing...");
		o.subscribeOn(Schedulers.io()).subscribe(we -> printWatchEvent(we), t -> println(t), () -> println("completed"));
		println("subscribed");
		// does not work
		o.take(5).forEach(we -> printWatchEvent(we));
		sleep(2000);
		s._1().close();
	}

}
