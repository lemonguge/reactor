package cn.homjie.reactor.intro;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import reactor.core.Exceptions;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

/**
 * @author jiehong.jh
 * @date 2019-03-07
 */
@Slf4j
public class FluxTest {

    @Test
    void just() {
        Flux<String> flux = Flux.just("foo", "bar", "foobar");
        // 订阅并触发序列
        flux.subscribe();
    }

    @Test
    void fromIterable() {
        List<String> iterable = Arrays.asList("foo", "bar", "foobar");
        Flux<String> flux = Flux.fromIterable(iterable);
        // 对每个产生的值 do something
        flux.subscribe(v -> log.info("value: {}", v));
    }

    @Test
    void empty() {
        // 不包含任何元素，只发布结束消息
        Flux.empty().subscribe(new BaseSubscriber<Object>() {
            @Override
            protected void hookOnComplete() {
                log.info("complete");
            }
        });
    }

    @Test
    void never() {
        // 不包含任何消息通知
        Flux.never().subscribe(new BaseSubscriber<Object>() {
            @Override
            protected void hookOnComplete() {
                // unreachable
                log.info("complete");
            }
        });
    }

    @Test
    void interval() throws InterruptedException {
        // 从 0 递增
        Flux<Long> flux = Flux.interval(Duration.ofSeconds(1));
        flux.subscribe(v -> log.info("value: {}", v));
        TimeUnit.SECONDS.sleep(5);
    }

    @Test
    void generateOne() {
        Flux.generate(sink -> {
            sink.next(Thread.currentThread().getName());
            // 每次回调的时候最多只能被调用一次
            //sink.next("World");
            sink.complete();
        }).subscribe(v -> log.info("value: {}", v));
    }

    @Test
    void generateList() {
        final Random random = new Random();
        Flux.generate(ArrayList::new, (list, sink) -> {
            int value = random.nextInt(100);
            list.add(value);
            if (list.size() == 10) {
                sink.next(Thread.currentThread().getName());
                sink.complete();
            } else {
                sink.next(value);
            }
            return list;
        }).subscribe(v -> log.info("value: {}", v));
    }

    @Test
    void generateState() {
        Flux.generate(
            // 初始化状态值
            () -> 0,
            (state, sink) -> {
                sink.next("3 x " + state + " = " + 3 * state);
                if (state == 10) {
                    sink.complete();
                }
                // 	返回一个新的状态值用于下次调用
                return state + 1;
            },
            // 在生成器终止或下游取消后调用，接收要处理的最后一个状态
            (state) -> log.info("state: {}", state))
            .subscribe(v -> log.info("value: {}", v));
    }

    /**
     * <p>OverflowStrategy 定义背压行为：
     * <p>1、IGNORE： 完全忽略下游背压请求，这可能会在下游队列积满的时候导致 IllegalStateException
     * <p>2、ERROR： 当下游跟不上节奏的时候发出一个 IllegalStateException 的错误信号
     * <p>3、DROP：当下游没有准备好接收新的元素的时候抛弃这个元素
     * <p>4、LATEST：让下游只得到上游最新的元素
     * <p>5、BUFFER：（默认的）缓存所有下游没有来得及处理的元素
     */
    @Test
    void create() {
        Flux.create(sink -> {
            for (int i = 0; i < 10; i++) {
                sink.next(i);
            }
            sink.next(Thread.currentThread().getName());
            sink.complete();
        }).subscribe(v -> log.info("value: {}", v));
    }

    @Test
    void error() {
        Flux.error(new NullPointerException())
            .doOnError(Throwable::printStackTrace)
            .subscribe();
        log.info("-----");
        Flux.just(1, 2)
            .concatWith(Mono.error(new IllegalStateException()))
            // 通过 subscribe()方法处理正常和错误消息
            .subscribe(v -> log.info("value: {}", v), e -> log.error("Error", e));
    }

