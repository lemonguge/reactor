package cn.homjie.reactor.intro;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * @author jiehong.jh
 * @date 2019-03-07
 */
@Slf4j
public class MonoTest {

    @Test
    void empty() {
        Mono<String> noData = Mono.empty();
        noData.subscribe(v -> log.info("value: {}", v));
    }

    @Test
    void just() {
        Mono<String> data = Mono.just(Thread.currentThread().getName());
        data.subscribe(v -> log.info("value: {}", v));
    }

    @Test
    void delay() throws InterruptedException {
        Mono<Long> mono = Mono.delay(Duration.ofMillis(500));
        long start = System.currentTimeMillis();
        mono.subscribe(value -> {
            long exec = System.currentTimeMillis() - start;
            log.info("exec: {}, value: {}", exec, value);
        });
        log.info("time: {}", (System.currentTimeMillis() - start));
        TimeUnit.SECONDS.sleep(1);
    }

    @Test
    void justOrEmpty() {
        Mono.justOrEmpty(Optional.of("Hello")).subscribe(v -> log.info("value: {}", v));
    }

    @Test
    void create() {
        Mono.create(sink -> sink.success("Hello")).subscribe(v -> log.info("value: {}", v));
    }

    @Test
    void biz() {
        Mono<String> mono = Mono.when(
            Mono.fromCallable(() -> {
                log.info("request biz1");
                TimeUnit.SECONDS.sleep(1);
                log.info("response biz1");
                return "biz1";
            }).subscribeOn(Schedulers.elastic()),
            Mono.fromCallable(() -> {
                log.info("request biz2");
                TimeUnit.SECONDS.sleep(2);
                log.info("response biz2");
                return "biz2";
            }).subscribeOn(Schedulers.elastic())
        ).then(Mono.just("ok"));

        log.info("start..");
        log.info("value: {}", mono.block());
        log.info("end..");
    }
}
