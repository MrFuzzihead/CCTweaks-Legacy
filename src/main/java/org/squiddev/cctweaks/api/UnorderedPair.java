package org.squiddev.cctweaks.api;

public class UnorderedPair<T> {

    public final T x;
    public final T y;

    public UnorderedPair(T x, T y) {
        this.x = x;
        this.y = y;
    }

    public T other(T obj) {
        if (obj.equals(this.x)) {
            return this.y;
        } else {
            return obj.equals(this.y) ? this.x : null;
        }
    }

    public boolean contains(Object z) {
        return this.x.equals(z) || this.y.equals(z);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else {
            if (other instanceof UnorderedPair) {
                UnorderedPair<?> pair = (UnorderedPair<?>) other;
                if (this.x.equals(pair.x) && this.y.equals(pair.y)) {
                    return true;
                }

                if (this.y.equals(pair.x) && this.x.equals(pair.y)) {
                    return true;
                }
            }

            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.x.hashCode() ^ this.y.hashCode();
    }

    @Override
    public String toString() {
        return String.format("<%s, %s>", this.x, this.y);
    }
}
