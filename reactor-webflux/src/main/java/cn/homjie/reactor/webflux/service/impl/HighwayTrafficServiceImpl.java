package cn.homjie.reactor.webflux.service.impl;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

import cn.homjie.reactor.webflux.model.Vehicle;
import cn.homjie.reactor.webflux.service.HighwayTrafficService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.RandomStringGenerator;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * @author jiehong.jh
 * @date 2019-03-08
 */
@Slf4j
@Service
public class HighwayTrafficServiceImpl implements HighwayTrafficService {

    /**
     * 最大循环次数
     */
    private static final int MAX_LOOP_TIMES = 30000;
    /**
     * 最大循环时间
     */
    private static final int MAX_LOOP_MILLIS = 30000;
    private static DecimalFormat plateFormatNumber = new DecimalFormat("0000");
    private static String[] COLORS = {"Blue", "White", "Silver", "Black", "Metalic Green", "Orange", "Yellow"};
    private static String[] GAS_TYPE = {"Diesel", "Gasoline", "Gas", "Eletric", "Alcohol"};

    @Override
    public Flux<Vehicle> flowTraffic() {
        LocalDateTime startTime = LocalDateTime.now();

        return Flux.<Vehicle>create(fluxSink -> {
            boolean inFrameTime = true;
            int index = 1;
            while (index <= MAX_LOOP_TIMES && inFrameTime && !fluxSink.isCancelled()) {
                fluxSink.next(HighwayUtilities.simulateTraffic(index));
                index++;

                long timeMinutesHighwayOpened = startTime.until(LocalDateTime.now(), ChronoUnit.MILLIS);
                if (timeMinutesHighwayOpened > MAX_LOOP_MILLIS) {
                    log.info("TrafficSimulator finish --> With timer");
                    inFrameTime = false;
                }
            }
        }).share();
    }

    private static class HighwayUtilities {

        private static Vehicle simulateTraffic(int id) {
            RandomStringGenerator rndStringGen = new RandomStringGenerator.Builder().withinRange('A', 'Z').build();

            String carPlateNumber = rndStringGen.generate(3).toUpperCase() + " "
                + plateFormatNumber.format(ThreadLocalRandom.current().nextInt(0, 9999));

            Long weight = ThreadLocalRandom.current().nextLong(250L, 4500L);
            Integer speed = ThreadLocalRandom.current().nextInt(60, 175);

            String color = COLORS[ThreadLocalRandom.current().nextInt(0, 6)];
            Integer modelYear = ThreadLocalRandom.current().nextInt(1975, 2018);
            String gasType = GAS_TYPE[ThreadLocalRandom.current().nextInt(0, 4)];

            return new Vehicle(id, carPlateNumber, weight, speed, color, modelYear, gasType);
        }

    }
}
