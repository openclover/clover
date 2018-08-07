package java9;

class NonModuleInfo {
    static void requires(int transitive) { }
    void exports(int to) { }
    void opens(int to) { }
    void uses() { }
    void provides(int with) { }
}
