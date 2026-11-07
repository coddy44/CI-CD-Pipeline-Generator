package com.pipelinegen.analyzer;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;

/** Временная рабочая копия. Закрытие удаляет её файлы, не переходя по символьным ссылкам. */
public final class ClonedRepository implements AutoCloseable {

    private final Path directory;
    private final String branch;

    ClonedRepository(Path directory, String branch) {
        this.directory = directory;
        this.branch = branch;
    }

    public Path directory() {
        return directory;
    }

    /** Фактически полученная ветка; не обязательно ветка репозитория по умолчанию. */
    public String branch() {
        return branch;
    }

    @Override
    public void close() throws IOException {
        deleteDirectory(directory);
    }

    static void deleteDirectory(Path directory) throws IOException {
        if (Files.notExists(directory)) {
            return;
        }
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException failure) throws IOException {
                if (failure != null) {
                    throw failure;
                }
                delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void delete(Path path) throws IOException {
        try {
            Files.delete(path);
        } catch (AccessDeniedException e) {
            // Git pack-файлы на Windows могут иметь атрибут read-only.
            DosFileAttributeView attributes = Files.getFileAttributeView(
                    path, DosFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
            if (attributes == null || !attributes.readAttributes().isReadOnly()) {
                throw e;
            }
            attributes.setReadOnly(false);
            Files.delete(path);
        }
    }
}
