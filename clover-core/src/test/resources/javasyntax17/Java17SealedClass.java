public class Java17SealedClass {

    // test different order of modifiers and extends/permits combinations

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

    // permits with extends not allowed?
    // sealed class SubClassE permits SubClassF extends Object { }
}
