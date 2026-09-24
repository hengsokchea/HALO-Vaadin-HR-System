package org.halocambodia.upload.startup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.halocambodia.upload.config.UploadProperties;
import org.halocambodia.upload.thumbnail.ThumbnailProperties;
import org.halocambodia.upload.thumbnail.VideoThumbnailProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Order(20)
@RequiredArgsConstructor
@Slf4j
public class UploadStartupDiagnostics implements ApplicationRunner {

    private final UploadProperties uploadProperties;
    private final ThumbnailProperties thumbnailProperties;
    private final VideoThumbnailProperties videoProperties;

    @Override
    public void run(ApplicationArguments args) throws IOException {
        Path root = uploadProperties.getRootDirectory().toAbsolutePath().normalize();
        Path temp = root.resolve("temp");
        Path files = root.resolve("files");
        Path thumbnails = thumbnailProperties.directory().toAbsolutePath().normalize();

        ensureWritable(root);
        ensureWritable(temp);
        ensureWritable(files);
        ensureWritable(thumbnails);

        FileStore store = Files.getFileStore(root);
        boolean ffmpegReady = !videoProperties.enabled() || executableAvailable(videoProperties.ffmpegPath());

        log.info("\n============================================================\n" +
                        " HALO UPLOAD FRAMEWORK STARTUP REPORT\n" +
                        "============================================================\n" +
                        " Upload root       : {}\n" +
                        " Temporary files   : {}\n" +
                        " Completed files   : {}\n" +
                        " Thumbnails        : {}\n" +
                        " Thumbnail enabled : {}\n" +
                        " Video enabled     : {}\n" +
                        " FFmpeg ready      : {}\n" +
                        " Usable disk bytes : {}\n" +
                        " Minimum required  : {}\n" +
                        "============================================================",
                root, temp, files, thumbnails,
                thumbnailProperties.enabled(), videoProperties.enabled(), ffmpegReady,
                store.getUsableSpace(), uploadProperties.getMinimumFreeDiskBytes());

        if (store.getUsableSpace() < uploadProperties.getMinimumFreeDiskBytes()) {
            throw new IllegalStateException("Upload disk does not have the configured minimum free space");
        }
        if (!ffmpegReady) {
            log.warn("Video thumbnails are enabled but FFmpeg was not found: {}", videoProperties.ffmpegPath());
        }
    }

    private void ensureWritable(Path directory) throws IOException {
        Files.createDirectories(directory);
        Path test = Files.createTempFile(directory, ".halo-write-test-", ".tmp");
        Files.deleteIfExists(test);
    }

    private boolean executableAvailable(Path configuredPath) {
        if (configuredPath == null) {
            return false;
        }
        if (configuredPath.isAbsolute()) {
            return Files.isRegularFile(configuredPath) && Files.isExecutable(configuredPath);
        }
        String path = System.getenv("PATH");
        if (path == null) {
            return false;
        }
        String command = configuredPath.toString();
        for (String folder : path.split(java.io.File.pathSeparator)) {
            Path candidate = Path.of(folder).resolve(command);
            if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)) {
                return true;
            }
            if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
                candidate = Path.of(folder).resolve(command.endsWith(".exe") ? command : command + ".exe");
                if (Files.isRegularFile(candidate)) {
                    return true;
                }
            }
        }
        return false;
    }
}