    @Test
    void retry() {
        IllegalStateException exception = new IllegalStateException();
        Flux<Integer> flux = Flux.just(1, 2)
            .concatWith(Mono.error(exception))
            // 重试是通过重新订阅序列来实现的
            .retry(1);
        flux.subscribe(v -> log.info("value: {}", v), e -> {
            log.error("Error");
            Assertions.assertEquals(e, exception);
        });
    }

    /**
     * <p>1、每次出现错误，错误信号会发送给伴随 Flux，后者已经被你用 Function 包装
     * <p>2、如果伴随 Flux 发出元素，就会触发重试
     * <p>3、如果伴随 Flux 完成（complete），重试循环也会停止，并且原始序列也会 完成（complete）
     * <p>4、如果伴随 Flux 产生一个错误，重试循环停止，原始序列也停止 或 完成，并且这个错误会导致 原始序列失败并终止
     */
    @Test
    void companion() {
        Flux<String> flux = Flux
            .<String>error(new IllegalArgumentException())
            .doOnError(e -> log.error("Error: {}", e.getClass()))
            .doFinally(type -> log.info("finally type: {}", type))
            // 认为前 3 个错误是可以重试的（take(3)），再有错误就放弃
            .retryWhen(companion -> companion.take(3));
        flux.subscribe();
    }

    @Test
    void elapsed() throws InterruptedException {
        Flux.interval(Duration.ofMillis(250))
            .map(input -> {
                if (input < 3) { return "tick " + input; }
                throw new RuntimeException("boom");
            })
            // 关联从当前值与上个值发出的时间间隔
            .elapsed()
            .retry(1)
            .subscribe(v -> log.info("value: {}", v), e -> log.error("Error", e));

        Thread.sleep(2100);
    }

    /**
     * 与其他以 doOn 开头的方法一样，只起副作用（"side-effect"）。它们对序列都是只读， 而不会带来任何改动。
     */
    @Test
    void doOnError() {
        LongAdder failureStat = new LongAdder();
        Flux.just("unknown")
            .flatMap(k -> {
                throw new RuntimeException("Timeout");
            })
            // // 错误将继续传递
            .doOnError(e -> {
                failureStat.increment();
                log.info("uh oh, falling back");
            })
            .onErrorResume(e -> Mono.just("cache"))
            .subscribe(v -> log.info("value: {}", v), e -> log.error("Error", e));
    }

    @Test
    void onErrorReturn() {
        Flux.just(1, 2)
            .concatWith(Mono.error(new IllegalStateException()))
            // 静态缺省值。当出现错误时，流会产生默认值 0
            .onErrorReturn(0)
            .subscribe(v -> log.info("value: {}", v));
    }

    @Test
    void onErrorResume() {
        Flux.just(1, 2)
            .concatWith(Mono.error(new IllegalStateException()))
            // 动态候补值。出现错误时使用另外的流
            .onErrorResume(e -> Mono.just(0))
            .subscribe(v -> log.info("value: {}", v));
        log.info("-----");
        Flux.just(1, 2)
            .concatWith(Mono.error(new IllegalArgumentException()))
            .onErrorResume(IllegalStateException.class, e -> Mono.just(0))
            // 优先匹配
            .onErrorResume(RuntimeException.class, e -> Mono.just(-2))
            // unreachable
            .onErrorResume(IllegalArgumentException.class, e -> Mono.just(-1))
            .subscribe(v -> log.info("value: {}", v));
    }

    @Test
    void onErrorMap() {
        Flux.just("Hello")
            .concatWith(Mono.error(new IllegalArgumentException()))
            // 捕获并重新抛出
            .onErrorMap(original -> new RuntimeException("Origin: " + original.getClass().getName()))
            .doOnNext(log::info)
            .subscribe(v -> log.info("value: {}", v), e -> log.error("Error", e));
    }

    @Test
    void propagate() {
        Flux.range(1, 10)
            .map(i -> {
                try {
                    if (i > 3) {
                        throw new IOException("boom " + i);
                    }
                    return "OK " + i;
                } catch (IOException e) {
                    throw Exceptions.propagate(e);
                }
            })
            .subscribe(
                v -> log.info("RECEIVED: {}", v),
                e -> {
                    if (Exceptions.unwrap(e) instanceof IOException) {
                        log.info("Something bad happened with I/O");
                    } else {
                        log.warn("Something bad happened");
                    }
                }
            );
    }

