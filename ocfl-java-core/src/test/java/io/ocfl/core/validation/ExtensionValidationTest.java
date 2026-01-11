package io.ocfl.core.validation;

import io.ocfl.api.OcflConstants;
import io.ocfl.api.model.ValidationCode;
import io.ocfl.core.extension.OcflExtension;
import io.ocfl.core.extension.OcflExtensionRegistry;
import io.ocfl.core.extension.ValidationContext;
import io.ocfl.core.storage.filesystem.FileSystemStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExtensionValidationTest {

    @TempDir
    public Path tempDir;

    private static final String EXTENSION_NAME = "test-extension";

    @BeforeEach
    public void setup() {
        OcflExtensionRegistry.register(EXTENSION_NAME, TestExtension.class);
    }

    @AfterEach
    public void teardown() {
        OcflExtensionRegistry.remove(EXTENSION_NAME);
    }

    @Test
    public void shouldCallExtensionOnValidate() throws IOException {
        var objectRoot = tempDir.resolve("object");
        Files.createDirectories(objectRoot);
        Files.writeString(objectRoot.resolve("0=ocfl_object_1.1"), "ocfl_object_1.1\n");
        Files.writeString(objectRoot.resolve("inventory.json"), "{\"id\": \"obj\", \"type\": \"https://ocfl.io/1.1/spec/#inventory\", \"digestAlgorithm\": \"sha512\", \"head\": \"v1\", \"manifest\": {}, \"versions\": {\"v1\": {\"created\": \"2020-01-01T00:00:00Z\", \"state\": {}}}}");
        Files.writeString(objectRoot.resolve("inventory.json.sha512"), "210452331604a11c83ec19b5c338f0d55e090df4b149b0621370557403487f97576a176840d8641490233e726b21696395561a70081079d8544a476045d9472d  inventory.json");

        var extensionsDir = objectRoot.resolve(OcflConstants.EXTENSIONS_DIR);
        var testExtDir = extensionsDir.resolve(EXTENSION_NAME);
        Files.createDirectories(testExtDir);

        TestExtension.called.set(false);

        var validator = new Validator(new FileSystemStorage(tempDir));
        var results = validator.validateObject("object", false);
        
        System.out.println("[DEBUG_LOG] Validation results: " + results);

        assertTrue(TestExtension.called.get(), "Extension's onValidate should have been called");
    }

    @Test
    public void extensionShouldBeAbleToReadAndWrite() throws IOException {
        var objectRoot = tempDir.resolve("object");
        Files.createDirectories(objectRoot);
        Files.writeString(objectRoot.resolve("0=ocfl_object_1.1"), "ocfl_object_1.1\n");
        Files.writeString(objectRoot.resolve("inventory.json"), "{\"id\": \"obj\", \"type\": \"https://ocfl.io/1.1/spec/#inventory\", \"digestAlgorithm\": \"sha512\", \"head\": \"v1\", \"manifest\": {}, \"versions\": {\"v1\": {\"created\": \"2020-01-01T00:00:00Z\", \"state\": {}}}}");
        Files.writeString(objectRoot.resolve("inventory.json.sha512"), "210452331604a11c83ec19b5c338f0d55e090df4b149b0621370557403487f97576a176840d8641490233e726b21696395561a70081079d8544a476045d9472d  inventory.json");

        var extensionsDir = objectRoot.resolve(OcflConstants.EXTENSIONS_DIR);
        var testExtDir = extensionsDir.resolve(EXTENSION_NAME);
        Files.createDirectories(testExtDir);

        var dataFile = objectRoot.resolve("data.txt");
        Files.writeString(dataFile, "some data");

        var validator = new Validator(new FileSystemStorage(tempDir));
        var results = validator.validateObject("object", false);

        var extFile = testExtDir.resolve("output.txt");
        assertTrue(Files.exists(extFile), "Extension should have written output.txt");
        assertEquals("some data", Files.readString(extFile));
        
        assertTrue(results.getWarnings().stream().anyMatch(w -> w.getCode() == ValidationCode.EXTENSION_WARNING), "Should have added a warning from extension");
    }

    public static class TestExtension implements OcflExtension {
        public static final AtomicBoolean called = new AtomicBoolean(false);

        @Override
        public String getExtensionName() {
            return EXTENSION_NAME;
        }

        @Override
        public void onValidate(ValidationContext context) {
            called.set(true);
            var storage = context.getStorage();
            var dataPath = context.getObjectRootPath() + "/data.txt";
            if (storage.fileExists(dataPath)) {
                var data = storage.readToString(dataPath);
                storage.write(context.getExtensionPath() + "/output.txt", data.getBytes(), null);
            }
            context.addIssue(ValidationCode.EXTENSION_WARNING, "Warning from extension");
        }
    }
}
