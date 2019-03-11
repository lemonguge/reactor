package cn.homjie.reactor.intro;

import lombok.extern.slf4j.Slf4j;

/**
 * @author jiehong.jh
 * @date 2019-03-09
 */
@Slf4j
public class Channel {

    public void poll(long n) {
        if (n != Long.MAX_VALUE) {
            log.info("request: {}", n);
        } else {
            log.info("request infinite");
        }
    }

    public void cancel() {
        log.info("cancel");
    }

    public void close() {
        log.info("close");
    }
}
