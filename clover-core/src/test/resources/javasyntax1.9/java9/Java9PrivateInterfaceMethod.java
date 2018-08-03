package java9;

public interface Java9PrivateInterfaceMethod {

    void publicMethod();

    default void defaultMethod() {
        privateMethod();
    }

    private void privateMethod() {
        int i = 0;
    }
}