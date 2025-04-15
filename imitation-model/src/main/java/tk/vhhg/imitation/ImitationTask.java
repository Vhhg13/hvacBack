package tk.vhhg.imitation;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.concurrent.atomic.AtomicInteger;

class ImitationTask implements Runnable {

    private static final int HEAT_CAPACITY = 1000;
    private static final float DENSITY = 1.2F;

    record Params(int id, String heaters, String coolers, float k, float out, float volume, String thermostat) {}

    public volatile Params params;


    private final MqttClient client;
    private final AtomicInteger temperature;
    private volatile float heating = 0;
    private volatile float cooling = 0;


    ImitationTask(ImitatedRoomDto room, String brokerUrl) throws MqttException {
        params = new Params(
                room.getId(),
                room.getHeaters(),
                room.getCoolers(),
                room.getK(),
                room.getOut(),
                room.getVolume(),
                room.getThermostat()
        );
        client = new MqttClient(brokerUrl, "room" + room.getId());
        client.connect();
        temperature = new AtomicInteger(Float.floatToIntBits(room.getOut()));
        client.subscribe(room.getHeaters(), 2, (topic, message) -> {
            heating = Float.parseFloat(new String(message.getPayload()));
        });
        client.subscribe(room.getCoolers(), 2, (topic, message) -> {
            cooling = Float.parseFloat(new String(message.getPayload()));
        });

    }

    public void tune(ImitatedRoomDto room) throws MqttException {
        if (!room.getHeaters().equals(params.heaters)) {
            client.unsubscribe(params.heaters);
            client.subscribe(room.getHeaters(), 2, (topic, message) -> {
                heating = Float.parseFloat(new String(message.getPayload()));
            });
        }
        if (!room.getCoolers().equals(params.coolers)) {
            client.unsubscribe(params.coolers);
            client.subscribe(room.getCoolers(), 2, (topic, message) -> {
                cooling = Float.parseFloat(new String(message.getPayload()));
            });
        }
        params = new Params(
                room.getId(),
                room.getHeaters(),
                room.getCoolers(),
                room.getK(),
                room.getOut(),
                room.getVolume(),
                room.getThermostat()
        );
    }

    @Override
    public void run() {
        float temp,  newTemp;
        temp = Float.intBitsToFloat(temperature.get());
        newTemp = temp + (heating - cooling) / (HEAT_CAPACITY * DENSITY * params.volume) - params.k * (temp - params.out);
//        System.out.println(temp + " " + heating + " " + cooling);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (!Double.isFinite(newTemp)) return;
        temperature.set(Float.floatToIntBits(newTemp));
//        System.out.println("set");
        try {
            client.publish(params.thermostat, String.valueOf(newTemp).getBytes(), 2, true);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() throws MqttException {
        client.disconnect();
        client.close();
    }
}
