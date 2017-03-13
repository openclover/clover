package com.atlassian.clover.eclipse.core.ui.editors.java.annotations.strategies.space;

import org.eclipse.jface.text.Position;

import java.util.Comparator;

public class AnnotationFragmentComparator implements Comparator<AnnotationFragment> {
    @Override
    public int compare(AnnotationFragment annotation1, AnnotationFragment annotation2) {
        Position position1 = annotation1.getPosition();
        Position position2 = annotation2.getPosition();
        if (position1.getOffset() < position2.getOffset()) {
            return -1;
        } else if (position1.getOffset() > position2.getOffset()) {
            return 1;
        } else {
            if (position1.getLength() < position2.getLength()) {
                return -1;
            } else if (position1.getLength() > position2.getLength()) {
                return 1;
            } else {
                return 0;
            }
        }
    }

}