    @Test
    void interrupt() {
        Flux<Integer> flux = Flux.range(1, 4)
            .map(i -> {
                if (i <= 3) {
                    return i;
                }
                throw new RuntimeException("Got to 4");
            });
        // 对值和错误都有处理
        flux.subscribe(v -> log.info("value: {}", v),
            e -> log.error("Error", e),
            // unreachable
            () -> log.info("Done"));
    }

    @Test
    void complete() {
        Flux<Integer> flux = Flux.range(1, 4);
        flux.subscribe(v -> log.info("value: {}", v),
            e -> log.error("Error", e),
            () -> log.info("Done"));
    }

    @Test
    void mini() {
        Flux<Integer> flux = Flux.range(1, 4);
        // 第一个订阅者只 request 2
        flux.subscribe(v -> log.info("value: {}", v),
            // unreachable
            e -> log.error("Error", e),
            // unreachable
            () -> log.info("Done"),
            s -> s.request(2));
        // 第二个订阅者 request one by one
        flux.subscribe(new MiniSubscriber<>());
    }

    @Test
    void cancel() {
        Flux.range(1, 10)
            .doOnRequest(r -> log.info("request of {}", r))
            .subscribe(new BaseSubscriber<Integer>() {

                @Override
                public void hookOnSubscribe(Subscription subscription) {
                    request(2);
                }

                @Override
                public void hookOnNext(Integer value) {
                    log.info("Cancelling after having received {}", value);
                    cancel();
                }
            });
    }

    /**
     * 当前流中的元素收集到集合中，并把集合对象作为流中的新元素
     */
    @Test
    void buffer() {
        // 包含的元素的最大数量
        Flux.range(1, 100).buffer(20).subscribe(v -> log.info("value: {}", v));
        log.info("-----");
        Flux.interval(Duration.ofMillis(100))
            // 收集的时间间隔
            .buffer(Duration.ofMillis(1001))
            .take(2)
            // 序列的生成是异步的，而转换成 Stream 对象可以保证主线程在序列生成完成之前不会退出
            .toStream().forEach(v -> log.info("value: {}", v));
        log.info("-----");
        // 一直收集直到 true
        Flux.range(1, 10).bufferUntil(i -> i % 2 == 0).subscribe(v -> log.info("value: {}", v));
        log.info("-----");
        // 只收集为 true
        Flux.range(1, 10).bufferWhile(i -> i % 2 == 0).subscribe(v -> log.info("value: {}", v));
    }

    @Test
    void groupBy() {
        StepVerifier.create(
            Flux.just(1, 3, 5, 2, 4, 6, 11, 12, 13)
                .groupBy(i -> i % 2 == 0 ? "even" : "odd")
                .concatMap(
                    // 如果组为空，显示为 -1
                    g -> g.defaultIfEmpty(-1)
                        // 转换为字符串
                        .map(String::valueOf)
                        // 以该组的 key 开头
                        .startWith(g.key()))
        )
            .expectNext("odd", "1", "3", "5", "11", "13")
            .expectNext("even", "2", "4", "6", "12")
            .verifyComplete();
    }

    @Test
    void filter() {
        // 只留下满足 Predicate 指定条件的元素
        Flux.range(1, 10).filter(i -> i % 2 == 0).subscribe(v -> log.info("value: {}", v));
    }

    /**
     * 类似于 buffer，所不同的是 window 操作符是把当前流中的元素收集到另外的 Flux 序列中
     */
    @Test
    void window() {
        Flux.range(1, 100).window(20)
            .subscribe(v -> log.info("value: {}", v));
        log.info("-----");
        Flux.interval(Duration.ofMillis(100))
            .window(Duration.ofMillis(1001))
            .take(2)
            .toStream().forEach(v -> log.info("value: {}", v));
    }

