# `Nexus` — Native Eidolon eXecution Unified System

Nexus is a high-performance Java JNI bridge for the [Eidolon programming language](https://github.com/Weever1337/eidolon_lang), enabling seamless execution of Eidolon code inside the JVM ecosystem with native Rust speed.

# It provides
- Full support for global variables shared between Java and Eidolon
- Running Eidolon script files by path or resources
- Direct evaluation of Eidolon code strings (eval)
- Log callback support from Eidolon to Java
- Thread-safety and resource management via AutoCloseable pattern
- Linux and Windows support (macOS is not supported yet)

# Getting started
## Requirements
- Java 8 or higher
- Rust 1.70 with Cargo (for building native library)
## Installation
### Gradle:
```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.Weever1337:nExus-bridge:${Tag}'
}
```

### Gradle Kotlin DSL:
```
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.Weever1337:nExus-bridge:${Tag}")
}
```

### Maven:
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.Weever1337</groupId>
    <artifactId>nExus-bridge</artifactId>
    <version>Tag</version>
</dependency>
```

# Using example

```java
import io.nexus.eidolon.NexusEngine;
import io.nexus.eidolon.NexusException;

public class Example {
    public static void main(String[] args) {
        try (NexusEngine nexus = new NexusEngine()) {
            nexus.setLogCallback((level, message) -> System.out.println("[LOG-" + level + "]: " + message));
            
            Map<String, Object> globals = new HashMap<>();
            globals.put("player_x", 100);
            globals.put("player_y", 50);

            String code = """
                fun distance[x1, y1, x2, y2] = ((x2 - x1)^2 + (y2 - y1)^2)^0.5
                distance[$player_x, $player_y, 200, 150]
                """;

            Object result = nexus.evaluate(code, globals);
            System.out.println("Distance: " + result);
        } catch (NexusException e) {
            e.printStackTrace();
        }
    }
}
```