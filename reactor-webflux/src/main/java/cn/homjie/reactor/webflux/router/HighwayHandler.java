package cn.homjie.reactor.webflux.router;

import cn.homjie.reactor.webflux.model.Vehicle;
import cn.homjie.reactor.webflux.service.HighwayTrafficService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * @author jiehong.jh
 * @date 2019-03-08
 */
@Component
public class HighwayHandler {

    @Autowired
    private HighwayTrafficService highwayTraffic;

    public Mono<ServerResponse> vehicleDetected(ServerRequest request) {
        return ServerResponse.ok()
            .contentType(MediaType.APPLICATION_STREAM_JSON)
            .body(highwayTraffic.flowTraffic(), Vehicle.class);
    }
}
