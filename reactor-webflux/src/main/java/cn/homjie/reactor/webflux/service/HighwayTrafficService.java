package cn.homjie.reactor.webflux.service;

import cn.homjie.reactor.webflux.model.Vehicle;
import reactor.core.publisher.Flux;

/**
 * @author jiehong.jh
 * @date 2019-03-08
 */
public interface HighwayTrafficService {

    Flux<Vehicle> flowTraffic();
}
