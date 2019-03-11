package cn.homjie.reactor.intro;

import java.util.List;

/**
 * @author jiehong.jh
 * @date 2019-03-09
 */
public interface EventListener {

    void onDataChunk(List<String> chunk);

    default void processComplete() {}

    default void processError(Throwable e) {}
}
