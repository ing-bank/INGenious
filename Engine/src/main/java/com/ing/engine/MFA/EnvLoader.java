package com.ing.engine.MFA;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * Utility class for loading environment variables from a file.
 * <p>
 * This class reads a file containing key-value pairs (e.g., `.env` format)
 * and returns them as a {@link Map}. Lines starting with '#' are treated as comments
 * and ignored.
 * </p>
 */
public class EnvLoader {

    /**
     * Loads environment variables from the specified file.
     *
     * @param filePath the path to the environment file
     * @return a {@link Map} containing the environment variables as key-value pairs
     * @throws IOException if an I/O error occurs while reading the file
     *
     * <p><b>Example:</b></p>
     * <pre>
     * Map&lt;String, String&gt; env = EnvLoader.loadEnv("path/to/.env");
     * String value = env.get("MY_KEY");
     * </pre>
     */
    public static Map<String, String> loadEnv(String filePath) throws IOException {
        Map<String, String> env = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().startsWith("#") && line.contains("=")) {
                    String[] parts = line.split("=", 2);
                    env.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
        return env;
    }

}