    /**
     * 把当前流中的元素与另外一个流中的元素按照一对一的方式进行合并
     */
    @Test
    void zipWith() {
        Flux.just("a", "b")
            .zipWith(Flux.just("c", "d"))
            .subscribe(v -> log.info("value: {}", v));
        log.info("-----");
        Flux.just("a", "b")
            .zipWith(Flux.just("c", "d", "i"))
            .subscribe(v -> log.info("value: {}", v));
        log.info("-----");
        Flux.just("a", "b")
            .zipWith(Flux.just("c", "d"), (s1, s2) -> String.format("%s-%s", s1, s2))
            .subscribe(v -> log.info("value: {}", v));
    }

    /**
     * 从当前流中提取元素
     */
    @Test
    void take() {
        Flux.range(1, 1000).take(10).subscribe(v -> log.info("value: {}", v));
        log.info("-----");
        // 提取流中的最后 N 个元素
        Flux.range(1, 1000).takeLast(10).subscribe(v -> log.info("value: {}", v));
        log.info("-----");
        // 当 Predicate 返回 true 时才进行提取
        Flux.range(1, 1000).takeWhile(i -> i < 10).subscribe(v -> log.info("value: {}", v));
        log.info("-----");
        // 提取元素直到 Predicate 返回 true
        Flux.range(1, 1000).takeUntil(i -> i == 10).subscribe(v -> log.info("value: {}", v));
    }

    /**
     * 对流中包含的所有元素进行累积操作，得到一个包含计算结果的 Mono 序列
     */
    @Test
    void reduce() {
        Flux.range(1, 100).reduce((x, y) -> x + y)
            .subscribe(v -> log.info("value: {}", v));
        Flux.range(1, 100).reduceWith(() -> 100, (x, y) -> x + y)
            .subscribe(v -> log.info("value: {}", v));
    }

    /**
     * 把多个流合并成一个 Flux 序列
     */
    @Test
    void merge() {
        // 按照所有流中元素的实际产生顺序来合并
        Flux.merge(Flux.interval(Duration.ZERO, Duration.ofMillis(100)).take(5),
            Flux.interval(Duration.ofMillis(50), Duration.ofMillis(100)).take(5))
            .toStream()
            .forEach(v -> log.info("value: {}", v));
        log.info("-----");
        // 按照所有流被订阅的顺序，以流为单位进行合并
        Flux.mergeSequential(Flux.interval(Duration.ZERO, Duration.ofMillis(100)).take(5),
            Flux.interval(Duration.ofMillis(50), Duration.ofMillis(100)).take(5))
            .toStream()
            .forEach(v -> log.info("value: {}", v));
    }

    /**
     * 把流中的每个元素转换成一个流，再把所有流中的元素进行合并
     */
    @Test
    void flatMap() {
        Flux.just(5, 10)
            .flatMap(x -> Flux.interval(Duration.ofMillis(x * 10), Duration.ofMillis(100)).take(x))
            .toStream()
            .forEach(v -> log.info("value: {}", v));
    }

    /**
     * 也是把流中的每个元素转换成一个流，再把所有流进行合并
     */
    @Test
    void concatMap() {
        Flux.just(5, 10)
            // 对转换之后的流的订阅是动态进行的，而 flatMapSequential 在合并之前就已经订阅了所有的流
            .concatMap(x -> Flux.interval(Duration.ofMillis(x * 10), Duration.ofMillis(100)).take(x))
            .toStream()
            .forEach(v -> log.info("value: {}", v));
    }

    @Test
    void using() {
        Flux<String> flux = Flux.using(
            // 1、生成资源
            Channel::new,
            // 2、处理资源，返回一个 Flux<T>
            channel -> {
                channel.poll(1);
                if (System.currentTimeMillis() % 2 == 0) {
                    return Mono.just("Request");
                } else {
                    return Mono.error(new RuntimeException());
                }
            },
            // 3、资源 Flux 终止或取消的时候，用于清理资源 == finally
            Channel::close
        );
        flux
            .doOnNext(log::info)
            .doOnError(e -> log.error("Error"))
            .doOnCancel(() -> log.info("Cancel"))
            .doOnComplete(() -> log.info("Complete"))
            // onComplete or onError
            .doFinally(type -> log.info("finally type: {}", type))
            .subscribe();
    }

