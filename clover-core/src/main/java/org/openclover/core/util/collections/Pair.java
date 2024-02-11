package org.openclover.core.util.collections;

import java.util.Objects;

/** Special thanks to Dick Wall */
public class Pair<A, B> {
    public final A first;
    public final B second;

    private Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }

    public static <A,B> Pair<A,B> of(A first, B second) {
       return new Pair<>(first, second);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Pair other = (Pair) obj;
        if (!Objects.equals(this.first, other.first)) {
            return false;
        }
        return Objects.equals(this.second, other.second);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + (this.first != null ?
                              this.first.hashCode() : 0);
        hash = 37 * hash + (this.second != null ? this.second.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString () {
        return String.format("Pair[%s,%s]", first,second);
    }
}