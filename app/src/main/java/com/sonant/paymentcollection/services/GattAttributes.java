package com.sonant.paymentcollection.services;



import java.util.HashMap;
import java.util.UUID;

/**
 * Gatt profile attributes needed for the actual project; i.e heart rate service,
 * battery service and device information service
 */

public class GattAttributes  {

    public static HashMap<UUID, String> gattAttributes = new HashMap<>();

    public static UUID HEART_RATE_SERVICE_UUID = convertFromInteger(0xFFF0);
    public static UUID HEART_RATE_MEASUREMENT_CHAR_UUID = convertFromInteger(0xFFF4);
    public static UUID HEART_RATE_CONTROL_POINT_CHAR_UUID = convertFromInteger(0xFFF5);
    public static UUID BATTERY_SERVICE_UUID = convertFromInteger(0x180F);
    public static UUID BATTERY_LEVEL_UUID = convertFromInteger(0x2A19);
    public static UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = convertFromInteger(0x2902);

// FFF0 Service UUID
    //FFF4 Char UUID
    //FFF5  Char UUID 2

    //
    /**
     * convert from an integer to UUID.
     * @param i integer input
     * @return UUID
     */
     public static UUID convertFromInteger(int i) {
        final long MSB = 0x0000000000001000L;
        final long LSB = 0x800000805f9b34fbL;
        long value = i & 0xFFFFFFFF;
        return new UUID(MSB | (value << 32), LSB);
    }

    static {
         gattAttributes.put(HEART_RATE_SERVICE_UUID, "Heart rate service");
         gattAttributes.put(HEART_RATE_MEASUREMENT_CHAR_UUID, "Heart rate measurement char");
         gattAttributes.put(HEART_RATE_CONTROL_POINT_CHAR_UUID, "Heart rate control point");
         gattAttributes.put(BATTERY_SERVICE_UUID, "Battery service");
         gattAttributes.put(BATTERY_LEVEL_UUID, "Battery level");
         gattAttributes.put(CLIENT_CHARACTERISTIC_CONFIG_UUID, "Client char config");
    }
}
