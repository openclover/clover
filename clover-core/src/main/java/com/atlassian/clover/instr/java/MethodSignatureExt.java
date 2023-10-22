package com.atlassian.clover.instr.java;

import com.atlassian.clover.registry.entities.MethodSignature;

/**
 * A triple of: methodSignature, pointer to the last token of the signature and whether it belongs to
 * a deprecated context. Returned by compact constructors, standard constructors and methods
 * in JavaRecognizer.
 */
public class MethodSignatureExt {
    private final MethodSignature signature;
    private final CloverToken endToken;
    private final Boolean deprecated;

    public static MethodSignatureExt of(MethodSignature signature, CloverToken endToken, Boolean deprecated) {
        return new MethodSignatureExt(signature, endToken, deprecated);
    }

    private MethodSignatureExt(MethodSignature signature, CloverToken endToken, Boolean deprecated) {
        this.signature = signature;
        this.endToken = endToken;
        this.deprecated = deprecated;
    }

    public MethodSignature signature() {
        return signature;
    }

    public CloverToken endToken() {
        return endToken;
    }

    public Boolean isDeprecated() {
        return deprecated;
    }
}
