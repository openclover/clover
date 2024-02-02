package org.openclover.idea.feature;

public interface CloverFeatures {
    String CLOVER = "clover";
    String REPORTING = "reporting";
    String BUILDING = "building";
    String TOOLTIPS = "tooltips";
    String INLINE = "inline";
    String GUTTER = "gutter";
    String ERRORMARKS = "errormarks";
    String REFRESH = "refresh";
    String ICON_DECORATION = "icondecoration";

    String CLOVER_REPORTING = CLOVER + "-" + REPORTING;
    String CLOVER_REFRESH = CLOVER + "-" + REFRESH;
    String CLOVER_BUILDING = CLOVER + "-" + BUILDING;
    String CLOVER_ICON_DECORATION = CLOVER + "-" + ICON_DECORATION;
    String CLOVER_REPORTING_TOOLTIPS = CLOVER + "-" + REPORTING + "-" + TOOLTIPS;
}
