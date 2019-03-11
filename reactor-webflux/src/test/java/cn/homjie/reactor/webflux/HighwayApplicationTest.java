package cn.homjie.reactor.webflux;

import java.time.Duration;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import cn.homjie.reactor.webflux.model.Vehicle;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Subscription;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.scheduler.Schedulers;

/**
 * @author jiehong.jh
 * @date 2019-03-08
 */
@Slf4j
public class HighwayApplicationTest {

    private WebClient webClient;

    @Before
    public void setUp() {
        webClient = WebClient.builder()
            .baseUrl("http://0.0.0.0:8080")
            .build();
    }

    @Test
    public void vehicleDetected() {
        AtomicInteger counter = new AtomicInteger(0);
        Disposable disposable = webClient.get()
            .uri("/vehicles")
            .accept(MediaType.APPLICATION_STREAM_JSON)
            .exchange()
            .publishOn(Schedulers.single())
            .flatMapMany(response -> response.bodyToFlux(Vehicle.class))
            .delayElements(Duration.ofMillis(1000))
            .subscribe(s -> log.info("{} >>>>>>>>>>>> {}", counter.incrementAndGet(), s),
                err -> log.error("Error on Vehicle Stream", err),
                () -> log.info("Vehicle stream stoped!"));

        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
        disposable.dispose();
    }

    @Test
    public void vehicleHigherThen120Detected() {
        AtomicInteger counter = new AtomicInteger(0);
        Disposable disposable = webClient.get()
            .uri("/vehicles")
            .accept(MediaType.APPLICATION_STREAM_JSON)
            .exchange()
            .flatMapMany(response -> response.bodyToFlux(Vehicle.class))
            .filter(v -> v.getSpeed() > 120)
            .delayElements(Duration.ofMillis(250))
            .subscribe(s -> log.info("{} >> 120Km+ >> {}", counter.incrementAndGet(), s),
                err -> log.error("Error on Vehicle Stream", err),
                () -> log.info("Vehicle stream stoped!"));

        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
        disposable.dispose();
    }

    @Test
    public void vehicleLoop() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        webClient.get()
            .uri("/vehicles")
            .accept(MediaType.APPLICATION_STREAM_JSON)
            .exchange()
            .flatMapMany(response -> response.bodyToFlux(Vehicle.class))
            .subscribeOn(Schedulers.parallel())
            .subscribe(new BaseSubscriber<Vehicle>() {

                @Override
                protected void hookOnSubscribe(Subscription subscription) {
                    log.info("OnSubscribe");
                    request(10);
                }

                @Override
                protected void hookOnNext(Vehicle value) {
                    log.info("{} >>>>>>>>>>>> {}", counter.incrementAndGet(), value);
                    if (counter.get() == 10) {
                        cancel();
                    }
                }

                @Override
                protected void hookOnComplete() {
                    log.info("OnComplete");
                }

                @Override
                protected void hookOnError(Throwable throwable) {
                    log.error("OnError", throwable);
                }

                @Override
                protected void hookOnCancel() {
                    log.info("OnCancel");
                }
            });

        TimeUnit.MINUTES.sleep(1);
    }
}