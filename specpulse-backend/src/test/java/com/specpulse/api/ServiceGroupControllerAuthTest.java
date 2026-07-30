package com.specpulse.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.specpulse.auth.repository.UserRepository;
import com.specpulse.auth.security.JwtService;
import com.specpulse.group.CreateGroupRequest;
import com.specpulse.group.ServiceGroupController;
import com.specpulse.group.ServiceGroupDTO;
import com.specpulse.group.ServiceGroupService;
import com.specpulse.test.JwtTestUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ServiceGroupController.class)
@Import(com.specpulse.config.SecurityConfig.class)
class ServiceGroupControllerAuthTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private ServiceGroupService groupService;

    @MockBean
    private UserRepository userRepository;

    private String token(long userId) {
        JwtTestUtil.stubEnabledAdmin(userRepository, userId);
        return JwtTestUtil.adminAccessToken(jwtService, userId);
    }

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
        long userId = 1L;
        String token = token(userId);

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
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Production"))
                .andExpect(jsonPath("$.description").value("desc"));
    }
}
