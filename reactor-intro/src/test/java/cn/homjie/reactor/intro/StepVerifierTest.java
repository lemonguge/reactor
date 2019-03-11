package cn.homjie.reactor.intro;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * @author jiehong.jh
 * @date 2019-03-08
 */
public class StepVerifierTest {

    @Test
    void sequenceOk() {
        StepVerifier.create(Flux.just("a", "b"))
            // 声明测试时所期待的流中的下一个元素的值
            .expectNext("a")
            .expectNext("b")
            // 验证流是否正常结束
            .verifyComplete();
    }

    @Test
    void sequenceError() {
        Flux<String> flux = Flux.just("a", "b").concatWith(Mono.error(new IllegalStateException()));
        StepVerifier.create(flux)
            // 声明测试时所期待的流中的下一个元素的值
            .expectNext("a")
            .expectNext("b")
            // 验证流是否正常结束
            .verifyError();
    }

    @Test
    void virtualTime() {
        // 创建出使用虚拟时钟的 StepVerifier
        StepVerifier.withVirtualTime(() -> Flux.interval(Duration.ofHours(4), Duration.ofDays(1)).take(2))
            .expectSubscription()
            // 验证在 4 个小时之内没有任何消息产生
            .expectNoEvent(Duration.ofHours(4))
            .expectNext(0L)
            // 让虚拟时钟前进一天
            .thenAwait(Duration.ofDays(1))
            .expectNext(1L)
            .verifyComplete();
    }
}
