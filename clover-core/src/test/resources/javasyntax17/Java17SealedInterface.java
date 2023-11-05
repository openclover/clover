import java.io.Serializable;

public class Java17SealedInterface {

    // test different order of modifiers

    // PublicStaticSealedInterface
    // + SubInterfaceA non-sealed
    // + SubInterfaceB non-sealed
    // + SubInterfaceC non-sealed

    public static sealed interface PublicStaticSealedInterface permits SubInterfaceA, SubInterfaceB, SubInterfaceC { }

    private non-sealed static interface SubInterfaceA extends PublicStaticSealedInterface { }

    static private non-sealed interface SubInterfaceB extends PublicStaticSealedInterface { }

    non-sealed static interface SubInterfaceC extends PublicStaticSealedInterface { }

    // SealedPrivateInterface
    // + SubInterfaceD non-sealed

    sealed private static interface SealedPrivateInterface permits SubInterfaceD { }

    private static non-sealed interface SubInterfaceD extends SealedPrivateInterface { }

    // SubInterfaceE
    // + SubInterfaceF final
    // + SubInterfaceG non-sealed
    //   + SubInterfaceH normal
    sealed interface SubInterfaceE permits SubInterfaceF, SubInterfaceG { }

    private non-sealed interface SubInterfaceF extends SubInterfaceE { }

    private non-sealed interface SubInterfaceG extends SubInterfaceE { }

    private interface SubInterfaceH extends SubInterfaceG { }

    // test different order of extends / permits / implements

    // permits ... extends ... is not allowed
    //sealed interface InterfaceExtendsSerializable2 permits SubInterfaceExtendsSerializable2 extends Serializable { }
    //non-sealed interface SubInterfaceExtendsSerializable2 extends InterfaceExtendsSerializable2 { }

    // implements ... permits ... is fine
    sealed interface InterfaceExtendsSerializable extends Serializable permits SubInterfaceImplementsSerializable { }
    non-sealed interface SubInterfaceImplementsSerializable extends InterfaceExtendsSerializable { }

}
