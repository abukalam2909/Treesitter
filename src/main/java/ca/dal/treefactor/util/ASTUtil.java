package ca.dal.treefactor.util;

import io.github.treesitter.jtreesitter.*;
import java.util.ArrayList;
import java.util.List;

public class ASTUtil {
    public static class ASTNode {
        public String type;
        public String fieldName;
        public List<ASTNode> children;
        public Point startPoint;
        public Point endPoint;

        public ASTNode(String type, String fieldName, Point startPoint, Point endPoint) {
            this.type = type;
            this.fieldName = fieldName;
            this.children = new ArrayList<>();
            this.startPoint = startPoint;
            this.endPoint = endPoint;
        }

        public void addChild(ASTNode child) {
            children.add(child);
        }
    }

    public static ASTNode buildASTWithCursor(Node rootNode) {
        try (TreeCursor cursor = rootNode.walk()) {
            return buildNodeWithCursor(cursor);
        }
    }

    private static ASTNode buildNodeWithCursor(TreeCursor cursor) {
        Node currentNode = cursor.getCurrentNode();
        String nodeType = currentNode.getType();
        String fieldName = cursor.getCurrentFieldName();
        Point startPoint = currentNode.getStartPoint();
        Point endPoint = currentNode.getEndPoint();

        ASTNode node = new ASTNode(nodeType, fieldName, startPoint, endPoint);

        if (cursor.gotoFirstChild()) {
            do {
                ASTNode childNode = buildNodeWithCursor(cursor);
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
}
