package org.squiddev.cctweaks;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

/**
 * Guards the two load-bearing build artefacts: the access transformer is in the shipped resources,
 * and the ComputerCraft version in dependencies.gradle matches the @Mod dependency string.
 */
public class BuildArtifactTest {

    @Test
    public void accessTransformerIsShipped() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/META-INF/CCTweaks_at.cfg")) {
            assertNotNull(stream, "CCTweaks_at.cfg must be on the runtime classpath");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = stream.read(buffer)) != -1) out.write(buffer, 0, read);
            String contents = new String(out.toByteArray(), StandardCharsets.UTF_8);
            assertTrue(contents.contains("durabilityRemainingOnBlock"));
        }
    }

    @Test
    public void dependencyVersionsMatchTheModMetadata() throws Exception {
        File dependencies = new File("dependencies.gradle");
        assertTrue(dependencies.isFile(), "tests must run from the project root");
        String contents = new String(Files.readAllBytes(dependencies.toPath()), StandardCharsets.UTF_8);

        assertTrue(
            contents.contains("Computercraft-Legacy:3.3.0"),
            "update CCTweaks.DEPENDENCIES when bumping Computercraft-Legacy");
    }
}
