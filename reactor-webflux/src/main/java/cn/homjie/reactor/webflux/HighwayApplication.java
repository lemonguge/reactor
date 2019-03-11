package cn.homjie.reactor.webflux;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.config.EnableWebFlux;

/**
 * @author jiehong.jh
 * @date 2019-03-08
 */
@SpringBootApplication
@EnableWebFlux
public class HighwayApplication {

    public static void main(String[] args) {
        SpringApplication.run(HighwayApplication.class, args);
    }
}
