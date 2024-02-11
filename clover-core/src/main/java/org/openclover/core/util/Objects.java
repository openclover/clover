package org.openclover.core.util;

import org.jetbrains.annotations.Nullable;

public abstract class Objects {

    public static ToStringBuilder toStringBuilder(Object object) {
        return toStringBuilder(object.getClass());
    }

    public static ToStringBuilder toStringBuilder(Class<?> clazz) {
        return new ToStringBuilder(clazz);
    }

    public static class ToStringBuilder {
        private final StringBuilder content;

        private ToStringBuilder(Class<?> clazz) {
            content = new StringBuilder(clazz.getSimpleName() + "{");
        }

        public <T> ToStringBuilder add(String fieldName, @Nullable T fieldValue) {
            content.append(fieldName)
                    .append("=")
                    .append(fieldValue == null ? "null" : fieldValue.toString())
                    .append(",");
            return this;
        }

        public String toString() {
            return content.toString() + "}";
        }
    }
}