    /**
     * 把所有流中的最新产生的元素合并成一个新的元素，作为返回结果流中的元素
     */
    @Test
    void combineLatest() {
        // 只要其中任何一个流中产生了新的元素，合并操作就会被执行一次，结果流中就会产生新的元素
        Flux.combineLatest(
            Arrays::toString,
            // 0[0], 1[100], 2[200], 3[300], 4[400]
            Flux.interval(Duration.ofMillis(100)).take(5),
            // 0[50], 1[150], 2[250], 3[350], 4[450]
            Flux.interval(Duration.ofMillis(50), Duration.ofMillis(100)).take(5)
        ).toStream().forEach(v -> log.info("value: {}", v));
    }

    @Test
    void checkpoint() {
        Flux.just(1, 2).map(x -> 2 / x).checkpoint("test").subscribe(v -> log.info("value: {}", v));
        log.info("-----");
        // 有错误处理所以检查点无效
        Flux.just(1, 0).map(x -> 1 / x).checkpoint("test")
            .subscribe(v -> log.info("value: {}", v), e -> log.error("Error", e));
        log.info("-----");
        // 当出现错误时，检查点名称会出现在异常堆栈信息中
        Flux.just(1, 0).map(x -> 1 / x).checkpoint("test").subscribe(v -> log.info("value: {}", v));
    }

    @Test
    void log() {
        Flux.range(1, 3).log("Range").subscribe(v -> log.info("value: {}", v));
    }

    /**
     * 将几个操作符封装到一个函数式中，无共享状态
     */
    @Test
    void transform() {
        Function<Flux<String>, Flux<String>> filterAndMap =
            f -> f.filter(color -> !color.equals("orange"))
                .map(String::toUpperCase);

        Flux.fromIterable(Arrays.asList("blue", "green", "orange", "purple"))
            .doOnNext(log::info)
            // 可以将这段操作链打包封装后备用
            .transform(filterAndMap)
            .subscribe(d -> log.info("Subscriber to Transformed MapAndFilter: {}", d));
    }

    /**
     * 也能够将几个操作符封装到一个函数式中，有共享状态
     */
    @Test
    void compose() {
        AtomicInteger ai = new AtomicInteger();
        Function<Flux<String>, Flux<String>> filterAndMap = f -> {
            if (ai.incrementAndGet() == 1) {
                return f.filter(color -> !color.equals("orange"))
                    .map(String::toUpperCase);
            }
            return f.filter(color -> !color.equals("purple"))
                .map(String::toUpperCase);
        };

        // 这个函数式作用到原始序列上的话，是基于每一个订阅者的（on a per-subscriber basis）
        Flux<String> composedFlux =
            Flux.fromIterable(Arrays.asList("blue", "green", "orange", "purple"))
                .doOnNext(log::info)
                .compose(filterAndMap);
                //.transform(filterAndMap);

        // 意味着它对每一个 subscription 可以生成不同的操作链（通过维护一些状态值）
        log.info("ai: "+ai.get());
        composedFlux.subscribe(d -> log.info("Subscriber 1 to Composed MapAndFilter: {}", d));
        log.info("ai: "+ai.get());
        composedFlux.subscribe(d -> log.info("Subscriber 2 to Composed MapAndFilter: {}", d));
        log.info("ai: "+ai.get());
    }

    @Test
    void connect() throws InterruptedException {
        Flux<Integer> source = Flux.range(1, 3)
            .doOnSubscribe(s -> log.info("subscribed to source"));

        ConnectableFlux<Integer> co = source.publish();

        co.subscribe(v -> log.info("s1 value: {}", v), e -> {
        }, () -> {
        });
        co.subscribe(v -> log.info("s2 value: {}", v), e -> {
        }, () -> {
        });

        log.info("done subscribing");
        Thread.sleep(500);
        log.info("will now connect");

        // 触发对上游源的订阅
        co.connect();
    }

