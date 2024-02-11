package org.openclover.core.spi.lang;

import org.openclover.core.api.registry.SourceInfo;
import org.openclover.core.reporters.html.source.SourceRenderHelper;

import java.text.MessageFormat;
import java.util.Locale;

public interface LanguageConstruct {
    /**
     * @return a unique, unchanging ID for the language construct. It should be as short as possible as it is stored in the clover database for each occurance of the construct.
     */
    String getId();

    /**
     * Calculates the message to be shown for the construct given its location in source, its coverage count and the current locale
     * @param sourceRegion the region in source where the construct occurred
     * @param trueBranchCount the coverage count for the true branch. If the construct is not a branch, the coverage is supplied here
     * @param falseBranchCount the coverage count for the false branch or 0 if the construct doesn't support false branches
     * @param locale the locale for the message
     * @return String message
     */
    String calcCoverageMsg(SourceInfo sourceRegion, int trueBranchCount, int falseBranchCount, Locale locale);

    /**
     * Language constructs that Clover supports for the languages it supports out-of-the-box.
     */
    enum Builtin implements LanguageConstruct {
        METHOD("()", "method {2,choice,0#not entered|1#entered {2,number,integer} time|1<entered {2,number,integer} times}."),
        STATEMENT(";", "statement {2,choice,0#not executed|1#executed 1 time|1<executed {2,number,integer} times}."),
        BRANCH("?", "true branch executed {2} {2,choice,0#times|1#time|1<times}, false branch executed {3} {3,choice,0#times|1#time|1<times}."),
        GROOVY_FIELD_EXPRESSION("=", "field {2,choice,0#not initialised|1#initialised 1 time|1<initialised {2,number,integer} times}."),
        GROOVY_SAFE_METHOD("?()", "safe method call had non-null target {2} {2,choice,0#times|1#time|1<times}, had null target {3} {3,choice,0#times|1#time|1<times}."),
        GROOVY_SAFE_PROPERTY("?P", "safe property call had non-null target {2} {2,choice,0#times|1#time|1<times}, had null target {3} {3,choice,0#times|1#time|1<times}."),
        GROOVY_SAFE_ATTRIBUTE("?A", "safe attribute call had non-null target {2} {2,choice,0#times|1#time|1<times}, had null target {3} {3,choice,0#times|1#time|1<times}."),
        GROOVY_ELVIS_OPERATOR("?:", "elvis expression defaulted {2} {2,choice,0#times|1#time|1<times}, evaluated alternate expression  {3} {3,choice,0#times|1#time|1<times}.");

        private final String id;
        private final MessageFormat msgFormat;

        Builtin(String id, String msgFormat) {
            this.id = id;
            this.msgFormat = new MessageFormat(msgFormat);
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String calcCoverageMsg(SourceInfo sourceRegion, int trueBranchCount, int falseBranchCount, Locale locale) {
            return SourceRenderHelper.getRegionStartStr(sourceRegion) + msgFormat.format(new Object[] { sourceRegion.getStartLine(), sourceRegion.getStartColumn(), trueBranchCount, falseBranchCount });
        }
    }
}
