package com.gateway.user.integration;

import com.gateway.support.PostgresTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestConfiguration.class)
class UserApiIntegrationTests {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void completeLifecycleUsesTheRealServiceAndRepository() throws Exception {
        var created = mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Asha\",\"email\":\"asha@example.com\"}"))
                .andExpect(status().isCreated()).andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        String location = created.getResponse().getHeader("Location");

        mvc.perform(get(location)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id)).andExpect(jsonPath("$.name").value("Asha"));
        mvc.perform(get("/api/users")).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")].name").value("Asha"));
        mvc.perform(put(location).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Asha Rao\",\"email\":\"rao@example.com\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Asha Rao"));
        mvc.perform(get(location)).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("rao@example.com"));
        mvc.perform(delete(location)).andExpect(status().isNoContent()).andExpect(content().string(""));
        mvc.perform(get(location)).andExpect(status().isNotFound());
        mvc.perform(put(location).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Again\",\"email\":\"again@example.com\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete(location)).andExpect(status().isNotFound());
    }
}
