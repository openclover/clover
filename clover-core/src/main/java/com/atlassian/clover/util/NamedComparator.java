package com.atlassian.clover.util;

import java.util.Comparator;

public interface NamedComparator<T> extends Comparator<T>
{
    String getName();

    String getDescription();

}
