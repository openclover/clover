package org.openclover.runtime.remote;


import org.openclover.runtime.ErrorInfo;

/**
 * In-process value holder for a distributed-coverage event: an opcode plus its arguments in declared order.
 * It is never serialized as an object - {@link MessageCodec} encodes it field-by-field onto the wire.
 */
public class RpcMessage {

    private final Integer methodId;
    private final Object[] methodArgs;

    public static final int METHOD_START = 1;
    public static final int METHOD_END = 2;

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

    public int getMethodId() {
        return methodId;
    }

    /** Human-readable name of the whitelisted {@link org_openclover_runtime.Clover} method - used for debug logging only. */
    public String getName() {
        switch (methodId) {
            case METHOD_START:
                return "allRecordersSliceStart";
            case METHOD_END:
                return "allRecordersSliceEnd";
            default:
                return "unknown(" + methodId + ")";
        }
    }

    public Object[] getMethodArgs() {
        return methodArgs;
    }

}
