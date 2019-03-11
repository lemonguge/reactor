package cn.homjie.reactor.webflux.router;

import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * @author jiehong.jh
 * @date 2019-03-08
 */
@Component
public class HighwayRouter {

    @Bean
    public RouterFunction<ServerResponse> route(HighwayHandler highwayHandler) {
        return RouterFunctions
            .route(RequestPredicates.GET("/vehicles")
                    .and(RequestPredicates.accept(MediaType.APPLICATION_STREAM_JSON)),
                highwayHandler::vehicleDetected);
    }
}
