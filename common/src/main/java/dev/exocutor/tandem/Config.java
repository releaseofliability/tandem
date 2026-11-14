package dev.exocutor.tandem;

import java.nio.file.*;
import java.util.*;
import java.io.*;

public final class Config {
    public static final List<String> offlineNames = new ArrayList<>();
    public static int maxSessions = 4;
    public static boolean animations = true;

    private static Path path() {
        return Tandem.client().gameDirectory.toPath().resolve("config/tandem.properties");
    }

    public static void load() {
        if (!Files.exists(path())) return;
        try (Reader reader = Files.newBufferedReader(path())) {
            Properties p = new Properties();
            p.load(reader);
            maxSessions = Math.clamp(Integer.parseInt(p.getProperty("maxSessions", "4")), 2, 8);
            animations = Boolean.parseBoolean(p.getProperty("animations", "true"));
            for (String name : p.getProperty("offlineNames", "").split(","))
                if (name.matches("[A-Za-z0-9_]{3,16}")) offlineNames.add(name);
        } catch (Exception ex) {
            Tandem.LOG.warn("Could not read Tandem settings: {}", ex.getClass().getSimpleName());
        }
    }

    public static void save() {
        try {
            Files.createDirectories(path().getParent());
            Properties p = new Properties();
            p.setProperty("maxSessions", "" + maxSessions);
            p.setProperty("animations", "" + animations);
            p.setProperty("offlineNames", String.join(",", offlineNames));
            Path temporary = path().resolveSibling("tandem.properties.tmp");
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                p.store(writer, "Tandem");
            }
            Files.move(temporary, path(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            Tandem.LOG.warn("Could not save Tandem settings: {}", ex.getClass().getSimpleName());
        }
    }
}
