package info.kgeorgiy.ja.ilyin.arrayset;

import java.util.*;

/**
 * @author Yaroslav Ilin
 */

public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {
    private final List<T> data;
    private final Comparator<? super T> comparator;

    public ArraySet() {
        this(List.of(), null);
    }

    public ArraySet(final Collection<? extends T> collection) {
        this(collection, null);
    }

    public ArraySet(final Comparator<? super T> comparator) {
        this(Collections.emptyList(), comparator);
    }

    public ArraySet(final Collection<? extends T> collection, final Comparator<? super T> comparator) {
        final TreeSet<T> tmp = new TreeSet<>(comparator);
        tmp.addAll(collection);
        this.data = new ArrayList<>(tmp);
        this.comparator = comparator;
    }

    private ArraySet(final List<T> list, final Comparator<? super T> comparator) {
        this.comparator = comparator;
        this.data = list;
    }

    private int findInd(final T key, final int ifFind, final int notFind) {
        final int index = Collections.binarySearch(data, key, comparator);
        return index >= 0 ? index + ifFind : -index - 1 + notFind;
    }

    private T get(final int ind) {
        return ind < 0 || size() <= ind ? null : data.get(ind);
    }

    @Override
    public T lower(final T key) {
        return get(findInd(key, -1, -1));
    }

    @Override
    public T floor(final T key) {
        return get(findInd(key, 0, -1));
    }

    @Override
    public T ceiling(final T key) {
        return get(findInd(key, 0, 0));
    }

    @Override
    public T higher(final T key) {
        return get(findInd(key, 1, 0));
    }

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException("operation in not supported");
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException("operation is not supported");
    }

    // :NOTE: Новый unmodifiableList
    @Override
    public Iterator<T> iterator() {
        return Collections.unmodifiableList(data).iterator();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(final Object o) {
        return Collections.binarySearch(data, (T) o, comparator) >= 0;
    }

    @Override
    public NavigableSet<T> descendingSet() {
        return new ArraySet<>(new ReversedList<>(data), Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<T> descendingIterator() {
        return descendingSet().iterator();
    }

    @SuppressWarnings("unchecked")
    private int compare(final T a, final T b) {
        if (comparator == null) {
            return ((Comparable<T>) a).compareTo(b);
        }
        return comparator.compare(a, b);
    }

    private ArraySet<T> sub(final int left, final int right) {
        if (left > right) {
            return new ArraySet<>(comparator);
        }
        return new ArraySet<>(data.subList(left, right), comparator);
    }


    @Override
    public NavigableSet<T> subSet(final T fromElement, final boolean fromInclusive, final T toElement, final boolean toInclusive) {
        if (compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException("wrong range of subSet");
        }
        final int left = findInd(fromElement, fromInclusive ? 0 : 1, 0);
        final int right = findInd(toElement, toInclusive ? 1 : 0, 0);
        return sub(left, right);
    }

    @Override
    public NavigableSet<T> headSet(final T toElement, final boolean inclusive) {
        final int right = findInd(toElement, inclusive ? 1 : 0, 0);
        return sub(0, right);
    }

    @Override
    public NavigableSet<T> tailSet(final T fromElement, final boolean inclusive) {
        final int left = findInd(fromElement, inclusive ? 0 : 1, 0);
        return sub(left, size());
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<T> subSet(final T fromElement, final T toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<T> headSet(final T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<T> tailSet(final T fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public T first() {
        if (data.isEmpty()) {
            throw new NoSuchElementException("set is empty");
        }
        return data.get(0);
    }

    @Override
    public T last() {
        if (data.isEmpty()) {
            throw new NoSuchElementException("set is empty");
        }
        return data.get(size() - 1);
    }

    @Override
    public int size() {
        return data.size();
    }
}
