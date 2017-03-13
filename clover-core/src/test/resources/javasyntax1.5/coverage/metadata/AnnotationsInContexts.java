package coverage.metadata;

/**
 * Tests in which contexts we can use annotations. In general, we can use annotation
 * whenever we declare a class, method, constructor or declare a field or variable.
 * This code should compiler correctly.
 *
 * @see
 */
@Deprecated // class declaration
public class AnnotationsInContexts {

    public @interface MyAnnotation {

    }

    // field declaration
    @Deprecated String veryOldField;

    @Deprecated // method declaration
    public void deprecated() {

    }

    // constructor declaration
    @Deprecated AnnotationsInContexts() {

    }

    public void withFormalParameters(@SuppressWarnings("unused") String s, @Deprecated Integer i) {

    }

    /**
     * See https://jira.atlassian.com/browse/CLOV-1096
     */
    public void inCatchClause() {
        try {

        } catch (@SuppressWarnings("unused") @Deprecated Exception ex) {

        }
    }

    public void withVariableDeclaration() {
        @MyAnnotation String abc;
    }
}
