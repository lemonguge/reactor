package cn.homjie.reactor.intro;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import static cn.homjie.reactor.intro.EventProcessor.alphabet;

/**
 * @author jiehong.jh
 * @date 2019-03-09
 */
@Slf4j
public class SinkTest {

    @Test
    void create() throws InterruptedException {
        EventProcessor processor = new EventProcessor();
        Flux<String> bridge = Flux.create(sink ->
            processor.register(
                new EventListener() {
                    @Override
                    public void onDataChunk(List<String> chunk) {
                        chunk.forEach(sink::next);
                    }

                    @Override
                    public void processComplete() {
                        sink.complete();
                    }
                })
        );

        bridge
            // 将使用 parallel 调度器
            .delayElements(Duration.ofMillis(100))
            .doOnNext(log::info)
            .doOnComplete(() -> log.info("Done"))
            .subscribe();

        List<String> list = processor.request(5);
        log.info("processor handle start");
        processor.handle(list);
        log.info("processor handle after");
        TimeUnit.SECONDS.sleep(1);
    }

    @Test
    void push() {
        EventProcessor processor = new EventProcessor();
        processor.setEnableError(true);
        // 每次只有一个生成线程可以调用 next，complete 或 error
        Flux<String> bridge = Flux.push(sink ->
            processor.register(
                new EventListener() {
                    @Override
                    public void onDataChunk(List<String> chunk) {
                        chunk.forEach(sink::next);
                    }

                    @Override
                    public void processComplete() {
                        sink.complete();
                    }

                    @Override
                    public void processError(Throwable e) {
                        sink.error(e);
                    }
                })
        );

        bridge
            .doOnNext(log::info)
            .doOnError(e -> log.error("Error", e))
            .doOnComplete(() -> log.info("Done"))
            .subscribe();

        List<String> list = processor.request(5);
        log.info("processor handle start");
        processor.handle(list);
        log.info("processor handle after");
    }

    @Test
    void pull() {
        // 初始化请求个数
        final int iRequest = 5;
        EventProcessor processor = new EventProcessor();
        // create 可以用于 push 或 pull 模式
        Flux<String> bridge = Flux.create(sink -> {
            // 2、后续异步到达的 chunk 也会被发送给 sink
            processor.register(chunk -> {
                log.info("onDataChunk");
                chunk.forEach(sink::next);
            });

            // 1、当有请求的时候开始处理
            sink.onRequest(n -> {
                if (n != Long.MAX_VALUE) {
                    log.info("onRequest: {}", n);
                }
                processor.request(iRequest).forEach(sink::next);
            });
        });

        Disposable disposable = bridge
            .log()
            .doOnNext(log::info)
            .doOnError(e -> log.error("Error", e))
            .doOnCancel(() -> log.info("Cancel"))
            .doOnComplete(() -> log.info("Done"))
            .subscribe();

        List<String> list = processor.request(3);
        log.info("processor handle start");
        processor.handle(list);
        log.info("processor handle after");
        disposable.dispose();
    }

    @Test
    void cancel() {
        Channel channel = new Channel();
        Flux<String> bridge = Flux.create(sink ->
            sink.onRequest(channel::poll)
                // 在取消时被调用，会先于 onDispose 执行
                .onCancel(channel::cancel)
                // 有错误出现或被取消的时候执行清理
                .onDispose(channel::close)
        );

        Disposable disposable = bridge.log().subscribe();
        disposable.dispose();
    }

    @Test
    void handle() {
        Flux<String> alphabet = Flux.just(-1, 13, 9, 18, 30, 20)
            .log()
            .handle((i, sink) -> {
                String letter = alphabet(i);
                if (letter != null) {
                    sink.next(letter);
                }
            });

        alphabet.subscribe(log::info);
    }
}
