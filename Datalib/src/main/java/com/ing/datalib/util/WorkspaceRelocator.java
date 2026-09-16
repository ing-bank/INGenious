package com.ing.datalib.util;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Copies an existing Workspace to a new location without deleting the source.
 */
public final class WorkspaceRelocator {

    private WorkspaceRelocator() {}

    /**
     * Copies the complete source Workspace into a destination that does not yet exist.
     *
     * <p>The copy is first created in a temporary sibling directory. The completed
     * temporary copy is then renamed to the requested destination. The source
     * Workspace is never modified or deleted.
     *
     * @param source current Workspace root
     * @param destination new Workspace root
     * @throws IOException if validation, copying, or destination creation fails
     */
    public static void copyWorkspace(Path source, Path destination) throws IOException {
        Path normalizedSource = source.toAbsolutePath().normalize();
        Path normalizedDestination = destination.toAbsolutePath().normalize();

        validateRelocationPaths(normalizedSource, normalizedDestination);

        if (!isValidWorkspace(normalizedSource)) {
            throw new IOException(
                "The current Workspace is incomplete or unavailable: " + normalizedSource
            );
        }

        if (Files.exists(normalizedDestination)) {
            throw new FileAlreadyExistsException(
                "The destination Workspace already exists: " + normalizedDestination
            );
        }

        Path parent = normalizedDestination.getParent();

        if (parent == null) {
            throw new IOException(
                "The destination Workspace has no parent directory: " + normalizedDestination
            );
        }

        Files.createDirectories(parent);

        Path temporary = parent.resolve(
            "." + normalizedDestination.getFileName() + ".copy-" + UUID.randomUUID()
        );

        try {
            copyRecursively(normalizedSource, temporary);
            moveCompletedCopy(temporary, normalizedDestination);
        } catch (IOException | RuntimeException ex) {
            deleteRecursively(temporary);
            throw ex;
        }
    }

    /**
     * Validates that the destination does not overlap the active Workspace.
     *
     * <p>The destination cannot be the source, a child of the source, or an
     * ancestor containing the source. Existing path components are resolved
     * through symbolic links before comparison.
     *
     * @param source current Workspace root
     * @param destination requested destination Workspace root
     * @throws IOException if the paths overlap or cannot be resolved safely
     */
    public static void validateRelocationPaths(Path source, Path destination) throws IOException {
        Path resolvedSource = resolvePathThroughExistingParent(source.toAbsolutePath().normalize());

        Path resolvedDestination = resolvePathThroughExistingParent(
            destination.toAbsolutePath().normalize()
        );

        if (resolvedSource.equals(resolvedDestination)) {
            throw new IOException("The selected location is already the active Workspace.");
        }

        if (resolvedDestination.startsWith(resolvedSource)) {
            throw new IOException(
                "The destination cannot be inside the active Workspace: " + resolvedDestination
            );
        }

        if (resolvedSource.startsWith(resolvedDestination)) {
            throw new IOException(
                "The destination cannot contain the active Workspace: " + resolvedDestination
            );
        }
    }

    /**
     * Resolves all existing path components through symbolic links while
     * retaining any trailing components that do not exist yet.
     */
    private static Path resolvePathThroughExistingParent(Path path) throws IOException {
        Path candidate = path.toAbsolutePath().normalize();
        Path existing = candidate;
        java.util.ArrayDeque<Path> missingParts = new java.util.ArrayDeque<>();

        while (existing != null && Files.notExists(existing)) {
            Path name = existing.getFileName();

            if (name != null) {
                missingParts.addFirst(name);
            }

            existing = existing.getParent();
        }

        if (existing == null) {
            throw new IOException("Could not resolve an existing parent for: " + candidate);
        }

        Path resolved = existing.toRealPath();

        for (Path missingPart : missingParts) {
            resolved = resolved.resolve(missingPart);
        }

        return resolved.normalize();
    }

