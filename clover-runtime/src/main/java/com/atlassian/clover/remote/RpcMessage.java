package com.atlassian.clover.remote;


import com.atlassian.clover.ErrorInfo;

import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;

/**
 *
 */
public class RpcMessage implements Serializable {

    public static final long serialVersionUID = 1l;
    
    private final Integer methodId;
    private final Object[] methodArgs;

    public static final int METHOD_START = 1;
    public static final int METHOD_END = 2;

    private static final Map<Integer, MethodDescriptor> METHODS = new HashMap<>();

    static {
        METHODS.put(METHOD_START, new MethodDescriptor("allRecordersSliceStart",
                String.class, int.class, long.class));
        METHODS.put(METHOD_END, new MethodDescriptor("allRecordersSliceEnd",
                String.class, String.class, String.class, int.class, int.class, ErrorInfo.class));
    }

    public static RpcMessage createMethodStart(String type, int slice, long currentSliceStart) {
        return new RpcMessage(RpcMessage.METHOD_START, type, slice, currentSliceStart);
    }

    public static RpcMessage createMethodEnd(String type, String method, String runtimeTestName,
                                             int slice, int p, ErrorInfo errorInfo) {
        return new RpcMessage(RpcMessage.METHOD_END, type, method, runtimeTestName, slice, p, errorInfo);
    }

    private RpcMessage(Integer methodId, Object... methodArgs) {
        this.methodId = methodId;
        this.methodArgs = methodArgs;
    }

    private RpcMessage() {
        methodId = null;
        methodArgs = null;
    }

    public String getName() {
        MethodDescriptor desc = METHODS.get(methodId);
        return desc.name;
    }

    public Object[] getMethodArgs() {
        return methodArgs;
    }

    static class MethodDescriptor {
        final String name;
        final Class[] argTypes;

        MethodDescriptor(String name, Class... argTypes) {
            this.name = name;
            this.argTypes = argTypes;
        }
    }

}
