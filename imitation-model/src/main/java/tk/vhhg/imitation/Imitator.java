package tk.vhhg.imitation;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;

class Imitator {
    private final ScheduledExecutorService executorService =
            Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

    private final String brokerUrl;

    private static final long IMITATION_DELAY = 1000;
    private static final Random random = new Random();

    private final Map<Integer, ImitationTask> tasks = new HashMap<>();
    private final Map<Integer, ScheduledFuture<?>> futures = new HashMap<>();

    public Imitator(String brokerUrl) {
        this.brokerUrl = brokerUrl;
    }

    public void imitate(ImitatedRoomDto room) throws MqttException {
        int id = room.getId();
        if (tasks.containsKey(id)) {
            tasks.get(id).tune(room);
        } else {
            ImitationTask task = new ImitationTask(room, brokerUrl);
            ScheduledFuture<?> future = executorService.scheduleAtFixedRate(
                    task,
                    random.nextInt(0, 2000),
                    IMITATION_DELAY,
                    TimeUnit.MILLISECONDS
            );
            // TODO: Maybe switch to ConcurrentHashMap, or use a lock idk
            tasks.put(id, task);
            futures.put(id, future);
        }
    }

    public void drop(int id) throws MqttException {
        ScheduledFuture<?> future = futures.remove(id);
        if (future != null) future.cancel(true);
        ImitationTask task = tasks.remove(id);
        if (task != null) task.close();
    }
}
