package ca.dal.treefactor.util;

import io.github.treesitter.jtreesitter.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ASTUtil {
    public static class ASTNode {
        private String type;
        private String fieldName;
        private final List<ASTNode> children;
        private Point startPoint;
        private Point endPoint;
        private ASTNode parent;
        private int startByte;
        private int endByte;

        public ASTNode(String type, String fieldName, Point startPoint, Point endPoint,
                       int startByte, int endByte, ASTNode parent) {
            this.type = type;
            this.fieldName = fieldName;
            this.children = new ArrayList<>();
            this.startPoint = startPoint;
            this.endPoint = endPoint;
            this.startByte = startByte;
            this.endByte = endByte;
            this.parent = parent;
        }

        // Getters
        public String getType() {
            return type;
        }

        public String getFieldName() {
            return fieldName;
        }

        public List<ASTNode> getChildren() {
            return Collections.unmodifiableList(children);
        }

        public Point getStartPoint() {
            return startPoint;
        }

        public Point getEndPoint() {
            return endPoint;
        }

        public int getStartByte() {
            return startByte;
        }

        public int getEndByte() {
            return endByte;
        }

        public Optional<ASTNode> getParent() {
            return Optional.ofNullable(parent);
        }

        // Protected setters for internal use
        protected void setType(String type) {
            this.type = type;
        }

        protected void setFieldName(String fieldName) {
            this.fieldName = fieldName;
        }

        protected void setStartPoint(Point startPoint) {
            this.startPoint = startPoint;
        }

        protected void setEndPoint(Point endPoint) {
            this.endPoint = endPoint;
        }

        protected void setStartByte(int startByte) {
            this.startByte = startByte;
        }

        protected void setEndByte(int endByte) {
            this.endByte = endByte;
        }

        protected void setParent(ASTNode parent) {
            this.parent = parent;
        }

        // Methods
        public void addChild(ASTNode child) {
            if (child != null) {
                children.add(child);
                child.setParent(this);
            }
        }

        public String getText(String sourceCode) {
            return sourceCode.substring(startByte, endByte);
        }
    }

    // Rest of the ASTUtil class remains unchanged
    public static ASTNode buildASTWithCursor(Node rootNode) {
        try (TreeCursor cursor = rootNode.walk()) {
            return buildNodeWithCursor(cursor, null);
        }
    }

    private static ASTNode buildNodeWithCursor(TreeCursor cursor, ASTNode parent) {
        Node currentNode = cursor.getCurrentNode();
        String nodeType = currentNode.getType();
        String fieldName = cursor.getCurrentFieldName();
        Point startPoint = currentNode.getStartPoint();
        Point endPoint = currentNode.getEndPoint();
        int startByte = currentNode.getStartByte();
        int endByte = currentNode.getEndByte();

        ASTNode node = new ASTNode(nodeType, fieldName, startPoint, endPoint,
                startByte, endByte, parent);

        if (cursor.gotoFirstChild()) {
            do {
                ASTNode childNode = buildNodeWithCursor(cursor, node);
                if (childNode != null) {
                    node.addChild(childNode);
                }
            } while (cursor.gotoNextSibling());
            cursor.gotoParent();
        }

        if (currentNode.isNamed()) {
            return node;
        } else if (!node.getChildren().isEmpty()) {
            return node.getChildren().size() == 1 ? node.getChildren().get(0) : node;
        }
        return null;
    }

    public static String printAST(ASTNode node, int depth) {
        StringBuilder sb = new StringBuilder();
        printASTHelper(node, depth, sb);
        return sb.toString();
    }

    private static void printASTHelper(ASTNode node, int depth, StringBuilder sb) {
        String indent = "  ".repeat(depth);
        String fieldInfo = node.getFieldName() != null ? node.getFieldName() + ": " : "";
        String positionInfo = String.format("[%d, %d] - [%d, %d]",
                node.getStartPoint().row(), node.getStartPoint().column(),
                node.getEndPoint().row(), node.getEndPoint().column());
        sb.append(String.format("%s%s%s %s\n", indent, fieldInfo, node.getType(), positionInfo));
        for (ASTNode child : node.getChildren()) {
            printASTHelper(child, depth + 1, sb);
        }
    }
}