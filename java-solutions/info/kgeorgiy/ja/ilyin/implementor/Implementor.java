package info.kgeorgiy.ja.ilyin.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

/**
 * The class is used to create implementations (extensions) of interfaces (classes)
 * Creates <code>.java</code> and <code>.jar</code> files
 *
 * @author Yaroslav Ilin
 */
public class Implementor implements JarImpler {
    /**
     * Constant equal to one path separator.
     */
    private static final String PATH_SEPARATOR = "/";

    /**
     * Constant equal to <code>Impl</code> class suffix.
     */
    private static final String IMPL_SUFFIX = "Impl";

    /**
     * Constant equal to <code>.java</code> file type.
     */
    private static final String JAVA_SUFFIX = ".java";

    /**
     * Constant equal to <code>.class</code> file type.
     */
    private static final String CLASS_SUFFIX = ".class";

    /**
     * Constant equal to one line separator.
     */
    private static final String END_LINE = System.lineSeparator();

    /**
     * Constant equal to one semicolon.
     */
    private static final String SEMICOLON = ";";

    /**
     * Constant equal to one open brace.
     */
    private static final String OPEN_BRACE = "{";

    /**
     * Constant equal to one close brace.
     */
    private static final String CLOSE_BRACE = "}";

    /**
     * Constant equal to one open parenthesis.
     */
    private static final String OPEN_PARENTHESIS = "(";

    /**
     * Constant equal to one close parenthesis.
     */
    private static final String CLOSE_PARENTHESIS = ")";

    /**
     * Constant equal to one comma.
     */
    private static final String COMMA = ",";

    /**
     * Constant equal to {@link java.lang.Override} anotation.
     */
    private static final String OVERRIDE = "@Override";

    /**
     * Comparator for compare {@link java.lang.reflect.Method}
     */
    private static final Comparator<Method> METHOD_COMPARATOR = Comparator.comparing(
            method -> (method.getName().hashCode() ^ Arrays.hashCode(method.getParameterTypes())));

