package org.openclover.core.registry.entities;

import org.openclover.core.api.registry.SourceInfo;
import org.openclover.core.spi.lang.LanguageConstruct;
import org.jetbrains.annotations.Nullable;

public class BasicMethodInfo extends BasicElementInfo {

    private MethodSignature signature;
    private boolean test = false;
    private int dataLength;

    /** Name of the method */
    private transient String name;

    /**
     * Name of the test associated with a method. Some test frameworks can declare a name of the test using annotations,
     * so that JUnit or other frameworks will use the test name and not the original method name in reporting.
     */
    @Nullable
    private String staticTestName;

    private boolean lambda;

    public BasicMethodInfo(SourceInfo region, int relativeDataIndex, int dataLength,
                           int complexity, MethodSignature signature,
                           boolean isTest, @Nullable String staticTestName,
                           boolean isLambda, LanguageConstruct construct) {
        super(region, relativeDataIndex, complexity, construct);
        this.signature = signature;
        this.test = isTest;
        this.lambda = isLambda;
        this.dataLength = dataLength;
        this.name = getNameFor(signature);
        this.staticTestName = staticTestName;
    }

    public BasicMethodInfo(SourceInfo region, int relativeDataIndex, int complexity, MethodSignature signature,
                           boolean isTest, @Nullable String staticTestName, boolean isLambda) {
        this(region, relativeDataIndex, complexity, signature, isTest, staticTestName, isLambda, LanguageConstruct.Builtin.METHOD);
    }

    public BasicMethodInfo(SourceInfo region, int relativeDataIndex, int complexity, MethodSignature signature,
                           boolean isTest, @Nullable String staticTestName, boolean isLambda, LanguageConstruct construct) {
        this(region, relativeDataIndex, 1, complexity, signature, isTest, staticTestName, isLambda, construct);
    }

    private String getNameFor(MethodSignature signature) {
        return signature.getName() + "("+signature.listParamTypes()+")" + ((signature.getReturnType() != null && signature.getReturnType().length() > 0) ? " : " + signature.getReturnType() : "");
    }

    public MethodSignature getSignature() {
        return signature;
    }

    public boolean isLambda() {
        return lambda;
    }

    public boolean isTest() {
        return test;
    }

    @Nullable
    public String getStaticTestName() {
        return staticTestName;
    }

    public void setStaticTestName(@Nullable String testName) {
        staticTestName = testName;
    }

    public int getDataLength() {
        return dataLength;
    }

    public String getName() {
        return name;
    }

    public void setDataLength(int length) {
        this.dataLength = length;
    }

    ///CLOVER:OFF
    @Override
    public String toString() {
        return "BasicMethodInfo{" +
            "name='" + name + '\'' +
            ", test=" + test +
            ", lambda=" + lambda +
            ", signature=" + signature +
            "} " + super.toString();
    }


    ///CLOVER:ON
}
