import java.io.IOException;

//covering CLOV-1834
public class AnnotationAndCatchClauses {

    @interface Annotation1 {
    }

    @interface Annotation2 {
    }

    @interface Annotation3 {
        String value();
    }

    static void simpleFinalFirstLastAnnotationAndCatch() throws IOException {
        try {
            throw new IOException();
        } catch (final @SuppressWarnings("unused") IOException e) {
        }
    }

    static void simpleFinalLastAnnotationAndCatch() throws IOException {
        try {
            throw new IOException();
        } catch (@SuppressWarnings("unused") final IOException e) {
        }
    }

    static void multipleFinalLastAnnotationAndCatch() throws IOException {
        try {
            throw new IOException();
        } catch (@Deprecated @SuppressWarnings("unused") final IOException e) {
        }
    }

    static void multipleFinalFirstAnnotationAndCatch() throws IOException {
        try {
            throw new IOException();
        } catch (final @Deprecated @SuppressWarnings("unused") IOException e) {
        }
    }

    static void multiplFinalMiddleAnnotationAndCatch() throws IOException {
        try {
            throw new IOException();
        } catch (@Deprecated final @SuppressWarnings("unused") IOException e) {
        }
    }

    static void multiplFinalMiddleBunchAnnotationAndCatch() throws IOException {
        try {
            throw new IOException();
        } catch (@Deprecated @Annotation1() @Annotation2 final @SuppressWarnings("unused") @Annotation3("test") IOException e) {
        }
    }
}