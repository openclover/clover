package com.atlassian.clover.eclipse.core.ui.editors.java;

import java.util.Comparator;

class CoverageEdgeComparator implements Comparator<CoverageEdge> {
    @Override
    public int compare(CoverageEdge o1, CoverageEdge o2) {
        if (o1 instanceof CoverageEnd && o2 instanceof CoverageBeginning
            || o1 instanceof CoverageBeginning && o2 instanceof CoverageEnd) {

            if (o1.getColumn() < o2.getColumn()) {
                return -1;
            } else if (o1.getColumn() > o2.getColumn()) {
                return 1;
            } else {
                return o1 instanceof CoverageEnd ? -1 : 1;
            }

        } else if (o1 instanceof CoverageBeginning && o2 instanceof CoverageBeginning) {
            int startingLine1 = o1.getInfo().getStartLine();
            int startingColumn1 = o1.getInfo().getStartColumn();
            int startingLine2 = o2.getInfo().getStartLine();
            int startingColumn2 = o2.getInfo().getStartColumn();

            int frontDisposition = disposition(startingLine1, startingColumn1, startingLine2, startingColumn2);
            if (frontDisposition == 0) {
                //Co-located beginnings should be ordered by their endings -
                //those ending first should be ordered first so their
                //corresponding CoverageEnd is delivered in the right order
                int endingLine1 = o1.getInfo().getEndLine();
                int endingColumn1 = o1.getInfo().getEndColumn();
                int endingLine2 = o2.getInfo().getEndLine();
                int endingColumn2 = o2.getInfo().getEndColumn();

                int backDisposition = disposition(endingLine2, endingColumn2, endingLine1, endingColumn1);
                if (backDisposition == 0) {
                    return compareIdentityHashCodeOfInfo(o1, o2);
                } else {
                    return backDisposition;
                }
            } else {
                return frontDisposition;
            }
        } else if (o1 instanceof CoverageEnd && o2 instanceof CoverageEnd) {
            int endingLine1 = o1.getInfo().getEndLine();
            int endingColumn1 = o1.getInfo().getEndColumn();
            int endingLine2 = o2.getInfo().getEndLine();
            int endingColumn2 = o2.getInfo().getEndColumn();

            int backDisposition = disposition(endingLine1, endingColumn1, endingLine2, endingColumn2);
            if (backDisposition == 0) {
                int startingLine1 = o1.getInfo().getStartLine();
                int startingColumn1 = o1.getInfo().getStartColumn();
                int startingLine2 = o2.getInfo().getStartLine();
                int startingColumn2 = o2.getInfo().getStartColumn();

                int frontDisposition = disposition(startingLine2, startingColumn2, startingLine1, startingColumn1);
                if (frontDisposition == 0) {
                    return compareIdentityHashCodeOfInfo(o1, o2);
                } else {
                    return frontDisposition;
                }
            } else {
                return backDisposition;
            }
        } else {
            throw new AssertionError("Edges should only be of type CoverageBeginning or CoverageEnding");
        }
    }

    private int disposition(int startingLine1, int startingColumn1, int startingLine2, int startingColumn2) {
        if (startingLine1 < startingLine2) {
            return -1;
        } else if (startingLine1 > startingLine2) {
            return 1;
        } else {
            return Integer.compare(startingColumn1, startingColumn2);
        }
    }

    private int compareIdentityHashCodeOfInfo(CoverageEdge o1, CoverageEdge o2) {
        //TODO: verify all main JVMs produce different hash values for o1 & o2 when !o1.equals(o2)
        int hashCode1 = System.identityHashCode(o1.getInfo());
        int hashCode2 = System.identityHashCode(o2.getInfo());

        if (hashCode1 == hashCode2) {
            return 0;
        } else if (o1 instanceof CoverageBeginning && hashCode1 < hashCode2) {
            return -1;
        } else if (o1 instanceof CoverageBeginning && hashCode1 > hashCode2) {
            return 1;
        } else if (o1 instanceof CoverageEnd && hashCode1 < hashCode2) {
            return 1;
        } else {
            return -1;
        }
    }
}
