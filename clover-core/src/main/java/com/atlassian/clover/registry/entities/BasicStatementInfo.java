package com.atlassian.clover.registry.entities;

import com.atlassian.clover.api.registry.SourceInfo;
import com.atlassian.clover.spi.lang.LanguageConstruct;

public class BasicStatementInfo extends BasicElementInfo {
    public BasicStatementInfo(SourceInfo region, int relativeDataIndex, int complexity, LanguageConstruct construct) {
        super(region, relativeDataIndex, complexity, construct);
    }
    
    public BasicStatementInfo(SourceInfo region, int relativeDataIndex, int complexity) {
        this(region, relativeDataIndex, complexity, LanguageConstruct.Builtin.STATEMENT);
    }
}
