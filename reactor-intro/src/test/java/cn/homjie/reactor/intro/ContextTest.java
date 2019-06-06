package cn.homjie.reactor.intro;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * @author jiehong.jh
 * @date 2019-06-06
 */
@Slf4j
public class ContextTest {

    /**
     * <p> Context 是绑定到每一个链中的 Subscriber 上的。
     * <p> 它使用 Subscription 的传播机制来让自己对每一个操作符都可见（从最后一个 subscribe 沿链向上）
     */

    @Test
    public void simple() {
        String key = "message";
        Mono<String> r = Mono.just("Hello")
            .flatMap(s -> Mono.subscriberContext()
                .map(ctx -> s + " " + ctx.get(key)))
            .subscriberContext(ctx -> ctx.put(key, "World"));

        StepVerifier.create(r)
            .expectNext("Hello World")
            .verifyComplete();
    }

    @Test
    public void immutable1() {
        String key = "message";
        Mono<String> r = Mono.just("Hello")
            // 对下游不可见
            .subscriberContext(ctx -> ctx.put(key, "World"))
            .flatMap(s -> Mono.subscriberContext()
                .map(ctx -> s + " " + ctx.getOrDefault(key, "Stranger")));

        StepVerifier.create(r)
            .expectNext("Hello Stranger")
            .verifyComplete();
    }

    @Test
    public void immutable2() {
        String key = "message";

        Mono<String> r = Mono.subscriberContext()
            .map(ctx -> ctx.put(key, "Hello"))
            .flatMap(ctx -> {
                log.info("0. ctx: " + ctx);
                return Mono.subscriberContext();
            })
            .map(ctx -> {
                log.info("1. ctx: " + ctx);
                return ctx.getOrDefault(key, "Default");
            });

        StepVerifier.create(r)
            .expectNext("Default")
            .verifyComplete();
    }

    @Test
    public void order() {
        String key = "message";
        Mono<String> r = Mono.just("Hello")
            // 2、之后数据开始流动，拿到最近的 Context
            .flatMap(s -> Mono.subscriberContext()
                .map(ctx -> {
                    log.info("2. ctx: " + ctx);
                    return s + " " + ctx.get(key);
                })
            ).subscriberContext(ctx -> {
                log.info("1. ctx: " + ctx);
                return ctx.put(key, "Reactor");
            })
            // 1、订阅信号（subscription signal）向上游移动
            .subscriberContext(ctx -> {
                log.info("0. ctx: " + ctx);
                return ctx.put(key, "World");
            });

        StepVerifier.create(r)
            .expectNext("Hello Reactor")
            .verifyComplete();
    }

    @Test
    public void signal1() {
        // 订阅信号向上游移动，数据信号向下游移动
        String key = "message";
        Mono<String> r = Mono.just("Hello")
            .flatMap(s -> Mono.subscriberContext()
                .map(ctx -> {
                    log.info("2. ctx: " + ctx);
                    return s + " " + ctx.get(key);
                }))
            .subscriberContext(ctx -> {
                log.info("1. ctx: {}", ctx);
                return ctx.put(key, "Reactor");
            })
            .flatMap(s -> Mono.subscriberContext()
                .map(ctx -> {
                    // Context 的不可变性
                    log.info("3. ctx: " + ctx);
                    return s + " " + ctx.get(key);
                }))
            .subscriberContext(ctx -> {
                log.info("0. ctx: {}", ctx);
                return ctx.put(key, "World");
            });

        StepVerifier.create(r)
            .expectNext("Hello Reactor World")
            .verifyComplete();
    }

    @Test
    public void signal2() {
        String key = "message";
        Mono<String> r =
            Mono.just("Hello")
                .flatMap(s -> Mono.subscriberContext()
                    .map(ctx -> {
                        log.info("1. ctx: {}", ctx);
                        return s + " " + ctx.get(key);
                    })
                )
                .flatMap(s -> Mono.subscriberContext()
                    .map(ctx -> {
                        log.info("3. ctx: " + ctx);
                        return s + " " + ctx.get(key);
                    })
                    .subscriberContext(ctx -> {
                        log.info("2. ctx: " + ctx);
                        return ctx.put(key, "Reactor");
                    })
                )
                .subscriberContext(ctx -> {
                    log.info("0. ctx: {}", ctx);
                    return ctx.put(key, "World");
                });

        StepVerifier.create(r)
            .expectNext("Hello World Reactor")
            .verifyComplete();
    }
}
