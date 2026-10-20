import java.io.File;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Tiny launcher so the project can be started with: java Main */
public class Main {
    public static void main(String[] args) throws Exception {
        File lib = new File("lib");
        if (!lib.exists()) lib.mkdirs();
        File driver = new File(lib, "sqlite-jdbc-3.53.4.0.jar");
        if (!driver.isFile() || driver.length() < 1000000) {
            File[] jars = lib.listFiles((dir, name) -> name.startsWith("sqlite-jdbc") && name.endsWith(".jar") && new File(dir, name).length() > 1000000);
            if (jars != null && jars.length > 0) driver = jars[0];
            else {
                System.out.println("SQLite JDBC driver not found; trying to download it into lib/...");
                File downloaded = new File(lib, "sqlite-jdbc-3.53.4.0.jar");
                Process download = new ProcessBuilder("wget", "-q", "-O", downloaded.getPath(),
                    "https://repo.maven.apache.org/maven2/org/xerial/sqlite-jdbc/3.53.4.0/sqlite-jdbc-3.53.4.0.jar").inheritIO().start();
                if (download.waitFor() == 0 && downloaded.isFile() && downloaded.length() > 1000000) driver = downloaded;
                else {
                    downloaded.delete();
                    System.err.println("Could not download SQLite JDBC automatically.");
                    System.err.println("Copy your downloaded sqlite-jdbc-*.jar into: " + lib.getAbsolutePath());
                    System.exit(1);
                }
            }
        }

        // Recompile all source files on every launch so replacing a .java file is enough.
        File[] sourceFiles = new File(".").listFiles((dir, name) -> name.endsWith(".java"));
        if (sourceFiles == null || sourceFiles.length == 0) {
            System.err.println("No Java source files found. Run java Main from the Acadex project folder.");
            System.exit(1);
        }
        Arrays.sort(sourceFiles, Comparator.comparing(File::getName));
        List<String> compile = new ArrayList<>();
        compile.add("javac");
        for (File source : sourceFiles) compile.add(source.getName());
        Process compiler = new ProcessBuilder(compile).inheritIO().start();
        if (compiler.waitFor() != 0) {
            System.err.println("Compilation failed. Fix the error above and run java Main again.");
            System.exit(1);
        }

        // Recent sqlite-jdbc versions use SLF4J API. Fetch it too when it is absent.
        File slf4j = new File(lib, "slf4j-api-2.0.17.jar");
        if (!slf4j.isFile() || slf4j.length() < 10000) {
            Process download = new ProcessBuilder("wget", "-q", "-O", slf4j.getPath(),
                "https://repo.maven.apache.org/maven2/org/slf4j/slf4j-api/2.0.17/slf4j-api-2.0.17.jar").inheritIO().start();
            if (download.waitFor() != 0 || !slf4j.isFile() || slf4j.length() < 10000) {
                slf4j.delete();
                System.err.println("Could not download the SLF4J API dependency. Check internet access and run java Main again.");
                System.exit(1);
            }
        }

        String java = new File(System.getProperty("java.home"), "bin/java").getAbsolutePath();
        String cp = "." + File.pathSeparator + driver.getPath() + File.pathSeparator + slf4j.getPath();
        Process process = new ProcessBuilder(Arrays.asList(java, "-cp", cp, "AcadexApp")).inheritIO().start();
        System.exit(process.waitFor());
    }
}
