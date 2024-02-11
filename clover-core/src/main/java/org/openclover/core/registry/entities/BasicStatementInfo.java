package org.openclover.core.registry.entities;

import org.openclover.core.api.registry.SourceInfo;
import org.openclover.core.spi.lang.LanguageConstruct;

public class BasicStatementInfo extends BasicElementInfo {
    public BasicStatementInfo(SourceInfo region, int relativeDataIndex, int complexity, LanguageConstruct construct) {
        super(region, relativeDataIndex, complexity, construct);
    }
    
    public BasicStatementInfo(SourceInfo region, int relativeDataIndex, int complexity) {
        this(region, relativeDataIndex, complexity, LanguageConstruct.Builtin.STATEMENT);
    }
}
