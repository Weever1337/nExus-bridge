import io.nexus.eidolon.*;

import java.util.HashMap;
import java.util.Map;

public class TestEval {
    public static void main(String[] args) {
        try (NexusEngine engine = new NexusEngine()) {
            engine.setLogCallback((level, message) -> {
                System.out.println("[LOG-" + level + "]: " + message);
            });

            System.out.println("[#1]");
            String simpleCode = "2 + 2 * 2";
            String result1 = engine.evaluate(simpleCode);
            System.out.println("> Result: " + result1);

            System.out.println("\n[#2] functions");
            String funcCode = "sqrt[9] + sin[PI]";
            String result2 = engine.evaluate(funcCode);
            System.out.println("> Result: " + result2);

            System.out.println("\n[#3] variables");
            Map<String, Object> globals = new HashMap<>();
            globals.put("myVar", 10);
            String varCode = "let x = $myVar * 2\nx + 5";
            String result3 = engine.evaluate(varCode, globals);
            System.out.println("> Result: " + result3);
        } catch (NexusException e) {
            e.printStackTrace();
        }
    }
}
