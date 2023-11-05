import java.io.Serializable;

public class Java17SealedClass {

    // test different order of modifiers

    // PublicStaticSealedClass
    // + SubClassA final
    // + SubClassB non-sealed
    // + SubClassC non-sealed

    public static sealed class PublicStaticSealedClass permits SubClassA, SubClassB, SubClassC { }

    private final static class SubClassA extends PublicStaticSealedClass { }

    static private non-sealed class SubClassB extends PublicStaticSealedClass { }

    non-sealed static class SubClassC extends PublicStaticSealedClass { }

    // SealedPrivateClass
    // + SubClassD final

    sealed private static class SealedPrivateClass permits SubClassD { }

    private static final class SubClassD extends SealedPrivateClass { }

    // SubClassE
    // + SubClassF final
    // + SubClassG non-sealed
    //   + SubClassH normal
    sealed class SubClassE permits SubClassF, SubClassG { }

    private final class SubClassF extends SubClassE { }

    private non-sealed class SubClassG extends SubClassE { }

    private class SubClassH extends SubClassG { }

    // test different order of extends / permits / implements

    // permits ... extends ... is not allowed
    //sealed class ClassExtendsObject2 permits SubClassExtendsObject2 extends Object { }
    //private final class SubClassExtendsObject2 extends ClassExtendsObject2 { }

    // extends ... permits ... is fine
    sealed class ClassExtendsObject extends Object permits SubClassExtendsObject { }
    final class SubClassExtendsObject extends ClassExtendsObject { }

    // permits ... implements ... is not allowed
    //sealed class ClassImplementsSerializable2 permits SubClassImplementsSerializable2 implements Serializable { }
    //final class SubClassImplementsSerializable2 extends ClassImplementsSerializable2 { }

    // implements ... permits ... is fine
    sealed class ClassImplementsSerializable implements Serializable permits SubClassImplementsSerializable { }
    final class SubClassImplementsSerializable extends ClassImplementsSerializable { }

}