    /**
     * Verifies that all source directories and regular files exist in the
     * destination with identical file contents.
     *
     * <p>Additional destination entries are allowed, but every source directory
     * and regular file must exist in the destination with identical contents.
     *
     * @param source current Workspace root
     * @param destination copied Workspace root
     * @throws IOException if an entry is missing or a file differs
     */
    public static void verifyWorkspaceCopy(Path source, Path destination) throws IOException {
        Path normalizedSource = source.toAbsolutePath().normalize();
        Path normalizedDestination = destination.toAbsolutePath().normalize();

        if (!isValidWorkspace(normalizedSource)) {
            throw new IOException(
                "The source Workspace is incomplete or unavailable: " + normalizedSource
            );
        }

        if (!isValidWorkspace(normalizedDestination)) {
            throw new IOException(
                "The destination Workspace is incomplete or unavailable: " + normalizedDestination
            );
        }

        for (Path relative : listRelativeEntries(normalizedSource)) {
            Path sourceEntry = normalizedSource.resolve(relative);
            Path destinationEntry = normalizedDestination.resolve(relative);

            if (Files.isDirectory(sourceEntry)) {
                if (!Files.isDirectory(destinationEntry)) {
                    throw new IOException("Destination directory is missing: " + relative);
                }
            } else if (Files.isRegularFile(sourceEntry)) {
                verifyRegularFile(sourceEntry, destinationEntry, relative);
            } else {
                throw new IOException("Unsupported Workspace entry: " + relative);
            }
        }
    }

    private static ArrayList<Path> listRelativeEntries(Path workspace) throws IOException {
        ArrayList<Path> entries = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(workspace)) {
            paths
                .filter(path -> !path.equals(workspace))
                .map(workspace::relativize)
                .sorted(Comparator.comparingInt(Path::getNameCount).thenComparing(Path::toString))
                .forEach(entries::add);
        }

        return entries;
    }

    private static void verifyRegularFile(Path source, Path destination, Path relative)
        throws IOException {
        if (!Files.isRegularFile(destination)) {
            throw new IOException("Destination file is missing: " + relative);
        }

        if (Files.size(source) != Files.size(destination)) {
            throw new IOException("Destination file size differs: " + relative);
        }

        if (!MessageDigest.isEqual(sha256(source), sha256(destination))) {
            throw new IOException("Destination file contents differ: " + relative);
        }
    }

    private static byte[] sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            try (var input = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int count;

                while ((count = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, count);
                }
            }

            return digest.digest();
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException("SHA-256 verification is unavailable.", ex);
        }
    }

    /**
     * Deletes the previous Workspace only after verifying the destination copy.
     *
     * @param source previous Workspace root
     * @param destination new Workspace root
     * @throws IOException if verification fails or the source cannot be removed
     */
    public static void deleteVerifiedSource(Path source, Path destination) throws IOException {
        Path normalizedSource = source.toAbsolutePath().normalize();
        Path normalizedDestination = destination.toAbsolutePath().normalize();

        if (normalizedSource.equals(normalizedDestination)) {
            throw new IOException("The source and destination Workspace paths are identical.");
        }

        verifyWorkspaceCopy(normalizedSource, normalizedDestination);

        deleteRecursively(normalizedSource);

        if (Files.exists(normalizedSource)) {
            throw new IOException(
                "The previous Workspace could not be removed completely: " + normalizedSource
            );
        }
    }

    /**
     * Removes a newly created destination after a failed relocation workflow.
     *
     * <p>This method must only be used for a destination created during the
     * current relocation attempt. The active source Workspace is never passed
     * to this method.
     *
     * @param destination newly created destination Workspace
     * @throws IOException if the destination cannot be removed completely
     */
    public static void removeFailedDestination(Path destination) throws IOException {
        deleteRecursively(destination.toAbsolutePath().normalize());
    }

    static boolean isValidWorkspace(Path workspace) {
        return (
            Files.isDirectory(workspace) &&
            Files.isDirectory(workspace.resolve("Configuration")) &&
            Files.isDirectory(workspace.resolve("Projects")) &&
            Files.isDirectory(workspace.resolve("Shared"))
        );
    }

    private static void copyRecursively(Path source, Path destination) throws IOException {
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path entry : paths
                .sorted(Comparator.comparingInt(Path::getNameCount).thenComparing(Path::toString))
                .toList()) {
                Path relative = source.relativize(entry);
                Path target = destination.resolve(relative);

                if (Files.isDirectory(entry)) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(entry, target, StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
        }
    }

    private static void moveCompletedCopy(Path temporary, Path destination) throws IOException {
        CopyOption[] atomicOptions = { StandardCopyOption.ATOMIC_MOVE };

        try {
            Files.move(temporary, destination, atomicOptions);
        } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
            Files.move(temporary, destination);
        }
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(path)) {
            IOException failure = null;

            for (Path entry : paths.sorted(Comparator.reverseOrder()).toList()) {
                try {
                    Files.deleteIfExists(entry);
                } catch (DirectoryNotEmptyException ex) {
                    if (failure == null) {
                        failure = ex;
                    }
                } catch (IOException ex) {
                    if (failure == null) {
                        failure = ex;
                    }
                }
            }

            if (failure != null) {
                throw failure;
            }
        }
    }
}
