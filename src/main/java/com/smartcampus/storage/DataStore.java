package com.smartcampus.storage;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton in-memory store. Using ConcurrentHashMap because Jersey creates
 * a new resource instance per request, so multiple threads hit this shared
 * state at the same time.
 */
public class DataStore {

    private static final DataStore INSTANCE = new DataStore();

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    private final Map<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

    private DataStore() {
        seed();
    }

    public static DataStore get() {
        return INSTANCE;
    }

    private void seed() {
        Room lib = new Room("LIB-301", "Library Quiet Study", 40);
        Room lab = new Room("LAB-112", "CS Lab 112", 25);
        Room hall = new Room("HALL-A", "Main Lecture Hall A", 200);
        rooms.put(lib.getId(), lib);
        rooms.put(lab.getId(), lab);
        rooms.put(hall.getId(), hall);

        Sensor temp1 = new Sensor("TEMP-001", "Temperature", "ACTIVE", 21.5, "LIB-301");
        Sensor co2 = new Sensor("CO2-014", "CO2", "ACTIVE", 420.0, "LIB-301");
        Sensor occ = new Sensor("OCC-207", "Occupancy", "MAINTENANCE", 0.0, "LAB-112");
        sensors.put(temp1.getId(), temp1);
        sensors.put(co2.getId(), co2);
        sensors.put(occ.getId(), occ);

        lib.getSensorIds().add(temp1.getId());
        lib.getSensorIds().add(co2.getId());
        lab.getSensorIds().add(occ.getId());

        readings.put(temp1.getId(), new ArrayList<>(List.of(
                new SensorReading(UUID.randomUUID().toString(), System.currentTimeMillis() - 600_000, 21.2),
                new SensorReading(UUID.randomUUID().toString(), System.currentTimeMillis() - 300_000, 21.5)
        )));
        readings.put(co2.getId(), new ArrayList<>(List.of(
                new SensorReading(UUID.randomUUID().toString(), System.currentTimeMillis() - 120_000, 418.0),
                new SensorReading(UUID.randomUUID().toString(), System.currentTimeMillis() - 60_000, 420.0)
        )));
        readings.put(occ.getId(), new ArrayList<>());
    }

    // Room ops
    public List<Room> allRooms() {
        return new ArrayList<>(rooms.values());
    }

    public Room findRoom(String id) {
        return rooms.get(id);
    }

    public Room addRoom(Room room) {
        rooms.put(room.getId(), room);
        return room;
    }

    public Room removeRoom(String id) {
        return rooms.remove(id);
    }

    // Sensor ops
    public List<Sensor> allSensors() {
        return new ArrayList<>(sensors.values());
    }

    public Sensor findSensor(String id) {
        return sensors.get(id);
    }

    public Sensor addSensor(Sensor sensor) {
        sensors.put(sensor.getId(), sensor);
        readings.computeIfAbsent(sensor.getId(), k -> new ArrayList<>());
        Room parent = rooms.get(sensor.getRoomId());
        if (parent != null && !parent.getSensorIds().contains(sensor.getId())) {
            parent.getSensorIds().add(sensor.getId());
        }
        return sensor;
    }

    // Reading ops, guarded with synchronized on the per-sensor list so
    // concurrent POSTs to the same sensor don't clobber each other.
    public List<SensorReading> readingsFor(String sensorId) {
        List<SensorReading> list = readings.get(sensorId);
        if (list == null) {
            return Collections.emptyList();
        }
        synchronized (list) {
            return new ArrayList<>(list);
        }
    }

    public void appendReading(String sensorId, SensorReading reading) {
        List<SensorReading> list = readings.computeIfAbsent(sensorId, k -> new ArrayList<>());
        synchronized (list) {
            list.add(reading);
        }
    }
}
