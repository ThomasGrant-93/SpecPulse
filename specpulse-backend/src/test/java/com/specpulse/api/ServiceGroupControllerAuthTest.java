package com.specpulse.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.specpulse.group.CreateGroupRequest;
import com.specpulse.group.ServiceGroupController;
import com.specpulse.group.ServiceGroupDTO;
import com.specpulse.group.ServiceGroupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ServiceGroupController.class)
@TestPropertySource(properties = "specpulse.auth.token=test-group-token")
class ServiceGroupControllerAuthTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ServiceGroupService groupService;

    @Test
    @DisplayName("Should return 401 for group creation without Authorization")
    void shouldReturn401WhenCreateWithoutAuth() throws Exception {
        CreateGroupRequest req = new CreateGroupRequest();
        req.setName("Production");
        req.setDescription("desc");

        String requestJson = objectMapper.writeValueAsString(req);

        mockMvc.perform(post("/api/v1/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 201 for group creation with correct Authorization")
    void shouldReturn201WhenCreateAuthorized() throws Exception {
        CreateGroupRequest req = new CreateGroupRequest();
        req.setName("Production");
        req.setDescription("desc");
        req.setSortOrder(1);

        ServiceGroupDTO created = ServiceGroupDTO.builder()
                .name("Production")
                .description("desc")
                .sortOrder(1)
                .build();

        given(groupService.createGroup(any())).willReturn(created);

        String requestJson = objectMapper.writeValueAsString(req);

        mockMvc.perform(post("/api/v1/groups")
                        .header("Authorization", "Bearer test-group-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Production"))
                .andExpect(jsonPath("$.description").value("desc"));
    }
}
