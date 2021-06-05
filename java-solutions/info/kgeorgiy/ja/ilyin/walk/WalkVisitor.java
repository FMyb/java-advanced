package info.kgeorgiy.ja.ilyin.walk;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * @author Yaroslav Ilin
 */

public class WalkVisitor extends SimpleFileVisitor<Path> {
    private final BufferedWriter output;
    private final boolean recursive;

    public WalkVisitor(BufferedWriter output, boolean recursive) {
        this.output = output;
        this.recursive = recursive;
    }

    public void write(long hash, String filePath) throws IOException {
        output.write(String.format("%016x %s%n", hash, filePath));
    }

    public static long pjwHash(Path filePath) {
        long high;
        long hash = 0;
        byte[] bytes = new byte[4096];
        try (BufferedInputStream reader = new BufferedInputStream(Files.newInputStream(filePath))) {
            int c;
            while ((c = reader.read(bytes)) >= 0) {
                for (int i = 0; i < c; i++) {
                    hash = (hash << 8) + (bytes[i] & 0xff);
                    if ((high = hash & 0xff00000000000000L) != 0) {
                        hash ^= high >> 48;
                        hash &= ~high;
                    }
                }
            }
        } catch (IOException e) {
            hash = 0;
        }
        return hash;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        long hash = pjwHash(file);
        write(hash, file.toString());
        return recursive ? FileVisitResult.CONTINUE : FileVisitResult.TERMINATE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        write(0, file.toString());
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        if (recursive) {
            return FileVisitResult.CONTINUE;
        } else {
            write(0, dir.toString());
            return FileVisitResult.SKIP_SUBTREE;
        }
    }
}
