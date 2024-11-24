package ca.dal.treefactor.model.core;

import java.util.Objects;

public class UMLComment {
    private final String text;
    private final LocationInfo locationInfo;
    private final CommentType type;

    public enum CommentType {
        SINGLE_LINE,    // For //, #, --
        MULTI_LINE,     // For /* */, """ """, --[[ ]]
        DOC_COMMENT,    // For /** */, ''', /** */
        INLINE          // For inline comments
    }

    public UMLComment(String text, LocationInfo locationInfo, CommentType type) {
        this.text = text;
        this.locationInfo = locationInfo;
        this.type = type;
    }

    // Getters
    public String getText() {
        return text;
    }

    public LocationInfo getLocationInfo() {
        return locationInfo;
    }

    public CommentType getType() {
        return type;
    }

    // Utility methods
    public boolean isDocComment() {
        return type == CommentType.DOC_COMMENT;
    }

    public boolean isMultiLine() {
        return type == CommentType.MULTI_LINE || type == CommentType.DOC_COMMENT;
    }

    public String getCleanText() {
        return switch (type) {
            case SINGLE_LINE -> cleanSingleLineComment();
            case MULTI_LINE -> cleanMultiLineComment();
            case DOC_COMMENT -> cleanDocComment();
            case INLINE -> text.trim();
        };
    }

    private String cleanSingleLineComment() {
        return text.replaceAll("^(//|#|--)+\\s*", "").trim();
    }

    private String cleanMultiLineComment() {
        return text
                .replaceAll("^/\\*+\\s*", "")
                .replaceAll("\\s*\\*/$", "")
                .replaceAll("^\"\"\"\\s*", "")
                .replaceAll("\\s*\"\"\"$", "")
                .replaceAll("^--\\[\\[\\s*", "")
                .replaceAll("\\s*\\]\\]$", "")
                .trim();
    }

    private String cleanDocComment() {
        String cleaned = text
                .replaceAll("^/\\*\\*+\\s*", "")
                .replaceAll("\\s*\\*/$", "")
                .replaceAll("^'''\\s*", "")
                .replaceAll("\\s*'''$", "");
        return cleaned
                .replaceAll("^\\s*\\*\\s*", "")
                .trim();
    }

    @Override
    public String toString() {
        return String.format("%s: %s", type, text);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UMLComment comment = (UMLComment) o;
        return Objects.equals(text, comment.text) &&
                Objects.equals(locationInfo, comment.locationInfo) &&
                type == comment.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, locationInfo, type);
    }

    // Builder pattern
    public static class Builder {
        private final String text;
        private final LocationInfo locationInfo;
        private CommentType type = CommentType.SINGLE_LINE;

        public Builder(String text, LocationInfo locationInfo) {
            this.text = text;
            this.locationInfo = locationInfo;
        }

        public Builder type(CommentType type) {
            this.type = type;
            return this;
        }

        public UMLComment build() {
            return new UMLComment(text, locationInfo, type);
        }
    }

    public static Builder builder(String text, LocationInfo locationInfo) {
        return new Builder(text, locationInfo);
    }
}