package com.solderbyte.tessract;

import java.util.HashMap;

public class Config {
    public static final String PACKAGE = "com.solderbyte.tessract";

    public static final String DEVICE_NAME = PACKAGE + ".device.name";
    public static final String DEVICE_ADDRESS = PACKAGE + ".device.address";
    public static final String DEVICE_STATE = PACKAGE + ".device.state";
    public static final String DEVICE_COLOR = PACKAGE + ".device.color";

    public static final String INTENT = PACKAGE + ".intent";

    public static final String INTENT_BLUETOOTH = INTENT + ".bluetooth";
    public static final String INTENT_BLUETOOTH_DEVICE = INTENT_BLUETOOTH + ".device";
    public static final String INTENT_BLUETOOTH_ENABLE = INTENT_BLUETOOTH + ".enable";
    public static final String INTENT_BLUETOOTH_ENABLED = INTENT_BLUETOOTH + ".enabled";
    public static final String INTENT_BLUETOOTH_DISABLE = INTENT_BLUETOOTH + ".disable";
    public static final String INTENT_BLUETOOTH_DISABLED = INTENT_BLUETOOTH + ".disabled";
    public static final String INTENT_BLUETOOTH_STATUS = INTENT_BLUETOOTH + ".status";
    public static final String INTENT_BLUETOOTH_SCAN = INTENT_BLUETOOTH + ".scan";
    public static final String INTENT_BLUETOOTH_SCANNED = INTENT_BLUETOOTH + ".scanned";
    public static final String INTENT_BLUETOOTH_CONNECT = INTENT_BLUETOOTH + ".connect";
    public static final String INTENT_BLUETOOTH_CONNECTING = INTENT_BLUETOOTH + ".connecting";
    public static final String INTENT_BLUETOOTH_CONNECTED = INTENT_BLUETOOTH + ".connected";
    public static final String INTENT_BLUETOOTH_DISCONNECT = INTENT_BLUETOOTH + ".disconnect";
    public static final String INTENT_BLUETOOTH_DISCONNECTED = INTENT_BLUETOOTH + ".disconnected";
    public static final String INTENT_BLUETOOTH_CHARACTERISTIC = INTENT_BLUETOOTH + ".characteristic";
    public static final String INTENT_BLUETOOTH_WRITE = INTENT_BLUETOOTH + ".write";

    public static final String INTENT_NOTIFICATION = INTENT + ".notification";

    public static final String INTENT_APPLICATION = INTENT + ".application";
    public static final String INTENT_APPLICATION_LIST = INTENT_APPLICATION + ".list";
    public static final String INTENT_APPLICATION_LISTED = INTENT_APPLICATION + ".listed";
    public static final String INTENT_APPLICATION_SAVED = INTENT_APPLICATION + ".saved";

    public static final String INTENT_EXTRA_DATA = INTENT + ".extra.data";
    public static final String INTENT_EXTRA_MSG = INTENT + ".extra.message";

    public static final String INTENT_SERVICE = INTENT + ".service";
    public static final String INTENT_SHUTDOWN = INTENT_SERVICE + ".shutdown";
    public static final String INTENT_SERVICE_BLUETOOTH = INTENT_SERVICE + ".bluetooth";
    public static final String INTENT_SERVICE_TESSRACT = INTENT_SERVICE + ".tessract";
    public static final String INTENT_SERVICE_NOTIFICATION = INTENT_SERVICE + ".notification";
    public static final String INTENT_SERVICE_BLUETOOTH_STARTED = INTENT_SERVICE_BLUETOOTH + ".started";
    public static final String INTENT_SERVICE_TESSRACT_STARTED = INTENT_SERVICE_TESSRACT + ".started";
    public static final String INTENT_SERVICE_NOTIFICATION_STARTED = INTENT_SERVICE_NOTIFICATION + ".started";

    public static final String INTENT_USER = INTENT + ".user";

    public static final String JSON = PACKAGE + ".json";
    public static final String JSON_APPLICATIONS = "applications";
    public static final String JSON_PACKAGE_NAME = "packageName";
    public static final String JSON_APPLICATION_NAME = "applicationName";
    public static final String JSON_APPLICATION_COLOR = "applicationColor";
    public static final String JSON_DEVICE_NAME = "deviceName";
    public static final String JSON_DEVICE_ADDRESS = "deviceAddress";
    public static final String JSON_DEVICE_TYPE = "deviceType";
    public static final String JSON_DEVICE_BOND = "deviceBond";

    public static final int COLOR_RED = 0;
    public static final int COLOR_GREEN = 1;
    public static final int COLOR_BLUE = 2;
    public static final int COLOR_DEFAULT = -12627531;

    public static HashMap<Integer, String> SERVICE_FLAGS = new HashMap<Integer, String>() {{
        put(0, "START_STICKY_COMPATIBILITY");
        put(1, "START_FLAG_REDELIVERY, START_STICKY");
        put(2, "START_FLAG_RETRY, START_NOT_STICKY");
        put(3, "START_REDELIVER_INTENT");
        put(15, "START_CONTINUATION_MASK");
    }};

    public static HashMap<Integer, String> DEVICE_BONDS = new HashMap<Integer, String>() {{
        put(10, "BOND_NONE");
        put(11, "BOND_BONDING");
        put(12, "BOND_BONDED");
    }};
}
