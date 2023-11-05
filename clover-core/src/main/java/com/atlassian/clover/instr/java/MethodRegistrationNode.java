package com.atlassian.clover.instr.java;

import com.atlassian.clover.api.registry.MethodInfo;
import com.atlassian.clover.cfg.instr.java.JavaInstrumentationConfig;
import com.atlassian.clover.instr.tests.ExpectedExceptionMiner;
import com.atlassian.clover.instr.tests.naming.DefaultTestNameExtractor;
import com.atlassian.clover.instr.tests.naming.JUnitParameterizedTestExtractor;
import com.atlassian.clover.registry.entities.FullMethodInfo;
import com.atlassian.clover.spi.lang.LanguageConstruct;
import com.atlassian.clover.CloverNames;
import com.atlassian.clover.recorder.PerTestRecorder;
import com.atlassian.clover.context.ContextSet;
import com.atlassian.clover.context.MethodRegexpContext;
import com.atlassian.clover.context.NamedContext;
import com.atlassian.clover.registry.FixedSourceRegion;
import com.atlassian.clover.registry.entities.MethodSignature;
import com.atlassian.clover.util.CloverUtils;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.atlassian.clover.instr.Bindings.$CoverageRecorder$globalSliceEnd;
import static com.atlassian.clover.instr.Bindings.$CoverageRecorder$globalSliceStart;
import static com.atlassian.clover.instr.Bindings.$CoverageRecorder$rethrow;
import static org.openclover.util.Lists.newArrayList;
import static org.openclover.util.Maps.newHashMap;

/**

 */
public class MethodRegistrationNode extends Emitter {
    private static final Map DEFAULT_RETURN_VALUES;

    static {
        Map<String, String> values = newHashMap();
        values.put("boolean", "false");
        values.put("byte", "0");
        values.put("short", "0");
        values.put("char", "0");
        values.put("int", "0");
        values.put("long", "0l");
        values.put("float", "0f");
        values.put("double", "0d");
        DEFAULT_RETURN_VALUES = Collections.unmodifiableMap(values);
    }

    private MethodSignature signature;
    private FullMethodInfo method;

    public MethodRegistrationNode(ContextSet context, MethodSignature signature, int line, int col) {
        super(context, line, col);
        this.signature = signature;
    }

    @Override
    protected boolean acceptsContextType(NamedContext context) {
        return context instanceof MethodRegexpContext;
    }

