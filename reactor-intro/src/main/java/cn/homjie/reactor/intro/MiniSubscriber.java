package cn.homjie.reactor.intro;

import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;

/**
 * @author jiehong.jh
 * @date 2019-03-07
 */
@Slf4j
public class MiniSubscriber<T> extends BaseSubscriber<T> {

    @Override
    public void hookOnSubscribe(Subscription subscription) {
        log.info("Mini Subscribed");
        // make the first request
        request(1);
    }

    @Override
    public void hookOnNext(T value) {
        log.info("Next: {}", value);
        // one request at a time
        request(1);
    }
}