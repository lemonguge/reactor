package cn.homjie.reactor.intro;

import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/**
 * @author jiehong.jh
 * @date 2019-03-08
 */
@Slf4j
public class SchedulerTest {

    /*
     * 1、当前线程，通过 Schedulers.immediate()方法来创建。
     * 2、单一的可复用的线程，通过 Schedulers.single()方法来创建。
     *      如果你想使用专一的线程，就对每一个调用使用 Schedulers.newSingle()。
     * 3、使用弹性的线程池，通过 Schedulers.elastic()方法来创建。
     *      线程池中的线程是可以复用的。当所需要时，新的线程会被创建。
     *      如果一个线程闲置太长时间，则会被销毁。
     *      该调度器适用于 I/O 操作相关的流的处理。
     * 4、使用对并行操作优化的线程池，通过 Schedulers.parallel()方法来创建。
     *      其中的线程数量取决于 CPU 的核的数量。
     *      该调度器适用于计算密集型的流的处理。
     * 5、使用支持任务调度的调度器，通过 Schedulers.timer()方法来创建。
     * 6、从已有的 ExecutorService 对象中创建调度器，通过 Schedulers.fromExecutorService()方法来创建。
     * 7、一些操作符默认会使用一个指定的调度器（通常也允许开发者调整为其他调度器）
     *      通过工厂方法 Flux.interval(Duration.ofMillis(300))
     *      生成的每 300ms 打点一次的 Flux<Long>，默认情况下使用的是 Schedulers.parallel()
     */

    /**
     * Reactor 提供了两种在响应式链中调整调度器 Scheduler 的方法：publishOn 和 subscribeOn
     */
    @Test
    void schedule() {
        Flux.create(sink -> {
            sink.next(Thread.currentThread().getName());
            sink.complete();
        })
            // 改变后续的操作符的执行所在线程
            .publishOn(Schedulers.single())
            .map(x -> String.format("[%s] %s", Thread.currentThread().getName(), x))
            .publishOn(Schedulers.elastic())
            .map(x -> String.format("(%s) %s", Thread.currentThread().getName(), x))
            // 影响到源头的线程执行环境 Context
            .subscribeOn(Schedulers.parallel())
            .toStream()
            .forEach(log::info);
    }

    @Test
    void parallel() throws InterruptedException {
        // 1、创建一个有 1,000 个元素的 Flux
        Flux.range(1, 1000)
            .log()
            // 2、切换到创建等同于 CPU 个数的线程
            .publishOn(Schedulers.parallel())
            .subscribeOn(Schedulers.newSingle("origin"))
            // 3、subscribe 之前什么都不会发生
            .subscribe(v -> log.info("v: {}", v));
        TimeUnit.SECONDS.sleep(3);
    }
}
