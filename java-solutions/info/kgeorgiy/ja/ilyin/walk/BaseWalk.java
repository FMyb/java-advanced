package info.kgeorgiy.ja.ilyin.walk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * @author Yaroslav Ilin
 */

public class BaseWalk {
    private static void visit(String path, WalkVisitor visitor) throws WalkException {
        try {
            try {
                Files.walkFileTree(Path.of(path), visitor);
            } catch (InvalidPathException | SecurityException e) {
                visitor.write(0, path);
            }
        } catch (IOException e) {
            throw new WalkException("filed to write output file", e);
        }
    }

    private static void process(Path inputPath, Path outputPath, boolean recursive) throws WalkException {
        try (BufferedReader inputFiles = Files.newBufferedReader(inputPath)) {
            try (BufferedWriter output = Files.newBufferedWriter(outputPath)) {
                try {
                    String filePath;
                    WalkVisitor visitor = new WalkVisitor(output, recursive);
                    while ((filePath = inputFiles.readLine()) != null) {
                        visit(filePath, visitor);
                    }
                } catch (IOException e) {
                    throw new WalkException("failed reading input file", e);
                }
            } catch (IOException e) {
                throw new WalkException("invalid to open output File (" + outputPath + ")", e);
            } catch (SecurityException e) {
                throw new WalkException("you don't have permission to open output file (" + outputPath + ")", e);
            }
        } catch (IOException e) {
            throw new WalkException("invalid reading input File (" + inputPath + ")", e);
        } catch (SecurityException e) {
            throw new WalkException("you don't have permission to open input file (" + inputPath + ")", e);
        }
    }

    private static Path toPath(String s) throws WalkException {
        try {
            return Path.of(s);
        } catch (InvalidPathException e) {
            throw new WalkException("Invalid path: " + s, e);
        }
    }

    public static void run(String[] args, boolean recursive) {
        try {
            if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
                throw new WalkException("expected 2 not null arguments for input and output file");
            }
            Path inputPath = toPath(args[0]);
            Path outputPath = toPath(args[1]);
            try {
                Path parent = outputPath.getParent();
                if (parent != null && Files.notExists(parent)) {
                    Files.createDirectories(parent);
                }
            } catch (IOException e) {
                throw new WalkException("can't create not-exists output directory", e);
            } catch (SecurityException e) {
                throw new WalkException("don't have permissions to create not-exists output directory", e);
            }
            process(inputPath, outputPath, recursive);
        } catch (WalkException e) {
            System.err.println(e.getMessage());
        }
    }
}
