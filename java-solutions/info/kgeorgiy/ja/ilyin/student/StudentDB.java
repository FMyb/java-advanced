package info.kgeorgiy.ja.ilyin.student;

import info.kgeorgiy.java.advanced.student.*;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Yaroslav Ilin
 */

public class StudentDB implements AdvancedQuery {
    private final static Comparator<Student> ORDERED_BY_NAME = Comparator
            .comparing(Student::getLastName, Comparator.reverseOrder())
            .thenComparing(Student::getFirstName, Comparator.reverseOrder())
            .thenComparingInt(Student::getId);

    private final static Comparator<Group> GROUP_COMPARATOR = Comparator.comparing(Group::getName);

    private Stream<Map.Entry<GroupName, List<Student>>> getStudentsStreamByGroup(Collection<Student> students) {
        return students.stream()
                .collect(Collectors.groupingBy(Student::getGroup))
                .entrySet().stream();
    }

    private List<Group> getGroupsBy(Collection<Student> students, Comparator<Student> comparator) {
        return getStudentsStreamByGroup(students)
                .map(x -> new Group(x.getKey(), x.getValue().stream().sorted(comparator).collect(Collectors.toList())))
                .sorted(GROUP_COMPARATOR)
                .collect(Collectors.toList());
    }

    private Function<Student, String> getFullName() {
        return student -> student.getFirstName() + " " + student.getLastName();
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getGroupsBy(students, ORDERED_BY_NAME);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return getGroupsBy(students, Comparator.comparingInt(Student::getId));
    }

    private GroupName getMaxGroupBy(Collection<Student> students, Comparator<Group> comparator) {
        return getStudentsStreamByGroup(students)
                .map(x -> new Group(x.getKey(), x.getValue()))
                .max(comparator)
                .map(Group::getName)
                .orElse(null);
    }

    private Comparator<Group> groupComparator(Function<Group, Integer> cmp) {
        return Comparator.comparing(cmp).thenComparing(GROUP_COMPARATOR);
    }

    @Override
    public GroupName getLargestGroup(Collection<Student> students) {
        return getMaxGroupBy(students, groupComparator(g -> g.getStudents().size()));
    }

    private <T, R> R getMaxGroupBy(Collection<Student> students, Function<Student, R> key,
                                   Function<Student, T> param, Comparator<Map.Entry<R, Integer>> thenComp, R orElse) {
        return students.stream()
                .collect(Collectors.groupingBy(
                        key,
                        Collectors.collectingAndThen(
                                Collectors.groupingBy(param, Collectors.counting()), Map::size)

                ))
                .entrySet().stream()
                .max(Comparator.comparing((Function<Map.Entry<R, Integer>, Integer>) Map.Entry::getValue)
                        .thenComparing(thenComp))
                .map(Map.Entry::getKey).orElse(orElse);
    }

    @Override
    public GroupName getLargestGroupFirstName(Collection<Student> students) {
        return getMaxGroupBy(students, Student::getGroup, Student::getFirstName,
                Map.Entry.<GroupName, Integer>comparingByKey().reversed(), null);
    }

    @Override
    public String getMostPopularName(Collection<Student> students) {
        return getMaxGroupBy(students,
                Student::getFirstName,
                Student::getGroup,
                Map.Entry.comparingByKey(), "");
    }

    private <T> List<T> getStudentsInfo(List<Student> students, Function<Student, T> f) {
        return students.stream().map(f).collect(Collectors.toList());
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return getStudentsInfo(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return getStudentsInfo(students, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(List<Student> students) {
        return getStudentsInfo(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return getStudentsInfo(students, getFullName());
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return students.stream().map(Student::getFirstName)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMaxStudentFirstName(List<Student> students) {
        return students.stream().max(Comparator.comparingInt(Student::getId)).map(Student::getFirstName).orElse("");
    }

    private List<Student> sortStudentsBy(Collection<Student> students, Comparator<Student> cmp) {
        return students.stream().sorted(cmp).collect(Collectors.toList());
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortStudentsBy(students, Comparator.comparingInt(Student::getId));
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortStudentsBy(students, ORDERED_BY_NAME);
    }

    private List<Student> findStudentsBy(Collection<Student> students, Predicate<Student> check) {
        return students.stream().filter(check)
                .sorted(ORDERED_BY_NAME)
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return findStudentsBy(students, student -> student.getFirstName().equals(name));
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return findStudentsBy(students, student -> student.getLastName().equals(name));
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
        return findStudentsBy(students, student -> student.getGroup().equals(group));
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
        return students.stream().filter(student -> student.getGroup().equals(group))
                .collect(
                        Collectors.groupingBy(
                                Student::getLastName,
                                Collectors.collectingAndThen(
                                        Collectors.minBy(Comparator.comparing(Student::getFirstName)),
                                        student -> student.map(Student::getFirstName).orElse(null))));
    }

    private <T> List<T> getByInd(Collection<Student> students, Function<Student, T> f, int[] indeces) {
        return Arrays.stream(indeces)
                .mapToObj(new ArrayList<>(students)::get)
                .map(f)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getFirstNames(Collection<Student> students, int[] indices) {
        return getByInd(students, Student::getFirstName, indices);
    }

    @Override
    public List<String> getLastNames(Collection<Student> students, int[] indices) {
        return getByInd(students, Student::getLastName, indices);
    }

    @Override
    public List<GroupName> getGroups(Collection<Student> students, int[] indices) {
        return getByInd(students, Student::getGroup, indices);
    }

    @Override
    public List<String> getFullNames(Collection<Student> students, int[] indices) {
        return getByInd(students, getFullName(), indices);
    }
}
