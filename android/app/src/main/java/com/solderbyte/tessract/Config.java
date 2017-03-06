package com.solderbyte.tessract;

import java.util.HashMap;

public class Config {
    public static final String PACKAGE = "com.solderbyte.tessract";

    public static final String DEVICE_NAME = PACKAGE + ".device.name";
    public static final String DEVICE_ADDRESS = PACKAGE + ".device.address";
    public static final String DEVICE_STATE = PACKAGE + ".device.state";

    public static final String INTENT = PACKAGE + ".intent";

    public static final String INTENT_BLUETOOTH = INTENT + ".bluetooth";
    public static final String INTENT_BLUETOOTH_DEVICE = INTENT_BLUETOOTH + ".device";
    public static final String INTENT_BLUETOOTH_ENABLE = INTENT_BLUETOOTH + ".enable";
    public static final String INTENT_BLUETOOTH_ENABLED = INTENT_BLUETOOTH + ".enabled";
    public static final String INTENT_BLUETOOTH_DISABLE = INTENT_BLUETOOTH + ".disable";
    public static final String INTENT_BLUETOOTH_DISABLED = INTENT_BLUETOOTH + ".disabled";
    public static final String INTENT_BLUETOOTH_SCAN = INTENT_BLUETOOTH + ".scan";
    public static final String INTENT_BLUETOOTH_SCANNED = INTENT_BLUETOOTH + ".scanned";
    public static final String INTENT_BLUETOOTH_CONNECT = INTENT_BLUETOOTH + ".connect";
    public static final String INTENT_BLUETOOTH_CONNECTING = INTENT_BLUETOOTH + ".connecting";
    public static final String INTENT_BLUETOOTH_CONNECTED = INTENT_BLUETOOTH + ".connected";
    public static final String INTENT_BLUETOOTH_DISCONNECT = INTENT_BLUETOOTH + ".disconnect";
    public static final String INTENT_BLUETOOTH_DISCONNECTED = INTENT_BLUETOOTH + ".disconnected";
    public static final String INTENT_EXTRA_DATA = INTENT + ".extra.data";
    public static final String INTENT_EXTRA_MSG = INTENT + ".extra.message";
    public static final String INTENT_SERVICE = INTENT + ".service";
    public static final String INTENT_SERVICE_START = INTENT_SERVICE + ".start";
    public static final String INTENT_SERVICE_STOP = INTENT_SERVICE + ".stop";
    public static final String INTENT_SHUTDOWN = INTENT_SERVICE + ".shutdown";

    public static final String INTENT_USER = INTENT + ".user";

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
