package com.github.mperry.watch;

import com.sun.nio.file.SensitivityWatchEventModifier;
import fj.*;
import fj.data.*;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import rx.Observable;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import static java.lang.String.format;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
//import java.util.List;

/**
 * Created by MarkPerry on 11/08/2014.
 */
public class FileMonitor {

    public static final List<WatchEvent.Kind<Path>> ALL_EVENTS = List.list(ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
    static final Logger log = Util.logger(FileMonitor.class);

    public static final String DEFAULT_PATH = SystemUtils.IS_OS_UNIX ? "/Users/mperry/repositories/file-monitor" : "D:/repositories/file-monitor";

    public static final File DEFAULT_DIR = new File(DEFAULT_PATH);
	static final SensitivityWatchEventModifier SENSITIVITY = SensitivityWatchEventModifier.HIGH;

    public static P2<WatchService, WatchKey> register(File dir, List<WatchEvent.Kind<Path>> list) throws IOException {
		WatchService s = FileSystems.getDefault().newWatchService();
        WatchKey k = dir.toPath().register(s, list.toCollection().toArray(new WatchEvent.Kind[list.length()]), SENSITIVITY);
		return P.p(s, k);
	}

	public static P3<WatchService, WatchKey, Observable<WatchEvent<Path>>> createDirect(File dir, List<WatchEvent.Kind<Path>> list) throws IOException {
		P2<WatchService, WatchKey> s = register(dir, list);
		return P.p(s._1(), s._2(), createDirect(s._1(), s._2()));
	}

	public static Observable<WatchEvent<Path>> createDirect(final WatchService s, final WatchKey key) {

		Observable.OnSubscribe<WatchEvent<Path>> os = sub -> {
			try {
				while (true) {
//                    log.info("Polling WatchService events...");
                    WatchKey k = s.take();
//                    log.info("Finished polling.");
					for (WatchEvent<?> e: k.pollEvents()) {
						WatchEvent<Path> we = (WatchEvent<Path>) e;
						if (sub.isUnsubscribed()) {
							sub.onCompleted();
							return;
						}
						sub.onNext(we);
					}
					boolean b = k.reset();
					if (!b) {
                        sub.onError(new Exception(format("Key invalid: %s", k)));
                        return;
					}
				}
			} catch (InterruptedException e) {
				log.info("interrupted in take");
				sub.onCompleted();
//				sub.onError(e);
			}
		};
		return Observable.create(os);
	}

	public static P1<Observable<WatchEvent<Path>>> observableActive(final WatchService s, WatchKey k) {
		return P.lazy(u -> Observable.from(streamEvents(s, k)._1()));
	}

	public static P1<Observable<WatchEvent<Path>>> observableInactive(final WatchService s, WatchKey k) {
		return observableOptions(s, k).map(o -> mapFilter(o, Function.identity()));
	}

    public static P1<Observable<Option<WatchEvent<Path>>>> observableOptions(final WatchService s, WatchKey k) {
        return P.lazy(u -> Observable.from(streamOptionEvent(s, k)._1()));
    }

	/**
	 * Process events on key
	 * @param key
	 */
	public static Seq<WatchEvent<Path>> processEventSeq(WatchKey key) {
		Seq<WatchEvent<Path>> result = Seq.<WatchEvent<Path>>empty();
		for (WatchEvent<?> event : key.pollEvents()) {
			WatchEvent<Path> we = (WatchEvent<Path>) event;
			result = result.snoc(we);
		}
		boolean b = key.reset();
		if (!b) {
			log.info(String.format("Key %s is now invalid"), key);
		}
		return result;
	}

	public static List<WatchEvent<Path>> processEventList(WatchKey key) {
		List<WatchEvent<Path>> result = List.<WatchEvent<Path>>nil();
		for (WatchEvent<?> event : key.pollEvents()) {
			WatchEvent<Path> we = (WatchEvent<Path>) event;
			result = result.cons(we);
		}
		boolean b = key.reset();
		if (!b) {
			log.info(String.format("Key %s is now invalid"), key);
		}
		return result.reverse();
	}

	/**
	 * Process the next key on the watch service
	 * @param s
	 * @param k Key for the watch service
	 * @return Fail if the watch service key is invalid or interrupted otherwise success with the sequence of watch events.
	 */
	public static Validation<String, Seq<WatchEvent<Path>>> processNextKey(final WatchService s, final WatchKey k) {
		if (!k.isValid()) {
			return Validation.fail("WatchKey is invalid: " + k);
		} else {
			Validation<String, WatchKey> v = Validation.validation(takeValidation(s).toEither().left().map(e -> e.getMessage()));
			return v.map(key -> processEventSeq(key));
		}
	}

	public static IO<Validation<String, Seq<WatchEvent<Path>>>> processNextKeyIO(final WatchService s, final WatchKey k) {
		return IOFunctions.unit(P.lazy(u -> processNextKey(s, k)));
	}

	public static IO<Seq<WatchEvent<Path>>> processNextKeySimpleIO(final WatchService s, final WatchKey k) {
		return IOFunctions.map(processNextKeyIO(s, k), v -> v.isFail() ? Seq.<WatchEvent<Path>>empty() : v.success());
	}

	public static Stream<IO<Seq<WatchEvent<Path>>>> streamIo(final WatchService s, final WatchKey k) {
		return Stream.repeat(processNextKeySimpleIO(s, k));
	}

	public <A> IO<Unit> runStreamIo(final WatchService s, final WatchKey k, F<Seq<WatchEvent<Path>>, A> f) {
		return IOFunctions.unit(P.lazy(u -> {
			Runnable r = () -> streamIo(s, k).foreach(io -> {
				try {
					Seq<WatchEvent<Path>> seq = io.run();
					f.f(seq);
				} catch (IOException e) {
					log.error(e.getMessage(), e);
				}
				return null;
			});
			r.run();
			return Unit.unit();
		}));
	}

	public static Stream<Seq<WatchEvent<Path>>> stream(final WatchService s, final WatchKey k) throws IOException {
		return IOFunctions.sequence(streamIo(s, k)).run();

	}

	public static P1<Stream<WatchEvent<Path>>> streamEvents(final WatchService s, WatchKey k) {
        return P.lazy(u -> {
            final Stream<WatchEvent<Path>> empty = Stream.nil();
            return takeOption(s).map(key -> {
				Stream<WatchEvent<Path>> result = empty;
				for (WatchEvent<Path> we: processEventList(key)) {
					result = result.cons(we);
				}
                return result.reverse().append(streamEvents(s, k));
            }).orSome(empty);
        });
    }

    public static P1<Stream<Option<WatchEvent<Path>>>> streamOptionEvent(final WatchService s, WatchKey k) {
		final Stream<Option<WatchEvent<Path>>> empty = Stream.nil();
        return P.lazy(u -> Stream.cons(Option.<WatchEvent<Path>>none(), P.lazy(u2 -> {
			return takeOption(s).map(key -> {
				Stream<Option<WatchEvent<Path>>> result = empty;
				for (WatchEvent<Path> we : processEventList(key)) {
					result = result.cons(Option.some(we));
				}
				return result.reverse().append(streamOptionEvent(s, k));
			}).orSome(empty);
		})));
    }

    static Option<WatchKey> takeOption(WatchService s) {
		return takeValidation(s).toOption();
    }

	static Validation<InterruptedException, WatchKey> takeValidation(WatchService s) {
		try {
			return Validation.success(s.take());
		} catch (InterruptedException e) {
			return Validation.fail(e);
		}
	}

    public static P3<WatchService, WatchKey, P1<Stream<WatchEvent<Path>>>> streamEvents(File dir, List<WatchEvent.Kind<Path>> list) throws IOException {
        P2<WatchService, WatchKey> p = register(dir, list);
        return P.p(p._1(), p._2(), streamEvents(p._1(), p._2()));
    }

	public static <A, B> Observable<B> flatMap(Observable<Option<A>> o, F<A, Observable<B>> f) {
		return o.flatMap(oa -> {
			return oa.isNone() ? Observable.empty() : f.f(oa.some());
		});
	}

	public static <A, B> Observable<B> mapFilter(Observable<A> o, F<A, Boolean> predicate, F<A, B> transform) {
		return o.flatMap(a -> {
			return !predicate.f(a) ? Observable.empty() : Observable.just(transform.f(a));
		});
	}

	public static <A, B> Observable<B> mapFilter(Observable<Option<A>> o, F<A, B> f) {
		return mapFilter(o, oa -> oa.isSome(), oa -> f.f(oa.some()));
	}

}