    /**
     * {@link java.nio.file.FileVisitor} for delete temp directory
     */
    private static final SimpleFileVisitor<Path> DELETE_VISITOR = new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(final Path directory, final IOException exc) throws IOException {
            Files.delete(directory);
            return FileVisitResult.CONTINUE;
        }
    };

    /**
     * Default constructor;
     */
    public Implementor() {
    }

    /**
     * By arguments, implements the class and puts its implementation along the path.
     *
     * @param args <ul>
     *             <li>if {@code args.length == 2} expected class in first argument and path in second argument</li>
     *             <li>if {@code args.length == 3} expected <code>-jar</code> in first argument and class and path in
     *             first and second argument</li>
     *             </ul>
     */
    public static void main(String[] args) {
        try {
            checkArgs(args);
            Implementor implementor = new Implementor();
            if (args.length == 2) {
                implementor.implement(classFromString(args[0]), pathFromString(args[1]));
            } else {
                implementor.implementJar(classFromString(args[1]), pathFromString(args[2]));
            }
        } catch (ImplerException e) {
            e.printStackTrace();
        }
    }

    /**
     * Invokes {@link Class#forName} for given class
     *
     * @param className name of class
     * @return {@code Class} object
     * @throws ImplerException if class have incorrect name.
     */
    private static Class<?> classFromString(String className) throws ImplerException {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new ImplerException("Invalid class name " + className, e);
        }
    }

    /**
     * Invokes {@link Path#of} for given path sequence
     *
     * @param path first string in sequence
     * @param more strings appended to the path
     * @return resulting {@code Path}
     */
    private static Path pathFromString(String path, String... more) {
        return Path.of(path, more);
    }

    /**
     * Checking command line arguments for correctness
     *
     * @param args command line arguments.
     * @throws ImplerException if wrong arguments
     */
    private static void checkArgs(String[] args) throws ImplerException {
        if (args == null || args.length != 2 && args.length != 3) {
            throw new ImplerException("wrong count of arguments");
        }
        for (String arg : args) {
            if (arg == null) {
                throw new ImplerException("expected not null arguments");
            }
        }
        if (args.length == 3 && !args[0].equals("-jar")) {
            throw new ImplerException("wrong arguments");
        }
    }

    /**
     * Generate a name what the class implements (extends)
     *
     * @param token type token of implementing (extending) class.
     * @return resulting {@code String}
     * @throws ImplerException if can't implement or extends {@code token}.
     */
    private String getImplements(Class<?> token) throws ImplerException {
        if (token.isPrimitive() ||
                token == Enum.class || // :NOTE: does not catch enums
                token.isEnum() ||  // :FIX
                token.isArray() ||
                Modifier.isPrivate(token.getModifiers()) ||
                Modifier.isFinal(token.getModifiers())) {
            throw new ImplerException("Can't implement void");
        }
        if (token.isInterface()) {
            return " implements " + token.getCanonicalName();
        }
        return " extends " + token.getCanonicalName();
    }

    /**
     * Generate access modifiers from the constructor.
     *
     * @param constructor constructor token to generate access modifiers for.
     * @return resulting {@code String}.
     */
    private String getConstructorAccess(Constructor<?> constructor) {
        return Modifier.toString(constructor.getModifiers() & ~Modifier.TRANSIENT & ~Modifier.ABSTRACT);
    }

    /**
     * Generate super constructor calls
     *
     * @param parameters parameters for call super constructor;
     * @return resulting {@code String}
     */
    private String getConstructorSuper(String parameters) {
        return "super" + OPEN_PARENTHESIS + parameters + CLOSE_PARENTHESIS + SEMICOLON + END_LINE;
    }

    /**
     * Generate constructor throws.
     *
     * @param constructor constructor token to generating.
     * @return resulting {@code String}
     */
    private String getConstructorThrows(Constructor<?> constructor) {
        if (constructor.getExceptionTypes().length == 0) return "";
        // :NOTE-2: please, understand why you do this
        return "throws " + Arrays.stream(constructor.getExceptionTypes())
                .map(Class::getCanonicalName)
                .collect(Collectors.joining(","));
    }

    /**
     * Generate constructor implementation.
     *
     * @param constructor constructor token to generating
     * @param token       type token to generate the name of constructor
     * @return resulting {@code String}
     */
    private String getConstructorImpl(Constructor<?> constructor, Class<?> token) {
        StringJoiner parameters = new StringJoiner(COMMA);
        StringJoiner parameterNames = new StringJoiner(COMMA);
        for (Parameter parameter : constructor.getParameters()) {
            Class<?> type = parameter.getType();
            parameters.add(getClassName(type) + " " + parameter.getName());
            parameterNames.add(parameter.getName());
        }
        return getConstructorAccess(constructor) + " " +
                token.getSimpleName() + IMPL_SUFFIX +
                OPEN_PARENTHESIS + parameters.toString() + CLOSE_PARENTHESIS +
                getConstructorThrows(constructor) + OPEN_BRACE + END_LINE +
                getConstructorSuper(parameterNames.toString()) +
                CLOSE_BRACE + END_LINE;
    }

    /**
     * Generate all constructors
     *
     * @param token the type token from which to generate constructors.
     * @return resulting {@code String}
     * @throws ImplerException if class token don't have non private constructor
     */
    private String getConstructors(Class<?> token) throws ImplerException {
        StringBuilder constructorsImpl = new StringBuilder();
        Constructor<?>[] constructors = token.getDeclaredConstructors();
        for (Constructor<?> constructor : constructors) {
            if (!Modifier.isPrivate(constructor.getModifiers())) {
                constructorsImpl.append(getConstructorImpl(constructor, token));
            }
        }
        if (!token.isInterface() && constructorsImpl.toString().isEmpty()) {
            throw new ImplerException("Can't find non private constructor");
        }
        return constructorsImpl.toString();
    }

    /**
     * Generate a class declaration for {@code .java} files
     *
     * @param token type token of implementing class
     * @return resulting {@code String}
     * @throws ImplerException if {@link #getImplements} throws exception.
     */
    private String getHeader(Class<?> token) throws ImplerException {
        // :NOTE: package name might be empty
        return (token.getPackageName().isEmpty() ? "" : "package " + token.getPackageName() + SEMICOLON + END_LINE) +
                "public class " +
                token.getSimpleName() + "Impl" + getImplements(token) + " "
                + OPEN_BRACE + END_LINE;
    }

    /**
     * Generate class name.
     * Return the correct class name (if type is array returning name with {@code []}).
     *
     * @param type type token of generating name.
     * @return resulting {@code String}
     */
    private String getClassName(Class<?> type) {
        return type.isArray() ? (type.getComponentType().getCanonicalName() + "[]") : (type.getCanonicalName());
    }

    /**
     * Generate access modifiers from the method.
     *
     * @param method method token of generating access modifiers.
     * @return resulting {@code String}
     */
    private String getMethodAccess(Method method) {
        return Modifier.toString(method.getModifiers() & ~Modifier.TRANSIENT & ~Modifier.ABSTRACT);
    }

    /**
     * Generate {@link java.lang.Override} annotation if method is not static.
     *
     * @param method method token of generating annotation.
     * @return resulting {@code String}
     */
    private String getOverride(Method method) {
        return Modifier.isStatic(method.getModifiers()) ? "" : OVERRIDE + END_LINE;
    }

    /**
     * Generate default implementation for method
     *
     * @param method method token to generating implementation
     * @return resulting {@code String}
     */
    private String getImpl(Method method) {
        String ans = "null";
        Class<?> returnType = method.getReturnType();
        if (returnType.equals(Void.TYPE)) {
            ans = "";
        } else if (returnType.equals(Boolean.TYPE)) {
            ans = "false";
        } else if (returnType.isPrimitive()) {
            ans = "0";
        }
        return "return " + ans + SEMICOLON + END_LINE;
    }

    /**
     * Generate method implementation.
     *
     * @param method method token to generate.
     * @return resulting {@code String}
     */
    private String getMethodImpl(Method method) {
        int modifiers = method.getModifiers();
        if (!Modifier.isAbstract(modifiers) || method.isDefault() || Modifier.isFinal(modifiers) ||
                Modifier.isNative(modifiers) || Modifier.isPrivate(modifiers)) return "";
        StringJoiner parameters = new StringJoiner(COMMA);
        for (Parameter parameter : method.getParameters()) {
            Class<?> type = parameter.getType();
            parameters.add(getClassName(type) + " " + parameter.getName());
        }
        return getOverride(method) + getMethodAccess(method) + " " +
                getClassName(method.getReturnType()) + " " +
                method.getName() +
                OPEN_PARENTHESIS + parameters.toString() + CLOSE_PARENTHESIS + OPEN_BRACE + END_LINE +
                getImpl(method) + CLOSE_BRACE + END_LINE;
    }

    /**
     * Generate all methods implementation
     *
     * @param token type token to generate methods
     * @return resulting {@code String}
     */
    private String getMethodsImpl(Class<?> token) {
        StringBuilder methodsImpl = new StringBuilder();
        // :NOTE-2: an inefficient data structure
        Set<Method> methods = new TreeSet<>(METHOD_COMPARATOR);
        methods.addAll(Arrays.asList(token.getMethods()));
        for (Class<?> clazz = token; clazz != null; clazz = clazz.getSuperclass()) {
            methods.addAll(Arrays.asList(clazz.getDeclaredMethods()));
        }
        for (Method method : methods) {
            methodsImpl.append(getMethodImpl(method));
        }
        return methodsImpl.toString();
    }

    /**
     * Generate end of file.
     *
     * @return resulting {@code String}
     */
    private String getEndOfFile() {
        return CLOSE_BRACE + END_LINE;
    }

    /**
     * Generate class implementation.
     *
     * @param token type token to generating.
     * @param path  path when implementation creating.
     * @throws ImplerException if file can't create;
     */
    private void createFile(Class<?> token, Path path) throws ImplerException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write(toUnicode(getHeader(token) + getConstructors(token) + getMethodsImpl(token) + getEndOfFile()));
        } catch (IOException e) {
            throw new ImplerException("Can't create file");
        }

    }

    /**
     * Translate the {@code String} to Unicode.
     *
     * @param s the string to translating
     * @return translated string.
     */
    private String toUnicode(String s) {
        StringBuilder stringBuilder = new StringBuilder();
        for (char c : s.toCharArray()) {
            stringBuilder.append(c < 128 ? c : String.format("\\u%04x", (int) c));
        }
        return stringBuilder.toString();
    }

    /**
     * Create directories for class implementation.
     *
     * @param path path to class.
     * @throws ImplerException if unable to create directories.
     */
    private void createPatentDirectory(Path path) throws ImplerException {
        try {
            Path parent = path.getParent();
            if (parent != null && Files.notExists(parent)) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new ImplerException("Unable create directories");
        }
    }

    /**
     * Return relative path to file with Impl suffix
     *
     * @param token  type token.
     * @param suffix file suffix.
     * @return resulting {@code Path}
     */
    private Path getImplPath(Class<?> token, String suffix) {
        return Path.of(
                String.format("%s%s%s%s%s",
                        token.getPackageName()
                                .replace(".", PATH_SEPARATOR),
                        PATH_SEPARATOR,
                        token.getSimpleName(),
                        IMPL_SUFFIX,
                        suffix));
    }

    /**
     * Return path to file relative to root.
     *
     * @param token  type token to which to look for a path.
     * @param root   path to file relative.
     * @param suffix file suffix
     * @return resulting {@code Path}
     */
    private Path getImplPath(Class<?> token, Path root, String suffix) {
        return root.resolve(Path.of(getImplPath(token, suffix).toString()));
    }

    /**
     * Generate implementation.
     *
     * @param token type token to create implementation for.
     * @param root  root directory.
     * @throws ImplerException if wrong {@code token} or {@code root} or can't create directories or implementation.
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (token == null || root == null) {
            throw new ImplerException("Illegal argument");
        }
        Path path = getImplPath(token, root, JAVA_SUFFIX);
        createPatentDirectory(path);
        createFile(token, path);
    }

    /**
     * Compile implementation.
     *
     * @param token     type token to compile
     * @param directory directory for the compilation result
     * @throws ImplerException if file can't compile
     */
    private void compileImplementation(Class<?> token, Path directory) throws ImplerException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        String filePath = getImplPath(token, directory, JAVA_SUFFIX).toString();
        String classPath;
        String[] args;
        if (token.getProtectionDomain().getCodeSource() == null) {
            args = new String[]{"--patch-module", token.getModule().getName() + "=" + directory.toString(), filePath};
        } else {
            try {
                classPath = Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
            } catch (URISyntaxException e) {
                throw new ImplerException("Can't compile implementation: Url can't convert to URI", e);
            }
            args = new String[]{"-cp", classPath, filePath};
        }
        final int exitCode = compiler.run(null, null, null, args);
        if (exitCode != 0) {
            throw new ImplerException("can't compile implementation");
        }
    }

    /**
     * Create jar file from compiled class
     *
     * @param token     type token compile
     * @param directory directory where to get the token
     * @param jarFile   resultant JAR file.
     * @throws ImplerException if unable to create jar file
     */
    private void createJar(Class<?> token, Path directory, Path jarFile) throws ImplerException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (JarOutputStream writer = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            writer.putNextEntry(new ZipEntry(getImplPath(token, CLASS_SUFFIX).toString().
                    replace("\\", PATH_SEPARATOR)));
            Files.copy(getImplPath(token, directory, CLASS_SUFFIX), writer);
        } catch (IOException e) {
            throw new ImplerException("Unable to create jar file", e);
        }
    }


    /**
     * Create jar with implementation.
     *
     * @param token   type token to create implementation for.
     * @param jarFile target <var>.jar</var> file.
     * @throws ImplerException if can't create jar
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        if (token == null || jarFile == null) {
            throw new ImplerException("Illegal argument");
        }
        Path tmpDir = Path.of(jarFile.toAbsolutePath().getParent().toString(), "implementor", "temp");
        try {
            implement(token, tmpDir);
            compileImplementation(token, tmpDir);
            createJar(token, tmpDir, jarFile);
        } finally {
            clearDir(tmpDir);
        }
    }

    /**
     * Clear temp directory
     *
     * @param tmpDir temp directory to clear.
     * @throws ImplerException if can't clear temp directory;
     */
    private void clearDir(Path tmpDir) throws ImplerException {
        try {
            Files.walkFileTree(tmpDir, DELETE_VISITOR);
        } catch (IOException e) {
            throw new ImplerException("Can't clear temp directory");
        }
    }
}

