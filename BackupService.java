import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class BackupService {
    private BackupService() {}
    public static Path backup() throws IOException {
        Path source=Paths.get("acadex.db").toAbsolutePath();if(!Files.exists(source))throw new IOException("Database file does not exist yet.");Path dir=Paths.get("backups");Files.createDirectories(dir);String stamp=LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));Path target=dir.resolve("acadex-"+stamp+".db");return Files.copy(source,target,StandardCopyOption.COPY_ATTRIBUTES);
    }
}
