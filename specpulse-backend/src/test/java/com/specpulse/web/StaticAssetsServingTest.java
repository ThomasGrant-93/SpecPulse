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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static String indexHtmlFromClasspath() throws IOException {
        InputStream is = StaticAssetsServingTest.class.getClassLoader()
                .getResourceAsStream("static/index.html");
        Assertions.assertNotNull(is, "Expected static/index.html to be present on test classpath");
        try (is) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String extractFirstAssetFile(String indexHtml, String extension) {
        // Example Vite tags:
        // <script type="module" crossorigin src="/assets/index-DvlMvI44.js"></script>
        // <link rel="stylesheet" href="/assets/index-BZAwdAaW.css">
        Pattern p = Pattern.compile("/assets/([^\"']+\\." + Pattern.quote(extension) + ")");
        Matcher m = p.matcher(indexHtml);
        Assertions.assertTrue(m.find(),
                "Could not extract a /assets/*." + extension + " filename from index.html");
        return m.group(1);
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
        String indexHtml = indexHtmlFromClasspath();
        String jsFile = extractFirstAssetFile(indexHtml, "js");

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
        String indexHtml = indexHtmlFromClasspath();
        String cssFile = extractFirstAssetFile(indexHtml, "css");

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
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(Matchers.containsString("<!doctype html")));
    }
}
