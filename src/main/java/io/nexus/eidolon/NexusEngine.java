package io.nexus.eidolon;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class NexusEngine implements Closeable {
    private static final Map<Long, NexusEngine> enginePool = new ConcurrentHashMap<>();
    private static final AtomicLong engineCounter = new AtomicLong(0);
    private final long engineId;
    private final ExecutorService logExecutor = Executors.newSingleThreadExecutor();
    private NexusLogCallback logCallback;

    static {
        try {
            loadNativeLibrary();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load native library", e);
        }
    }

    private static void loadNativeLibrary() throws IOException {
        String osName = System.getProperty("os.name").toLowerCase();
        String osArch = System.getProperty("os.arch").toLowerCase();

        String platform;
        String arch;
        String fileExtension;
        String libName;

        if (osName.contains("win")) {
            platform = "windows";
            fileExtension = ".dll";
            libName = "nexus_bridge";
        } else if (osName.contains("mac")) {
            platform = "macos";
            fileExtension = ".dylib";
            libName = "libnexus_bridge";
        } else if (osName.contains("nix") || osName.contains("nux")) {
            platform = "linux";
            fileExtension = ".so";
            libName = "libnexus_bridge";
        } else {
            throw new UnsupportedOperationException("Unsupported operating system: " + osName);
        }

        if (osArch.equals("amd64") || osArch.equals("x86_64")) {
            arch = "x86_64";
        } else if (osArch.equals("aarch64")) {
            arch = "aarch64";
        } else {
            throw new UnsupportedOperationException("Unsupported architecture: " + osArch);
        }

        String libPath = String.format("/natives/%s/%s/%s%s", platform, arch, libName, fileExtension);

        try (InputStream libStream = NexusEngine.class.getResourceAsStream(libPath)) {
            if (libStream == null) {
                throw new UnsatisfiedLinkError("Native library not found for " + platform + "-" + arch + " at path " + libPath);
            }

            Path tempFile = Files.createTempFile(libName, fileExtension);
            tempFile.toFile().deleteOnExit();
            Files.copy(libStream, tempFile, StandardCopyOption.REPLACE_EXISTING);

            System.load(tempFile.toAbsolutePath().toString());
        }
    }

    private native long nativeInit();
    private native void nativeDestroy(long engineId);
    private native String nativeEvaluate(long engineId, String source, Map<String, Object> globals);
    private native void nativeSetLogCallback(long engineId, NexusLogCallback callback);

    public NexusEngine() {
        this.engineId = nativeInit();
        enginePool.put(this.engineId, this);
    }

    public String evaluate(String source) throws NexusException {
        return evaluate(source, null);
    }

    public String evaluate(String source, Map<String, Object> globals) throws NexusException {
        String interpolatedSource = interpolateVariables(source, globals);
        String result = nativeEvaluate(engineId, interpolatedSource, globals);
        if (result.startsWith("ERROR:")) {
            throw new NexusException(result.substring(6));
        }
        return result;
    }

    public String evaluateFile(String filePath) throws NexusException, IOException {
        return evaluateFile(Paths.get(filePath), null);
    }

    public String evaluateFile(String filePath, Map<String, Object> globals) throws NexusException, IOException {
        return evaluateFile(Paths.get(filePath), globals);
    }

    public String evaluateFile(Path path) throws NexusException, IOException {
        return evaluateFile(path, null);
    }

    public String evaluateFile(Path path, Map<String, Object> globals) throws NexusException, IOException {
        byte[] fileBytes = Files.readAllBytes(path);
        String sourceCode = new String(fileBytes, StandardCharsets.UTF_8);
        return evaluate(sourceCode, globals);
    }

    public String evaluateResource(String resourcePath) throws NexusException, IOException {
        return evaluateResource(resourcePath, null);
    }

    public String evaluateResource(String resourcePath, Map<String, Object> globals) throws NexusException, IOException {
        String sourceCode;
        try (InputStream inputStream = NexusEngine.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("cant find resource: " + resourcePath);
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            sourceCode = buffer.toString(StandardCharsets.UTF_8.name());
        }

        return evaluate(sourceCode, globals);
    }

    public void setLogCallback(NexusLogCallback callback) {
        this.logCallback = callback;
        nativeSetLogCallback(engineId, (level, message) -> {
            logExecutor.submit(() -> logCallback.onLog(level, message));
        });
    }

    @Override
    public void close() {
        nativeDestroy(engineId);
        enginePool.remove(engineId);
        logExecutor.shutdown();
    }

    private String interpolateVariables(String source, Map<String, Object> globals) {
        if (globals == null) {
            return source;
        }
        String interpolated = source;
        for (Map.Entry<String, Object> entry : globals.entrySet()) {
            String key = "$" + entry.getKey();
            String value = entry.getValue().toString();
            interpolated = interpolated.replace(key, value);
        }
        return interpolated;
    }

    public void handleLog(int level, String message) {
        if (logCallback != null) {
            logCallback.onLog(level, message);
        }
    }
}