    @Test
    void autoConnect() throws InterruptedException {
        Flux<Integer> source = Flux.range(1, 3)
            .doOnSubscribe(s -> log.info("subscribed to source"));

        // 在有 n 个订阅的时候自动触发
        Flux<Integer> autoCo = source.publish().autoConnect(2);

        autoCo.subscribe(v -> log.info("s1 value: {}", v), e -> {
        }, () -> {
        });
        log.info("subscribed first");
        Thread.sleep(500);
        log.info("subscribing second");
        autoCo.subscribe(v -> log.info("s2 value: {}", v), e -> {
        }, () -> {
        });
    }

    @Test
    void hot() throws InterruptedException {
        final Flux<Long> source = Flux.interval(Duration.ofMillis(1000))
            .take(10)
            // 把一个 Flux 对象转换成 ConnectableFlux 对象
            .publish()
            // 当 ConnectableFlux 对象有一个订阅者时就开始产生消息
            .autoConnect();
        // 第一个订阅者开始产生消息
        source.subscribe();
        Thread.sleep(5000);
        // 第二个订阅者此时只能获得到该序列中的后 5 个元素
        source
            .toStream()
            .forEach(v -> log.info("value: {}", v));
    }

    @Test
    void parallel() {
        Flux.range(1, 10)
            // 本身并不会进行并行处理，而是将负载划分到多个"轨道（rails）"上 （默认情况下，轨道个数与 CPU 核数相等）
            .parallel(2)
            .subscribe(i -> log.info("--> " + i));
        log.info("-----");
        Flux.range(1, 10)
            .parallel(2)
            // 使用 runOn(Scheduler) 配置 ParallelFlux 并行地执行每一个轨道
            .runOn(Schedulers.parallel())
            .subscribe(i -> log.info("--> " + i));
        // 注意对 ParallelFlux 使用一个 Subscriber 而不是基于 lambda 进行订阅（subscribe()）的时候，sequential() 会被自动应用
        // 注意 subscribe(Subscriber<T>) 会合并所有的执行轨道，而 subscribe(Consumer<T>) 会在所有轨道上运行
    }

    @Test
    void backpressure() {
        List<Integer> elements = new ArrayList<>();
        Flux.just(1, 2, 3, 4, 5)
            .log()
            .subscribe(new BaseSubscriber<Integer>() {
                int onNextAmount;

                @Override
                protected void hookOnSubscribe(Subscription subscription) {
                    request(2);
                }

                @Override
                protected void hookOnNext(Integer value) {
                    elements.add(value);
                    onNextAmount++;
                    if (onNextAmount % 2 == 0) {
                        request(2);
                    }
                }

                @Override
                protected void hookOnComplete() {
                    log.info("Complete");
                }

                @Override
                protected void hookOnError(Throwable throwable) {
                    log.error("Error", throwable);
                }

                @Override
                protected void hookOnCancel() {
                    log.info("Cancel");
                }
            });
        log.info("elements: {}", elements);
    }

    @Test
    public void publish() throws InterruptedException {
        Flux<Long> flux = Flux.interval(Duration.ofMillis(500))
            .publish(3)
            .autoConnect(3);
            //.refCount(3, Duration.ofMillis(2000));

        flux.subscribe(v -> log.info("S1: {}", v));
        TimeUnit.SECONDS.sleep(1);
        flux
            .take(3)
            .doFinally(signalType -> log.info("S2 signal type: {}", signalType))
            .subscribe(v -> log.info("S2: {}", v));
        flux.subscribe(v -> log.info("S3: {}", v));
        log.info("start connect..");
        TimeUnit.SECONDS.sleep(3);
        log.info("t5");
        flux
            .doFinally(signalType -> log.info("S4 signal type: {}", signalType))
            .subscribe(v -> log.info("S4: {}", v));
        log.info("t6");
        TimeUnit.SECONDS.sleep(3);
    }

}
