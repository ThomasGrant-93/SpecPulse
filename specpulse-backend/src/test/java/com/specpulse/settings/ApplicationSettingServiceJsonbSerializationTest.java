package com.specpulse.settings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.specpulse.settings.application.ApplicationSettingService;
import com.specpulse.settings.domain.ApplicationSetting;
import com.specpulse.settings.domain.ApplicationSettingRepositoryPort;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ApplicationSettingServiceJsonbSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ApplicationSettingRepositoryPort settingRepository;

    private ApplicationSettingService service;

    @Captor
    private ArgumentCaptor<ApplicationSetting> settingCaptor;

    @BeforeEach
    void setUp() {
        service = new ApplicationSettingService(settingRepository, objectMapper);
    }

    private ApplicationSetting editableSetting(String valueType) {
        ApplicationSetting s = new ApplicationSetting();
        s.setCategory("general");
        s.setKey("app.description");
        s.setIsPublic(true);
        s.setIsEditable(true);
        s.setValueType(valueType);
        s.setDescription("desc");
        s.setValue(null);
        return s;
    }

    @Test
    void shouldStoreAndReturnJsonStringForStringSettings() {
        ApplicationSetting setting = editableSetting("string");
        given(settingRepository.findByCategoryAndKey(eq("general"), eq("app.description")))
                .willReturn(Optional.of(setting));
        given(settingRepository.save(any(ApplicationSetting.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        ApplicationSettingDTO dto = service.updateSetting("general", "app.description", "foo");
        assertEquals("foo", dto.getValue());

        verify(settingRepository).save(settingCaptor.capture());
        Object savedValue = settingCaptor.getValue().getValue();
        assertEquals("\"foo\"", savedValue);
    }

    @Test
    void shouldStoreAndReturnJsonBooleanForBooleanSettings() {
        ApplicationSetting setting = editableSetting("boolean");
        given(settingRepository.findByCategoryAndKey(eq("general"), eq("app.description")))
                .willReturn(Optional.of(setting));
        given(settingRepository.save(any(ApplicationSetting.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        ApplicationSettingDTO dto = service.updateSetting("general", "app.description", true);
        assertEquals(Boolean.TRUE, dto.getValue());

        verify(settingRepository).save(settingCaptor.capture());
        Object savedValue = settingCaptor.getValue().getValue();
        assertEquals("true", savedValue);
    }

    @Test
    void shouldStoreAndReturnJsonNumberForIntegerSettings() {
        ApplicationSetting setting = editableSetting("integer");
        given(settingRepository.findByCategoryAndKey(eq("general"), eq("app.description")))
                .willReturn(Optional.of(setting));
        given(settingRepository.save(any(ApplicationSetting.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        ApplicationSettingDTO dto = service.updateSetting("general", "app.description", 123);
        assertEquals(123, dto.getValue());

        verify(settingRepository).save(settingCaptor.capture());
        Object savedValue = settingCaptor.getValue().getValue();
        assertEquals("123", savedValue);
    }

    @Test
    void shouldStoreAndReturnJsonObjectsForJsonSettings() throws Exception {
        ApplicationSetting setting = editableSetting("json");
        given(settingRepository.findByCategoryAndKey(eq("general"), eq("app.description")))
                .willReturn(Optional.of(setting));
        given(settingRepository.save(any(ApplicationSetting.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> payload = new HashMap<>();
        payload.put("a", 1);
        payload.put("b", List.of("x", "y"));

        ApplicationSettingDTO dto = service.updateSetting("general", "app.description", payload);
        assertInstanceOf(Map.class, dto.getValue());

        @SuppressWarnings("unchecked")
        Map<String, Object> dtoMap = (Map<String, Object>) dto.getValue();
        assertEquals(1, dtoMap.get("a"));
        assertEquals(List.of("x", "y"), dtoMap.get("b"));

        verify(settingRepository).save(settingCaptor.capture());
        Object savedValue = settingCaptor.getValue().getValue();
        assertInstanceOf(String.class, savedValue);

        JsonNode storedTree = objectMapper.readTree((String) savedValue);
        JsonNode expectedTree = objectMapper.valueToTree(payload);
        assertEquals(expectedTree, storedTree);
    }

    @Test
    void shouldNormalizeStoredJsonStringLiteralWhenReading() {
        ApplicationSetting setting = editableSetting("string");
        // Simulate a stored JSON string literal (e.g. from jsonb read/write paths)
        setting.setValue("\"foo\"");

        given(settingRepository.findByCategoryAndKey(eq("general"), eq("app.description")))
                .willReturn(Optional.of(setting));

        ApplicationSettingDTO dto = service.getSetting("general", "app.description");
        assertEquals("foo", dto.getValue());
    }

    @Test
    void shouldRejectInvalidBooleanValues() {
        ApplicationSetting setting = editableSetting("boolean");
        given(settingRepository.findByCategoryAndKey(eq("general"), eq("app.description")))
                .willReturn(Optional.of(setting));

        try {
            service.updateSetting("general", "app.description", "not-bool");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().toLowerCase().contains("boolean"));
        }
    }

    @Test
    void shouldRejectInvalidIntegerValues() {
        ApplicationSetting setting = editableSetting("integer");
        given(settingRepository.findByCategoryAndKey(eq("general"), eq("app.description")))
                .willReturn(Optional.of(setting));

        try {
            service.updateSetting("general", "app.description", "not-int");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().toLowerCase().contains("integer"));
        }
    }

    @Test
    void shouldRejectNonStringValueForStringSetting() {
        ApplicationSetting setting = editableSetting("string");
        given(settingRepository.findByCategoryAndKey(eq("general"), eq("app.description")))
                .willReturn(Optional.of(setting));

        try {
            service.updateSetting("general", "app.description", true);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().toLowerCase().contains("valueType=string")
                    || e.getMessage().toLowerCase().contains("expected a string"));
        }
    }
}
