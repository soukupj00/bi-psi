package cz.cvut.fit.psi.semestral;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Objects;

/** Enums */
public enum Direction {
    UP, LEFT, DOWN, RIGHT, UNDEFINED
}

public enum Movement {
    ROTATE_LEFT, ROTATE_RIGHT, MOVE, PICK_UP
}

/** Custom Exceptions */
public class KeyOutOfRangeException extends Exception {
}
public class LogicException extends Exception {
}
public class LoginFailedException extends Exception {
}
public class SyntaxException extends Exception {
}
public class TimeoutException extends Exception{
}

/** Helper classes */
public class KeyPair {
    int server;
    int client;

    public KeyPair(int server, int client) {
        this.server = server;
        this.client = client;
    }
}

public class Position {
    private final int x;
    private final int y;

    /**
     * Initializes position to INT_MIN_VALUE,
     * as this should be outside the possible range and not interfere with navigation
     */
    public Position() {
        x = Integer.MIN_VALUE;
        y = Integer.MIN_VALUE;
    }

    public Position(Integer lastX, Integer lastY) {
        this.x = lastX;
        this.y = lastY;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public String toString() {
        return "[" + x + ", " + y + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Position position = (Position) o;
        return Objects.equals(getX(), position.getX()) && Objects.equals(getY(), position.getY());
    }
}

/** All constants needed in program */
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

/**
 * Socket programming idea is taken from: https://www.geeksforgeeks.org/socket-programming-in-java/
 */
public class Server {
    public static void main(String[] args) {
        ServerSocket server = null;

        //Starts server and waits for a connection
        try {
            server = new ServerSocket(Constants.PORT);
            System.out.println("Server started on port: " + Constants.PORT);

            System.out.println("Waiting for a client ...");

            while (true) {
                Socket client = server.accept();
                System.out.println("Client accepted: " + client.getInetAddress().getHostAddress());

                Handler handler = new Handler(client);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (server != null) {
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

/** Class that handles client messages */
public class Handler extends Thread {
    private final Socket socket;
    //Inspiration for using BufferedReader: https://www.baeldung.com/java-buffered-reader
    private final BufferedReader bufferedReader;
    private final PrintWriter printWriter;

    public Handler(Socket client) throws IOException {
        this.socket = client;
        this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    public void run() {
        try {
            String robotName = authenticate();
            moveToTreasure(robotName);
            pickUpSecret();
            logoutClient();
        } catch (LoginFailedException e) {
            printWriter.write(Constants.SERVER_LOGIN_FAILED + Constants.MSG_ENDING);
            printWriter.flush();
            closeConnection();
            System.out.println("Login error occurred.");
        } catch (KeyOutOfRangeException e) {
            printWriter.write(Constants.SERVER_KEY_OUT_OF_RANGE_ERROR + Constants.MSG_ENDING);
            printWriter.flush();
            closeConnection();
            System.out.println("Key out of range error occurred.");
        } catch (SyntaxException e) {
            printWriter.write((Constants.SERVER_SYNTAX_ERROR + Constants.MSG_ENDING));
            printWriter.flush();
            closeConnection();
            System.out.println("Syntax error occurred.");
        } catch (TimeoutException e) {
            closeConnection();
            System.out.println("Timeout exception occurred in readMessage.");
        } catch (LogicException e) {
            printWriter.write((Constants.SERVER_LOGIC_ERROR + Constants.MSG_ENDING));
            printWriter.flush();
            closeConnection();
            System.out.println("Logic exception occurred - wrong sequence of RECHARGING, FULL_POWER.");
        }
    }

    /**
     * Authenticates client - robot name, KeyID, hash, correct message format/length
     *
     * @return robot name acquired from client
     * @throws KeyOutOfRangeException client send key that si outside of range 0-4
     * @throws LoginFailedException   client send incorrect hash for authentication
     * @throws SyntaxException        incorrect client message
     * @throws TimeoutException       message is not provided in given time
     * @throws LogicException         trying to send another message while robot is recharging
     */
    private String authenticate() throws SyntaxException, KeyOutOfRangeException, LoginFailedException, TimeoutException, LogicException {
        System.out.println("--------------------Authentication--------------------");
        String name = readMessage(Constants.MSG_TYPE.get("name"));    //get name
        System.out.println("Robot name: " + name);

        //Request KeyID
        System.out.println("Requesting KeyID.");
        printWriter.write(Constants.SERVER_KEY_REQUEST + Constants.MSG_ENDING);
        printWriter.flush();

        String strKeyId = readMessage(Constants.MSG_TYPE.get("key"));
        System.out.println("Client given KeyID: " + strKeyId);

        //Key contains only numbers
        if (!strKeyId.matches("^[0-9]+$")) {
            throw new SyntaxException();
        }

        int keyId = Integer.parseInt(strKeyId);
        //Key is in range
        if (keyId < 0 || keyId > 4) {
            throw new KeyOutOfRangeException();
        }

        int nameHash = getStringHash(name);
        int serverHash = (nameHash + Constants.KEYS[keyId].server) % 65536; //calculate server hash with key given by ID
        int clientHash = (nameHash + Constants.KEYS[keyId].client) % 65536; //calculate client hash with key given by ID

        System.out.println("Server hash: " + serverHash + " | Client hash: " + clientHash);

        //Send confirmation - server hash
        printWriter.write(serverHash + Constants.MSG_ENDING);
        printWriter.flush();

        String receivedClientHash = readMessage(Constants.MSG_TYPE.get("confirmation"));

        System.out.println("Received client hash: " + receivedClientHash);
        //Hash contains only numbers
        if (!receivedClientHash.matches("^[0-9]+$")) {
            throw new SyntaxException();
        }
        if (Integer.parseInt(receivedClientHash) != clientHash) {
            throw new LoginFailedException();
        } else {
            System.out.println("Authentication successful.");
            printWriter.write(Constants.SERVER_OK + Constants.MSG_ENDING);
            printWriter.flush();
            return name;
        }
    }

    /**
     * Sends first to signals as MOVE to determine location/direction of robot, then navigates robot to field [0,0]
     *
     * @param robotName acquired robot name in authentication
     * @throws SyntaxException  incorrect client message
     * @throws TimeoutException message is not provided in given time
     * @throws LogicException   trying to send another message while robot is recharging
     */
    private void moveToTreasure(String robotName) throws SyntaxException, TimeoutException, LogicException {
        System.out.println("--------------------Robot Movement--------------------");
        Robot robot = new Robot(robotName);

        //To establish position, try moving twice
        System.out.println("First two robot moves.");
        for (int i = 0; i < 2; i++) {
            printWriter.write(Constants.SERVER_MOVE + Constants.MSG_ENDING);
            printWriter.flush();

            Position position = getRobotPositionFromClient();
            robot.setPosition(position);
            System.out.println("Msg " + i + " :" + robot);
        }

        //After robot moved twice, navigate to [0,0]
        robot.findDirection();
        System.out.println("Navigating robot to [0,0].");

        while (true) {
            if (robot.getCollisions() >= 20) {
                System.out.println("Robot collided 20 times, ending connection with client.");
                logoutClient();
                return;
            }

            Queue<Movement> steps = robot.getMoves();
            while (!steps.isEmpty()) {
                Movement movement = steps.remove();

                switch (movement) {
                    case MOVE -> printWriter.write(Constants.SERVER_MOVE + Constants.MSG_ENDING);
                    case ROTATE_LEFT -> printWriter.write(Constants.SERVER_TURN_LEFT + Constants.MSG_ENDING);
                    case ROTATE_RIGHT -> printWriter.write(Constants.SERVER_TURN_RIGHT + Constants.MSG_ENDING);
                    case PICK_UP -> {
                        return;    //Located at [0,0], should pick up the secret
                    }
                }
                printWriter.flush();
                Position position = getRobotPositionFromClient();

                //Update position only when robot tried to move, else change direction
                if (movement == Movement.MOVE) {
                    robot.setPosition(position);

                    //Fixes some situations, where obstacle is next to [0,0] and robot goes around it,
                    //and not picking up the secret in process (slightly bad programming on my part...)
                    if (robot.isAtStartingCoordinates()) {
                        return;
                    }
                } else {
                    robot.changeDirection(movement);
                }

                //In case robot was stuck on first two moves, need to determine direction after successful move was made
                if (robot.getDirection() == Direction.UNDEFINED) {
                    robot.findDirection();
                }
                System.out.println(robot);
            }
        }
    }

    /**
     * Acquires robot position from client message, checks correct format of client message
     *
     * @return position of robot provided by client message
     * @throws SyntaxException  incorrect client message
     * @throws TimeoutException message is not provided in given time
     * @throws LogicException   trying to send another message while robot is recharging
     */
    private Position getRobotPositionFromClient() throws SyntaxException, TimeoutException, LogicException {
        String clientResponse = readMessage(Constants.MSG_TYPE.get("ok"));
        //Check whether string is in correct format
        if (!clientResponse.matches("^OK -?[0-9]+ -?[0-9]+$")) {
            throw new SyntaxException();
        }
        //msg[0] should be "OK", followed by position x and y
        String[] msg = clientResponse.split(" ");
        int x = Integer.parseInt(msg[1]);
        int y = Integer.parseInt(msg[2]);
        return new Position(x, y);
    }

    private void pickUpSecret() throws SyntaxException, TimeoutException, LogicException {
        System.out.println("--------------------Revealing Secret--------------------");
        System.out.println("Trying to pick up secret message.");
        printWriter.write(Constants.SERVER_PICK_UP + Constants.MSG_ENDING);
        printWriter.flush();

        String msg = readMessage(Constants.MSG_TYPE.get("secret"));
        System.out.println("Secret message: " + msg);
    }

    private void logoutClient() {
        System.out.println("--------------------Client Logout--------------------");
        System.out.println("Logging out client.");
        printWriter.write(Constants.SERVER_LOGOUT + Constants.MSG_ENDING);
        printWriter.flush();
        closeConnection();
    }

    private void closeConnection() {
        System.out.println("--------------------Closing Connection--------------------");
        try {
            socket.close();
            System.out.println("Closed connection.");
        } catch (IOException e) {
            System.out.println("Cannot close connection - socket.close() failed.");
        }
    }

    /**
     * Determines whether expected message in buffer from client is correct - format, length
     *
     * @param maxMessageLength - maximum length of message to be read
     * @return - returns contents of received message, without the ending character sequence
     * @throws SyntaxException  incorrect client message
     * @throws TimeoutException message is not provided in given time
     * @throws LogicException   trying to send another message while robot is recharging
     */
    private String readMessage(int maxMessageLength) throws SyntaxException, TimeoutException, LogicException {
        int bufferMsgLength = Math.max(maxMessageLength, Constants.MSG_TYPE.get("recharging"));
        String clientMessage = readBuffer(bufferMsgLength);

        if (clientMessage == null) {
            throw new SyntaxException();
        }
        //Handle recharging of robot, continue with previous expected action
        if (clientMessage.equals(Constants.RECHARGING)) {
            handleRecharging();
            return readMessage(maxMessageLength);
        }
        //Subtract 2 for removing \a\b
        if (clientMessage.length() > maxMessageLength - 2) {
            throw new SyntaxException();
        }

        return clientMessage;
    }

    /**
     * Acquires string from buffer without \a\b, or null if something went wrong
     * Checks whether base client message is in correct format - length, ending \a\b
     *
     * @param length maximum length of chars that should be read
     * @return client message without \a\b, or null if something went wrong
     * @throws SyntaxException  incorrect client message
     * @throws TimeoutException message is not provided in given time
     */
    private String readBuffer(int length) throws TimeoutException, SyntaxException {
        //Inspiration on using StringBuilder: https://www.geeksforgeeks.org/stringbuilder-class-in-java-with-examples/
        StringBuilder strBuilder = new StringBuilder();
        int currChar;
        try {
            //Length of buffered msg does not exceed max length and buffer still has chars remaining
            while (((currChar = bufferedReader.read()) != -1)) {
                strBuilder.append((char) currChar);
                //Set timeout to 1 second
                socket.setSoTimeout(Constants.TIMEOUT);

                if (strBuilder.length() >= 2) {
                    //Client message is in correct format
                    if (strBuilder.substring(strBuilder.length() - 2).equals(Constants.MSG_ENDING)
                            || strBuilder.length() > length) {
                        return strBuilder.substring(0, strBuilder.length() - 2);
                    }

                    //Client message doesn't have an \a as second to last char - \a\b
                    if (strBuilder.charAt(strBuilder.length() - 1) != '\u0007'
                            && strBuilder.length() + 1 == length) {
                        System.out.println("Message contains \\a that is not followed by \\b at the end " + strBuilder.length());
                        throw new SyntaxException();
                    }
                }
            }
        } catch (IOException e) {
            throw new TimeoutException();
        }
        return null;
    }

    /**
     * When message from client is "RECHARGING", set timeout to 5s, await for "FULL_POWER", if it isn't send as next,
     * raises exception and will end connection
     *
     * @throws SyntaxException  incorrect client message
     * @throws TimeoutException message is not provided in given time
     * @throws LogicException   trying to send another message while robot is recharging
     */
    private void handleRecharging() throws SyntaxException, TimeoutException, LogicException {
        try {
            System.out.println("RECHARGING");

            //Set 5s timeout
            socket.setSoTimeout(Constants.TIMEOUT_RECHARGING);
            String clientMessage = readBuffer(Constants.MSG_TYPE.get("full_power"));

            if (clientMessage == null) {
                throw new LogicException();
            }

            //If we receive other message than "FULL_POWER", server sends Logic Error
            if (clientMessage.equals(Constants.FULL_POWER)) {
                socket.setSoTimeout(Constants.TIMEOUT);
            } else {
                throw new LogicException();
            }
        } catch (IOException e) {
            throw new TimeoutException();
        }
    }

    private int getStringHash(String name) {
        int hash = 0;
        for (int i = 0; i < name.length(); i++) {
            hash += name.charAt(i);   //add char ascii representation
        }
        hash = hash * 1000; //multiply
        return hash % 65536;
    }
}

/** Robot class, determines direction, movement, collisions, name,... */
public class Robot {
    private Position lastPosition;
    private Position currentPosition;
    private Direction direction = Direction.UNDEFINED;
    //Array of moves that server should send to client, updates on every response
    private int collisions = 0;
    private final String name;
    private Queue<Movement> movements = new LinkedList<>();

    public Robot(String name) {
        this.name = name;
        this.lastPosition = new Position();
        this.currentPosition = new Position();
    }

    public Queue<Movement> getMoves() {
        if (isAtStartingCoordinates()) {   //Robot is at [0,0], pick up message
            movements.add(Movement.PICK_UP);
        } else if (isStuck()) {            //Robot has collided with an obstacle
            System.out.println("Robot " + name + " is stuck.");
            collisions++;

            //First two moves failed to determine direction (got stuck), turn LEFT and MOVE
            if (direction == Direction.UNDEFINED) {
                movements.add(Movement.ROTATE_LEFT);
                movements.add(Movement.MOVE);
            } else {
                loadMovesToGoAroundObstacle();
            }
        } else {
            simpleMove();
        }
        return movements;
    }

    public void setPosition(Position position) {
        lastPosition = currentPosition;
        currentPosition = position;
    }

    public int getCollisions() {
        return collisions;
    }

    public Direction getDirection() {
        return direction;
    }

    @Override
    public String toString() {
        return "Robot name: " + name + " | Current: " + currentPosition
                + " | Last: " + lastPosition + " | Direction: " + direction + " | Collisions: " + collisions;
    }

    public boolean isAtStartingCoordinates() {
        return currentPosition.equals(new Position(0, 0));
    }

    public void findDirection() {
        if (lastPosition.getX() > currentPosition.getX()) {
            direction = Direction.LEFT;
        }
        if (lastPosition.getX() < currentPosition.getX()) {
            direction = Direction.RIGHT;
        }
        if (lastPosition.getY() > currentPosition.getY()) {
            direction = Direction.DOWN;
        }
        if (lastPosition.getY() < currentPosition.getY()) {
            direction = Direction.UP;
        }
    }

    public void changeDirection(Movement movement) {
        switch (movement) {
            case ROTATE_LEFT -> direction = Constants.TURN_LEFT.get(direction);
            case ROTATE_RIGHT -> direction = Constants.TURN_RIGHT.get(direction);
        }
    }

    private boolean isStuck() {
        return lastPosition.equals(currentPosition);
    }

    public void simpleMove() {
        /* Robot is to the left of [0,0], navigate RIGHT */
        if (currentPosition.getX() < 0) {
            if (direction == Direction.RIGHT) {
                movements.add(Movement.MOVE);
            } else if (direction == Direction.UP) {
                movements.add(Movement.ROTATE_RIGHT);
            } else if (direction == Direction.DOWN) {
                movements.add(Movement.ROTATE_LEFT);
            } else if (direction == Direction.LEFT) {
                movements.add(Movement.ROTATE_RIGHT);
            }
        }
        /* Robot is to the right of [0,0], navigate LEFT */
        else if (currentPosition.getX() > 0) {
            if (direction == Direction.LEFT) {
                movements.add(Movement.MOVE);
            } else if (direction == Direction.UP) {
                movements.add(Movement.ROTATE_LEFT);
            } else if (direction == Direction.DOWN) {
                movements.add(Movement.ROTATE_RIGHT);
            } else if (direction == Direction.RIGHT) {
                movements.add(Movement.ROTATE_LEFT);
            }
        }
        /* Robot is up top of [0,0], navigate DOWN */
        else if (currentPosition.getY() > 0) {
            if (direction == Direction.DOWN) {
                movements.add(Movement.MOVE);
            } else if (direction == Direction.LEFT) {
                movements.add(Movement.ROTATE_LEFT);
            } else if (direction == Direction.RIGHT) {
                movements.add(Movement.ROTATE_RIGHT);
            } else if (direction == Direction.UP) {
                movements.add(Movement.ROTATE_LEFT);
            }
        }
        /* Robot is down of [0,0], navigate UP */
        else if (currentPosition.getY() < 0) {
            if (direction == Direction.UP) {
                movements.add(Movement.MOVE);
            } else if (direction == Direction.LEFT) {
                movements.add(Movement.ROTATE_RIGHT);
            } else if (direction == Direction.RIGHT) {
                movements.add(Movement.ROTATE_LEFT);
            } else if (direction == Direction.DOWN) {
                movements.add(Movement.ROTATE_LEFT);
            }
        }
    }

    private void loadMovesToGoAroundObstacle() {
        if (currentPosition.getX() == 0 || currentPosition.getY() == 0) {
            movements.add(Movement.ROTATE_LEFT);
            movements.add(Movement.MOVE);
            movements.add(Movement.ROTATE_RIGHT);
            movements.add(Movement.MOVE);
            movements.add(Movement.MOVE);
            movements.add(Movement.ROTATE_RIGHT);
            movements.add(Movement.MOVE);
            movements.add(Movement.ROTATE_LEFT);
        } else {
            // In case we are not at position [0,y] or [x,0],
            // navigate to other direction that will lead us to said positions
            switch (direction) {
                case UP -> movements.add(currentPosition.getX() > 0 ? Movement.ROTATE_LEFT : Movement.ROTATE_RIGHT);
                case DOWN -> movements.add(currentPosition.getX() < 0 ? Movement.ROTATE_LEFT : Movement.ROTATE_RIGHT);
                case LEFT -> movements.add(currentPosition.getY() > 0 ? Movement.ROTATE_LEFT : Movement.ROTATE_RIGHT);
                case RIGHT -> movements.add(currentPosition.getY() < 0 ? Movement.ROTATE_LEFT : Movement.ROTATE_RIGHT);
            }
            movements.add(Movement.MOVE);
        }
    }
}
