import io.nexus.eidolon.NexusEngine;
import io.nexus.eidolon.NexusException;

import java.io.IOException;

public class ExecuteFileTest {
    public static void main(String[] args) {
        String resourcePath = "/examples/test_features.eidolon";

        try (NexusEngine engine = new NexusEngine()) {
            engine.setLogCallback((level, message) -> {
                System.out.println("[LOG-" + level + "]: " + message);
            });

            String res = engine.evaluateResource(resourcePath);
            System.out.println("> result: " + res);

        } catch (NexusException | IOException e) {
            e.printStackTrace();
        }
    }
}
