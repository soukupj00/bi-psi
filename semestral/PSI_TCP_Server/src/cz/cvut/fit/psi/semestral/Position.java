package cz.cvut.fit.psi.semestral;

import java.util.Objects;

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
