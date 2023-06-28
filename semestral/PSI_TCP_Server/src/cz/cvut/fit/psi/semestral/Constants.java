package cz.cvut.fit.psi.semestral;

import java.util.HashMap;
import java.util.Map;

public class Constants {
    public static final String HOST = "127.0.0.1";
    public static final Integer PORT = 65432;

    /* Timeouts */
    public static final Integer TIMEOUT = 1000; //ms
    public static final Integer TIMEOUT_RECHARGING = 5000; //ms

    /* Client messages */
    public static final String MSG_ENDING = "\u0007\b"; // "\\a\b" doesnt work, so needed to write it in this form
    public static final String FULL_POWER = "FULL POWER";
    public static final String RECHARGING = "RECHARGING";

    /* Server messages */
    public static final String SERVER_MOVE = "102 MOVE";
    public static final String SERVER_TURN_LEFT = "103 TURN LEFT";
    public static final String SERVER_TURN_RIGHT = "104 TURN RIGHT";
    public static final String SERVER_PICK_UP = "105 GET MESSAGE";
    public static final String SERVER_LOGOUT = "106 LOGOUT";
    public static final String SERVER_KEY_REQUEST = "107 KEY REQUEST";
    public static final String SERVER_OK = "200 OK";
    public static final String SERVER_LOGIN_FAILED = "300 LOGIN FAILED";
    public static final String SERVER_SYNTAX_ERROR = "301 SYNTAX ERROR";
    public static final String SERVER_LOGIC_ERROR = "302 LOGIC ERROR";
    public static final String SERVER_KEY_OUT_OF_RANGE_ERROR = "303 KEY OUT OF RANGE";

    /* Map of authentication keys - (SERVER, CLIENT) */
    public static KeyPair[] KEYS;

    static {
        KEYS = new KeyPair[5];
        KEYS[0] = new KeyPair(23019, 32037);
        KEYS[1] = new KeyPair(32037, 29295);
        KEYS[2] = new KeyPair(18789, 13603);
        KEYS[3] = new KeyPair(16443, 29533);
        KEYS[4] = new KeyPair(18189, 21952);
    }

    /* Map for length of message types */
    public static Map<String, Integer> MSG_TYPE;

    static {
        MSG_TYPE = new HashMap<>();
        MSG_TYPE.put("name", 20);
        MSG_TYPE.put("key", 5);
        MSG_TYPE.put("confirmation", 7);
        MSG_TYPE.put("ok", 12);
        MSG_TYPE.put("recharging", 12);
        MSG_TYPE.put("full_power", 12);
        MSG_TYPE.put("secret", 100);
    }

    /* Map for determining, which direction robot will be facing after turning LEFT */
    public static Map<Direction, Direction> TURN_LEFT;

    static {
        TURN_LEFT = new HashMap<>();
        TURN_LEFT.put(Direction.LEFT, Direction.DOWN);
        TURN_LEFT.put(Direction.DOWN, Direction.RIGHT);
        TURN_LEFT.put(Direction.RIGHT, Direction.UP);
        TURN_LEFT.put(Direction.UP, Direction.LEFT);
        TURN_LEFT.put(Direction.UNDEFINED, Direction.UNDEFINED);
    }

    /* Map for determining, which direction robot will be facing after turning RIGHT */
    public static Map<Direction, Direction> TURN_RIGHT;

    static {
        TURN_RIGHT = new HashMap<>();
        TURN_RIGHT.put(Direction.LEFT, Direction.UP);
        TURN_RIGHT.put(Direction.UP, Direction.RIGHT);
        TURN_RIGHT.put(Direction.RIGHT, Direction.DOWN);
        TURN_RIGHT.put(Direction.DOWN, Direction.LEFT);
        TURN_RIGHT.put(Direction.UNDEFINED, Direction.UNDEFINED);
    }
}
