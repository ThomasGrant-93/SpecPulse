package com.specpulse.web;

import com.specpulse.api.DiffController;
import com.specpulse.config.WebConfig;
import com.specpulse.diff.DiffService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {DiffController.class, FrontendSpaController.class})
@Import(WebConfig.class)
class StaticAssetsServingTest {

    @Autowired
    private MockMvc mockMvc;

    // WebMvc slice requires at least one controller; we import WebConfig explicitly.
    @MockBean
    @SuppressWarnings("unused")
    private DiffService diffService;

    private static Path frontendAssetsDir() {
        Path p = Path.of("specpulse-frontend", "dist", "assets");
        if (Files.isDirectory(p)) return p;

        // When tests are executed from the backend module directory.
        Path p2 = Path.of("..", "specpulse-frontend", "dist", "assets");
        if (Files.isDirectory(p2)) return p2;

        Path p3 = Path.of("..", "..", "specpulse-frontend", "dist", "assets");
        if (Files.isDirectory(p3)) return p3;

        throw new AssertionError("Cannot locate specpulse-frontend dist assets directory. Tried: " + p + ", " + p2 + ", " + p3);
    }

    private static String pickFirstAssetFile(Path dir, String glob) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, glob)) {
            for (Path p : stream) {
                return p.getFileName().toString();
            }
        }
        throw new AssertionError("No assets found in " + dir + " for pattern " + glob);
    }

    @Test
    void shouldServeIndexHtmlAtRoot() throws Exception {
        Assertions.assertNotNull(
                getClass().getClassLoader().getResource("static/index.html"),
                "Expected static/index.html to be present on test classpath"
        );

        mockMvc.perform(MockMvcRequestBuilders.get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(Matchers.containsString("<!doctype html")));
    }

    @Test
    void shouldServeHashedJsWithCorrectMimeType() throws Exception {
        Path assetsDir = frontendAssetsDir();
        String jsFile = pickFirstAssetFile(assetsDir, "*.js");

        String classpathResource = "static/assets/" + jsFile;
        Assertions.assertNotNull(
                getClass().getClassLoader().getResource(classpathResource),
                "Expected " + classpathResource + " to be present on test classpath"
        );

        mockMvc.perform(MockMvcRequestBuilders.get("/assets/" + jsFile))
                .andExpect(status().isOk())
                // Spring should return application/javascript (or text/javascript depending on runtime).
                .andExpect(header().string("Content-Type", Matchers.containsString("javascript")))
                .andExpect(header().string("Content-Type", Matchers.not(Matchers.containsString("application/json"))));
    }

    @Test
    void shouldServeHashedCssWithCorrectMimeType() throws Exception {
        Path assetsDir = frontendAssetsDir();
        String cssFile = pickFirstAssetFile(assetsDir, "*.css");

        String classpathResource = "static/assets/" + cssFile;
        Assertions.assertNotNull(
                getClass().getClassLoader().getResource(classpathResource),
                "Expected " + classpathResource + " to be present on test classpath"
        );

        mockMvc.perform(MockMvcRequestBuilders.get("/assets/" + cssFile))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", Matchers.containsString("text/css")));
    }

    @Test
    void shouldReturn404ForMissingAssets() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/assets/does-not-exist.js"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldSupportSpaRefreshForKnownRoute() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/services/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }
}
