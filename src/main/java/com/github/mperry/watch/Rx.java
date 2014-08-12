package com.github.mperry.watch;

import fj.P;
import fj.P1;
import fj.P2;
import fj.data.List;
import fj.data.Option;
import fj.data.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import static java.lang.String.format;
import static java.nio.file.FileSystems.getDefault;
//import java.util.List;

/**
 * Created by MarkPerry on 11/08/2014.
 */
public class Rx {

	static final Logger log = Util.logger(Rx.class);

    public static final String DEFAULT_PATH = "/Users/mperry/repositories/file-monitor";
    public static final File DEFAULT_DIR = new File(DEFAULT_PATH);


    public static WatchService register(File dir, List<WatchEvent.Kind<Path>> list) throws IOException {
		WatchService s = FileSystems.getDefault().newWatchService();
		Path p = dir.toPath();
		for (WatchEvent.Kind<Path> k: list) {
			p.register(s, k);
		}
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
                    log.info("Polling WatchService events...");
                    WatchKey k = s.take();
                    log.info("Finished polling.");
					for (WatchEvent<?> e: k.pollEvents()) {
						WatchEvent<Path> we = (WatchEvent<Path>) e;
						if (sub.isUnsubscribed()) {
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
				sub.onError(e);
			}
		};
		return Observable.create(os);
	}

	public static P1<Observable<WatchEvent<Path>>> observable(final WatchService s) {
		return P.lazy(u -> Observable.from(stream(s)._1()));
	}

    public static P1<Observable<Option<WatchEvent<Path>>>> observableOpt(final WatchService s) {
        return P.lazy(u -> Observable.from(streamOpt(s)._1()));
    }

    public static P1<Stream<WatchEvent<Path>>> stream(final WatchService s) {
        return P.lazy(u -> {
            final Stream<WatchEvent<Path>> empty = Stream.nil();

            log.info("Polling WatchService events...");
            Option<WatchKey> optKey = take(s);
            log.info("Finished polling.");
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
                log.info("Polling WatchService events...");
                Option<WatchKey> optKey = take(s);
                log.info("Finished polling.");
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

}

