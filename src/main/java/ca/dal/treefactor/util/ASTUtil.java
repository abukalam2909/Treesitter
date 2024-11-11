package ca.dal.treefactor.util;

import io.github.treesitter.jtreesitter.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ASTUtil {
    public static class ASTNode {
        public String type;
        public String fieldName;
        public List<ASTNode> children;
        public Point startPoint;
        public Point endPoint;
        public ASTNode parent; // Add parent field
        public int startByte;  // Add these fields
        public int endByte;    // Add these fields


        public ASTNode(String type, String fieldName, Point startPoint, Point endPoint,
                       int startByte, int endByte, ASTNode parent) {
            this.type = type;
            this.fieldName = fieldName;
            this.children = new ArrayList<>();
            this.startPoint = startPoint;
            this.endPoint = endPoint;
            this.startByte = startByte;  // Initialize these
            this.endByte = endByte;      // Initialize these
            this.parent = parent; // Initialize parent
        }

        public void addChild(ASTNode child) {
            children.add(child);
        }

        // Add method to get text content
        public String getText(String sourceCode) {
            return sourceCode.substring(startByte, endByte);
        }

        public Optional<ASTNode> getParent() {
            return Optional.ofNullable(parent);
        }
    }

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
        int startByte = currentNode.getStartByte();  // Get byte offsets
        int endByte = currentNode.getEndByte();      // Get byte offsets

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

        // Only return the node if it's named
        if (currentNode.isNamed()) {
            return node;
        }
        // If this node is not named but has children, return its children
        else if (!node.children.isEmpty()) {
            return node.children.size() == 1 ? node.children.get(0) : node;
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
        String fieldInfo = node.fieldName != null ? node.fieldName + ": " : "";
        String positionInfo = String.format("[%d, %d] - [%d, %d]",
                node.startPoint.row(), node.startPoint.column(),
                node.endPoint.row(), node.endPoint.column());
        sb.append(String.format("%s%s%s %s\n", indent, fieldInfo, node.type, positionInfo));
        for (ASTNode child : node.children) {
            printASTHelper(child, depth + 1, sb);
        }
    }

//    // method to get the text content of a node
//    public static String getNodeText(ASTNode node, String sourceCode) {
//        int startByte = node.getStartByte();
//        int endByte = node.getEndByte();
//        return sourceCode.substring(startByte, endByte);
//    }
}
