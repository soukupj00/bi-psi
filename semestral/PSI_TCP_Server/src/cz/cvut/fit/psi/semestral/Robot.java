package cz.cvut.fit.psi.semestral;

import java.util.LinkedList;
import java.util.Queue;

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