    @Override
    public void init(InstrumentationState state) {
        final JavaInstrumentationConfig cfg = state.getCfg();
        final boolean isTestMethod = state.isDetectTests() && state.getTestDetector().isMethodMatch(state, JavaMethodContext.createFor(signature));
        final String javaLangPrefix = cfg.getJavaLangPrefix();

        boolean addTestRewriteInstr = state.isInstrEnabled() && cfg.isRecordTestResults() && isTestMethod;

        // Check only if any of the previous test methods are not annotated with ParameterizedTest.
        if (isTestMethod && !(state.isParameterizedJUnit5TestClass())) {
            state.setParameterizedJUnit5TestClass(JUnitParameterizedTestExtractor.isJUnit5ParameterizedTest(signature));
        }

        method = (FullMethodInfo) state.getSession().enterMethod(
                getElementContext(),
                new FixedSourceRegion(getLine(), getColumn()),
                signature, isTestMethod, null, false,
                FullMethodInfo.DEFAULT_METHOD_COMPLEXITY, LanguageConstruct.Builtin.METHOD);

        // add information about static test name
        method.setStaticTestName(DefaultTestNameExtractor.INSTANCE.getTestNameForMethod(method));

        if (addTestRewriteInstr) {
            /* Here's how the rewritten test method should look
            int p = 0;
            Throwable t = null;
            try {
                CoverageRecorder.globalSliceStart(getClass().getName(), 298);
                //IF NON-VOID
                ReturnType r =
                //ENDIF NON-VOID
                    syntheticTestMethod(args);
                //IF EXPECTED EXCEPTION
                p = 0
                throw new RuntimeException("Expected one of the following exceptions to be thrown from test method: [FooException]");
                //ELSE
                p = 1
                //IF NON-VOID
                return r;
                //ENDIF NON-VOID
                //ENDIF EXPECTED EXCEPTION
            } catch (Throwable t2) {
                //IF EXPECTED EXCEPTION
                if (t2 instanceof FooException) {
                  p = 1;
                  t = null;
                  CoverageRecorder.rethrow(t2);
                } else {
                //ENDIF EXPECTED EXCEPTION
                  p = 0;
                  t = t2;
                //IF EXPECTED EXCEPTION
                }
                //ENDIF EXPECTED EXCEPTION
                //IF NON-VOID
                return r;
                //ENDIF NON-VOID
            } finally {
                CoverageRecorder.globalSliceEnd(getClass().getName(), "fully.qualified.methodName", 298, p t);
            }
            */

            final List expectedExceptions =
                    state.isDetectTests() ?
                            newArrayList(ExpectedExceptionMiner.extractExpectedExceptionsFor(signature, true))
                            : Collections.emptyList();

            final boolean expectsExceptions = expectedExceptions.size() > 0;

            String syntheticTestName = CloverUtils.createSyntheticTestName(method);
            StringBuilder instr = new StringBuilder();
            instr.append("{");

            String typeInstr = "getClass().getName()";
            if (Modifier.isStatic(getSignature().getBaseModifiersMask())) {
                typeInstr = getMethod().getContainingClass().getName() + ".class.getName()";
            }

            instr.append($CoverageRecorder$globalSliceStart(state.getRecorderPrefix(), typeInstr, Integer.toString(method.getDataIndex()))).append(";");

            instr.append("int ").append(CloverNames.namespace("p")).append("=").append(PerTestRecorder.ABNORMAL_EXIT).append(";");

            instr.append(javaLangPrefix).append("Throwable ").append(CloverNames.namespace("t")).append("=null;");
            instr.append("try{");
            //Not ctors or void-returning methods
            final boolean nonVoidReturn =
                    signature.getReturnType() != null
                            && !"void".equals(signature.getReturnType());

            if (nonVoidReturn) {
                instr.append(signature.getReturnType()).append(" ").append(CloverNames.namespace("r")).append("=");
            }

            instr.append(syntheticTestName);
            instr.append("(").append(signature.listParamIdents()).append(");");
            instr.append(CloverNames.namespace("p")).append("=");
            if (expectsExceptions) {
                instr.append(PerTestRecorder.ABNORMAL_EXIT).append(";").append(CloverNames.namespace("t")).append("=new ").append(javaLangPrefix).append("RuntimeException(");

                StringBuilder msgBuffer = new StringBuilder();
                msgBuffer.append("Expected one of the following exceptions to be thrown from test method ")
                        .append(method.getSimpleName())
                        .append(": ")
                        .append("[");

                for (int i = 0; i < expectedExceptions.size(); i++) {
                    String expectedException = (String) expectedExceptions.get(i);
                    msgBuffer.append(expectedException).append((i < expectedExceptions.size() - 1) ? ", " : "");
                }
                msgBuffer.append("]");

                //Output message as a char array to avoid any encoding issues
                instr.append("new String(new char[] {");

                for (int i = 0; i < msgBuffer.length(); i++) {
                    instr.append((int) msgBuffer.charAt(i))
                            .append(",");
                }

                instr.append("}));");
            } else {
                instr.append(PerTestRecorder.NORMAL_EXIT).append(";");
            }
            if (nonVoidReturn) {
                instr.append("return ").append(CloverNames.namespace("r")).append(";");
            }
            instr.append("}catch(").append(javaLangPrefix).append("Throwable ").append(CloverNames.namespace("t2")).append(")").append("{");

            if (expectsExceptions) {
                instr.append("if(");
                for (int i = 0; i < expectedExceptions.size(); i++) {
                    String expectedException = (String) expectedExceptions.get(i);
                    instr.append(CloverNames.namespace("t2")).append(" instanceof ").append(expectedException);
                    if (i < expectedExceptions.size() - 1) {
                        instr.append("||");
                    }
                }
                instr.append("){").append(CloverNames.namespace("p")).append("=").append(PerTestRecorder.NORMAL_EXIT).append(";").append(CloverNames.namespace("t")).append("=null;}else{").append(CloverNames.namespace("p")).append("=").append(PerTestRecorder.ABNORMAL_EXIT).append(";").append(CloverNames.namespace("t")).append("=").append(CloverNames.namespace("t2")).append(";}");
            }

            instr.append("if(").append(CloverNames.namespace("p")).append("==").append(PerTestRecorder.ABNORMAL_EXIT).append("&&").append(CloverNames.namespace("t")).append("==null){").append(CloverNames.namespace("t")).append("=").append(CloverNames.namespace("t2")).append(";}");

            //Rethrow
            instr.append($CoverageRecorder$rethrow(state.getRecorderPrefix(), CloverNames.namespace("t2"))).append(";");
            if (nonVoidReturn) {
                instr.append("return ").append(defaultNullValueForType(signature.getReturnType())).append(";");
            }

            instr.append("}");

            instr.append("finally{");

            instr.append($CoverageRecorder$globalSliceEnd(state.getRecorderPrefix(), typeInstr,
                    "\"" + method.getQualifiedName() + "\"",
                    CloverNames.CLOVER_TEST_NAME_SNIFFER + ".getTestName()",
                    Integer.toString(method.getDataIndex()), CloverNames.namespace("p"), CloverNames.namespace("t")));
            instr.append(";");
            instr.append("}}");
            instr.append(signature.getRenamedNormalisedSignature(syntheticTestName));

            setInstr(instr.toString());
        }
    }

    private static String defaultNullValueForType(String returnType) {
        return String.valueOf(DEFAULT_RETURN_VALUES.get(returnType));
    }

    public MethodSignature getSignature() {
        return signature;
    }

    public MethodInfo getMethod() {
        return method;
    }

    @Override
    public void addContext(NamedContext ctx) {
        super.addContext(ctx);
        if (method != null) {
            method.addContext(ctx);
        }
    }
}
