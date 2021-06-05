package info.kgeorgiy.ja.ilyin.arrayset;

import java.util.AbstractList;
import java.util.List;

/**
 * @author Yaroslav Ilin
 */

public class ReversedList<T> extends AbstractList<T> {
    private final List<T> data;
    private final boolean reversed;

    public ReversedList(final List<T> data) {
        if (data.getClass() == ReversedList.class) {
            final ReversedList<T> tmp = (ReversedList<T>) data;
            this.data = tmp.data;
            this.reversed = !tmp.reversed;
        } else {
            this.data = data;
            this.reversed = true;
        }
    }

    @Override
    public T get(final int index) {
        if (reversed) {
            return data.get(size() - 1 - index);
        }
        return data.get(index);
    }

    @Override
    public int size() {
        return data.size();
    }
}
