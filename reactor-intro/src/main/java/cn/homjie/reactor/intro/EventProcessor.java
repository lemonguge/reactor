package cn.homjie.reactor.intro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author jiehong.jh
 * @date 2019-03-09
 */
public class EventProcessor {

    private static final int ALPHABET_SIZE = 26;
    private List<EventListener> listeners = new ArrayList<>();

    private boolean enableError;

    /**
     * @param letterNumber from 0 to {@link #ALPHABET_SIZE}
     * @return
     */
    public static String alphabet(int letterNumber) {
        if (letterNumber < 1 || letterNumber > ALPHABET_SIZE) {
            return null;
        }
        int letterIndexAscii = 'A' + letterNumber - 1;
        return String.valueOf((char)letterIndexAscii);
    }

    public EventProcessor register(EventListener listener) {
        listeners.add(listener);
        return this;
    }

    public void handle(List<String> list) {
        for (EventListener listener : listeners) {
            try {
                listener.onDataChunk(list);
                if (enableError && System.currentTimeMillis() % 2 == 0) {
                    throw new RuntimeException("Mock");
                }
                listener.processComplete();
            } catch (Throwable e) {
                listener.processError(e);
            }
        }
    }

    public EventProcessor setEnableError(boolean enableError) {
        this.enableError = enableError;
        return this;
    }

    public List<String> request(int n) {
        if (n <= 0) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>(n);
        int loop = 0;
        while (loop++ < n) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            int a = random.nextInt(1, 27);
            int b = random.nextInt(1, 27);
            int c = random.nextInt(1, 27);
            list.add(alphabet(a) + alphabet(b) + alphabet(c));
        }
        return list;
    }
}
