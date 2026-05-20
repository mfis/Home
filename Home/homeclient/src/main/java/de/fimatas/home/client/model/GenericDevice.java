package de.fimatas.home.client.model;

import de.fimatas.home.library.domain.model.Place;
import de.fimatas.home.library.homematic.model.Device;
import de.fimatas.home.library.model.PersistentCacheKey;

public class GenericDevice {

    private final Device device;

    private final PersistentCacheKey persistentCacheKey;

    private GenericDevice(Device device, PersistentCacheKey persistentCacheKey) {
        this.device = device;
        this.persistentCacheKey = persistentCacheKey;
    }

    public static GenericDevice from(Device device) {
        return new GenericDevice(device, null);
    }

    public static GenericDevice from(PersistentCacheKey persistentCacheKey) {
        return new GenericDevice(null, persistentCacheKey);
    }

    public String name() {
        return device != null ? device.name() : persistentCacheKey.name();
    }

    public Place place() {
        return device != null ? device.getPlace() : persistentCacheKey.getPlace();
    }

    public String typeName() {
        return device != null ? device.getType().getTypeName() : persistentCacheKey.name();
    }

    public String description() {
        return device != null ? device.getDescription() : persistentCacheKey.getDescription();
    }
}
