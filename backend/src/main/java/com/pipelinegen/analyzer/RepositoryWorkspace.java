package com.pipelinegen.analyzer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/** Блокировка вне каталога клона позволяет чистить остатки даже на Windows. */
final class RepositoryWorkspace implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(RepositoryWorkspace.class);
    private static final String PREFIX = "repository-";

    private final Path directory;
    private final Path lockPath;
    private final FileChannel channel;

    private RepositoryWorkspace(Path directory, Path lockPath, FileChannel channel) {
        this.directory = directory;
        this.lockPath = lockPath;
        this.channel = channel;
    }

    static RepositoryWorkspace create(Path root) throws IOException {
        Files.createDirectories(root);
        Path directory = root.resolve(PREFIX + UUID.randomUUID());
        Path lockPath = lockPath(directory);
        FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        try {
            // Захватываем блокировку до появления каталога, который может увидеть уборщик.
            channel.lock();
            Files.createDirectory(directory);
            return new RepositoryWorkspace(directory, lockPath, channel);
        } catch (IOException | RuntimeException e) {
            try {
                channel.close();
                Files.deleteIfExists(lockPath);
            } catch (IOException cleanupFailure) {
                e.addSuppressed(cleanupFailure);
            }
            throw e;
        }
    }

    Path directory() {
        return directory;
    }

    @Override
    public void close() throws IOException {
        try (channel) {
            ClonedRepository.deleteDirectory(directory);
        }
        Files.deleteIfExists(lockPath);
    }

    static void cleanAbandoned(Path root) {
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        try (var entries = Files.newDirectoryStream(root, PREFIX + "*")) {
            for (Path directory : entries) {
                if (Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
                    cleanOne(directory);
                }
            }
        } catch (IOException | java.nio.file.DirectoryIteratorException e) {
            log.warn("Не удалось прочитать временный каталог репозиториев {}", root, e);
        }
    }

    private static void cleanOne(Path directory) {
        Path lockPath = lockPath(directory);
        if (!Files.isRegularFile(lockPath, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        try {
            try (var channel = FileChannel.open(lockPath, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
                 FileLock lock = channel.tryLock()) {
                if (lock == null) {
                    return;
                }
                ClonedRepository.deleteDirectory(directory);
            }
            Files.deleteIfExists(lockPath);
        } catch (OverlappingFileLockException | java.nio.file.NoSuchFileException ignored) {
            // Каталог используется в этом процессе или уже удалён другим экземпляром.
        } catch (IOException e) {
            log.warn("Не удалось удалить временную копию {}", directory, e);
        }
    }

    private static Path lockPath(Path directory) {
        return directory.resolveSibling(directory.getFileName() + ".lock");
    }
}
