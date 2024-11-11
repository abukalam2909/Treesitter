package ca.dal.treefactor.model.core;

import ca.dal.treefactor.model.CodeElementType;
import io.github.treesitter.jtreesitter.Point;
import java.util.Objects;

public class LocationInfo {
    private final String filePath;
    private final Point startPoint;
    private final Point endPoint;
    private final CodeElementType type;

    public LocationInfo(String filePath, Point startPoint, Point endPoint, CodeElementType type) {
        this.filePath = filePath;
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.type = type;
    }

    /**
     * Get the file path of this code element
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Get the starting position (row, column)
     */
    public Point getStartPoint() {
        return startPoint;
    }

    /**
     * Get the ending position (row, column)
     */
    public Point getEndPoint() {
        return endPoint;
    }

    /**
     * Get the type of code element this location represents
     */
    public CodeElementType getType() {
        return type;
    }

    /**
     * Checks if this location contains a given point
     */
    public boolean containsPoint(Point point) {
        if (startPoint.row() > point.row() || endPoint.row() < point.row()) {
            return false;
        }
        if (startPoint.row() == point.row() && startPoint.column() > point.column()) {
            return false;
        }
        return endPoint.row() != point.row() || endPoint.column() >= point.column();
    }

    /**
     * Checks if this location contains another location
     */
    public boolean contains(LocationInfo other) {
        if (!filePath.equals(other.filePath)) {
            return false;
        }
        return containsPoint(other.startPoint) && containsPoint(other.endPoint);
    }

    /**
     * Checks if this location overlaps with another location
     */
    public boolean overlaps(LocationInfo other) {
        if (!filePath.equals(other.filePath)) {
            return false;
        }
        return containsPoint(other.startPoint) || containsPoint(other.endPoint) ||
                other.containsPoint(startPoint) || other.containsPoint(endPoint);
    }

    /**
     * Checks if this location comes before another location
     */
    public boolean isBefore(LocationInfo other) {
        if (!filePath.equals(other.filePath)) {
            return filePath.compareTo(other.filePath) < 0;
        }
        if (startPoint.row() != other.startPoint.row()) {
            return startPoint.row() < other.startPoint.row();
        }
        return startPoint.column() < other.startPoint.column();
    }

    /**
     * Checks if this location is on the same line as another location
     */
    public boolean isOnSameLine(LocationInfo other) {
        return filePath.equals(other.filePath) &&
                startPoint.row() == other.startPoint.row();
    }

    /**
     * Get the length of this location in terms of rows
     */
    public int getRowLength() {
        return endPoint.row() - startPoint.row() + 1;
    }

    /**
     * Get the length of this location in terms of columns (if on same row)
     */
    public int getColumnLength() {
        if (startPoint.row() == endPoint.row()) {
            return endPoint.column() - startPoint.column();
        }
        throw new IllegalStateException("Cannot get column length for multi-line location");
    }

    /**
     * Creates a new LocationInfo that encompasses both this location and another
     */
    public LocationInfo merge(LocationInfo other) {
        if (!filePath.equals(other.filePath)) {
            throw new IllegalArgumentException("Cannot merge locations from different files");
        }

        Point newStart = comparePoints(startPoint, other.startPoint) <= 0 ? startPoint : other.startPoint;
        Point newEnd = comparePoints(endPoint, other.endPoint) >= 0 ? endPoint : other.endPoint;

        return new LocationInfo(filePath, newStart, newEnd, type);
    }

    /**
     * Compare two points for ordering
     */
    private int comparePoints(Point p1, Point p2) {
        if (p1.row() != p2.row()) {
            return Integer.compare(p1.row(), p2.row());
        }
        return Integer.compare(p1.column(), p2.column());
    }

    @Override
    public String toString() {
        return String.format("%s:%d:%d-%d:%d [%s]",
                filePath,
                startPoint.row() + 1, startPoint.column() + 1,
                endPoint.row() + 1, endPoint.column() + 1,
                type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LocationInfo that = (LocationInfo) o;
        return Objects.equals(filePath, that.filePath) &&
                Objects.equals(startPoint, that.startPoint) &&
                Objects.equals(endPoint, that.endPoint) &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(filePath, startPoint, endPoint, type);
    }

    /**
     * Builder pattern for LocationInfo
     */
    public static class Builder {
        private String filePath;
        private Point startPoint;
        private Point endPoint;
        private CodeElementType type;

        public Builder() {
        }

        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder startPoint(Point startPoint) {
            this.startPoint = startPoint;
            return this;
        }

        public Builder endPoint(Point endPoint) {
            this.endPoint = endPoint;
            return this;
        }

        public Builder type(CodeElementType type) {
            this.type = type;
            return this;
        }

        public Builder startPoint(int row, int column) {
            this.startPoint = new Point(row, column);
            return this;
        }

        public Builder endPoint(int row, int column) {
            this.endPoint = new Point(row, column);
            return this;
        }

        public LocationInfo build() {
            if (filePath == null || startPoint == null || endPoint == null || type == null) {
                throw new IllegalStateException("All fields must be set");
            }
            return new LocationInfo(filePath, startPoint, endPoint, type);
        }
    }

    /**
     * Creates a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new LocationInfo from Tree-sitter node information
     */
    public static LocationInfo fromTreeSitterNode(String filePath, io.github.treesitter.jtreesitter.Node node, CodeElementType type) {
        return new LocationInfo(
                filePath,
                node.getStartPoint(),
                node.getEndPoint(),
                type
        );
    }
}