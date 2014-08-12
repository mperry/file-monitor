package com.github.mperry.watch;

import com.sun.nio.file.SensitivityWatchEventModifier;
import fj.*;
import fj.data.Java;
import fj.data.List;
import fj.data.Option;
import fj.data.Stream;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import static com.github.mperry.watch.Util.threadId;
import static java.lang.String.format;
import static java.nio.file.FileSystems.getDefault;
//import java.util.List;

/**
 * Created by MarkPerry on 11/08/2014.
 */
public class Rx {

	static final Logger log = Util.logger(Rx.class);

    public static final String DEFAULT_PATH = SystemUtils.IS_OS_UNIX ? "/Users/mperry/repositories/file-monitor" : "D:/repositories/file-monitor";

    public static final File DEFAULT_DIR = new File(DEFAULT_PATH);
	static final SensitivityWatchEventModifier SENSITIVITY = SensitivityWatchEventModifier.HIGH;

    public static WatchService register(File dir, List<WatchEvent.Kind<Path>> list) throws IOException {
		WatchService s = FileSystems.getDefault().newWatchService();
        // ignore k (below) for now
        WatchKey k = dir.toPath().register(s, list.toCollection().toArray(new WatchEvent.Kind[list.length()]), SENSITIVITY);
		return s;
	}

	public static P2<WatchService, Observable<WatchEvent<Path>>> create(File dir, List<WatchEvent.Kind<Path>> list) throws IOException {
		WatchService s = register(dir, list);
		return P.p(s, create(s));
	}

	public static Observable<WatchEvent<Path>> create(final WatchService s) {
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

	public static P1<Observable<WatchEvent<Path>>> observable(final WatchService s) {
		return P.lazy(u -> Observable.from(stream(s)._1()));
	}

	public static P1<Observable<WatchEvent<Path>>> observable2(final WatchService s) {
		return observableOpt(s).map(o -> mapFilter(o, Function.identity()));
	}

    public static P1<Observable<Option<WatchEvent<Path>>>> observableOpt(final WatchService s) {
        return P.lazy(u -> Observable.from(streamOpt(s)._1()));
    }

    public static P1<Stream<WatchEvent<Path>>> stream(final WatchService s) {
        return P.lazy(u -> {
            final Stream<WatchEvent<Path>> empty = Stream.nil();
            Util.printThread();
            Option<WatchKey> optKey = take(s);
            return optKey.map(key -> {
                Stream<WatchEvent<Path>> result = empty;
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent<Path> we = (WatchEvent<Path>) event;
                    result = result.snoc(we);
                }
                boolean b = key.reset();
                if (!b) {
                    log.error(String.format("Key %s is invalid"), key);
                    return result;
                }
                return result.append(stream(s));
            }).orSome(empty);
        });
    }

    public static P1<Stream<Option<WatchEvent<Path>>>> streamOpt(final WatchService s) {
        return P.lazy(u -> {
            return Stream.cons(Option.<WatchEvent<Path>>none(), P.lazy(u2 -> {
                final Stream<Option<WatchEvent<Path>>> empty = Stream.nil();
//                log.info(format("Polling WatchService events on %s...", threadId()));
                Option<WatchKey> optKey = take(s);
//                log.info("Finished polling.");
                return optKey.map(key -> {
                    Stream<Option<WatchEvent<Path>>> result = empty;
                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent<Path> we = (WatchEvent<Path>) event;
                        result = result.snoc(Option.some(we));
                    }
                    boolean b = key.reset();
                    if (!b) {
                        log.error(String.format("Key %s is invalid"), key);
                        return result;
                    }
                    return result.append(streamOpt(s));
                }).orSome(empty);
            }));
        });
    }

    static Option<WatchKey> take(WatchService s) {
        try {
            return Option.fromNull(s.take());
        } catch (InterruptedException e) {
            return Option.<WatchKey>none();
        }
    }

    public static P2<WatchService, P1<Stream<WatchEvent<Path>>>> stream(File dir, List<WatchEvent.Kind<Path>> list) throws IOException {
        WatchService s2 = register(dir, list);
        return P.p(s2, stream(s2));
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

