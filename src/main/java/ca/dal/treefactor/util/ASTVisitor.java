package ca.dal.treefactor.util;

import java.util.ArrayList;
import java.util.List;

import ca.dal.treefactor.model.UMLModel;
import ca.dal.treefactor.util.ASTUtil.ASTNode;

public abstract class ASTVisitor {
    protected final UMLModel model;
    protected final String sourceCode;
    protected final String filePath;

    public ASTVisitor(UMLModel model, String sourceCode, String filePath) {
        this.model = model;
        this.sourceCode = sourceCode;
        this.filePath = filePath;
    }

    /**
     * Main visit method to traverse the AST
     */
    public abstract void visit(ASTNode node);

    /**
     * Process module/namespace/package level declarations
     */
    protected abstract void processModule(ASTNode node);

    /**
     * Process class declarations
     */
    protected abstract void processClass(ASTNode node);

    /**
     * Process method/function declarations
     */
    protected abstract void processMethod(ASTNode node);

    /**
     * Process field/attribute declarations
     */
    protected abstract void processField(ASTNode node);

    /**
     * Process import declarations
     */
    protected abstract void processImport(ASTNode node);

    /**
     * Helper method to find a child node by type
     */
    protected ASTNode findChildByType(ASTNode parent, String type) {
        if (parent == null) return null;
        for (ASTNode child : parent.getChildren()) {
            if (child.getType().equals(type)) {
                return child;
            }
        }
        return null;
    }

    protected ASTNode findChildByFieldName(ASTNode parent, String fieldName) {
        if (parent == null || fieldName == null) {
            return null;
        }
        for (ASTNode child : parent.getChildren()) {
            // Defensive check for null field name
            if (child != null && child.getFieldName() != null && child.getFieldName().equals(fieldName)) {
                return child;
            }
        }
        return null;
    }

    /**
     * Helper method to find all children of a specific type
     */
    protected List<ASTNode> findChildrenByType(ASTNode parent, String type) {
        List<ASTNode> children = new ArrayList<>();
        if (parent == null) return children;
        for (ASTNode child : parent.getChildren()) {
            if (child.getType().equals(type)) {
                children.add(child);
            }
        }
        return children;
    }

    /**
     * Helper method to find the first child with any of the given types
     */
    protected ASTNode findChildByTypes(ASTNode parent, String... types) {
        if (parent == null) return null;
        for (ASTNode child : parent.getChildren()) {
            for (String type : types) {
                if (child.getType().equals(type)) {
                    return child;
                }
            }
        }
        return null;
    }

    /**
     * Helper method to check if a node has a specific type
     */
    protected boolean hasType(ASTNode node, String type) {
        return node != null && node.getType().equals(type);
    }

    /**
     * Helper method to get a node's text safely
     */
    protected String getNodeText(ASTNode node) {
        return node != null ? node.getText(sourceCode) : null;
    }

    /**
     * Helper method to get text of a child node by type
     */
    protected String getChildText(ASTNode parent, String childType) {
        ASTNode child = findChildByType(parent, childType);
        return getNodeText(child);
    }

    /**
     * Helper method to find first node of a given type in the subtree
     */
    protected ASTNode findFirstNodeOfType(ASTNode root, String type) {
        if (root.getType().equals(type)) {
            return root;
        }
        for (ASTNode child : root.getChildren()) {
            ASTNode result = findFirstNodeOfType(child, type);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    /**
     * Helper method to find all nodes of a given type in the subtree
     */
    protected List<ASTNode> findNodesOfType(ASTNode root, String type) {
        List<ASTNode> results = new ArrayList<>();
        if (root.getType().equals(type)) {
            results.add(root);
        }
        for (ASTNode child : root.getChildren()) {
            results.addAll(findNodesOfType(child, type));
        }
        return results;
    }
}