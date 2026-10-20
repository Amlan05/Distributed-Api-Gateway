package com.gateway.user.controller;

import java.util.List;
import java.util.stream.Stream;

import com.gateway.user.dto.UserRequest;
import com.gateway.user.dto.UserResponse;
import com.gateway.user.exception.UserNotFoundException;
import com.gateway.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTests {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private UserService service;
    private final UserResponse user = new UserResponse(1L, "Asha", "asha@example.com");
    private static final String VALID_BODY = "{\"name\":\"Asha\",\"email\":\"asha@example.com\"}";

    @Test
    void listsUsers() throws Exception {
        when(service.findAll()).thenReturn(List.of(user));
        mvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].email").value("asha@example.com"));
    }

    @Test
    void listsNoUsersAsEmptyJsonArray() throws Exception {
        when(service.findAll()).thenReturn(List.of());
        mvc.perform(get("/api/users")).andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void readsUser() throws Exception {
        when(service.findById(1L)).thenReturn(user);
        mvc.perform(get("/api/users/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Asha"));
    }

    @Test
    void createsUserAndReturnsLocation() throws Exception {
        when(service.create(any(UserRequest.class))).thenReturn(user);
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/users/1"))
                .andExpect(jsonPath("$.id").value(1));
        verify(service).create(new UserRequest("Asha", "asha@example.com"));
    }

    @Test
    void updatesUser() throws Exception {
        when(service.update(eq(1L), any(UserRequest.class))).thenReturn(user);
        mvc.perform(put("/api/users/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
        verify(service).update(1L, new UserRequest("Asha", "asha@example.com"));
    }

    @Test
    void deletesUserWithNoResponseBody() throws Exception {
        mvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(service).delete(1L);
    }

    @Test
    void returnsNotFoundOnReadUpdateAndDelete() throws Exception {
        when(service.findById(99L)).thenThrow(new UserNotFoundException(99L));
        when(service.update(eq(99L), any(UserRequest.class))).thenThrow(new UserNotFoundException(99L));
        doThrow(new UserNotFoundException(99L)).when(service).delete(99L);
        for (var request : List.of(get("/api/users/99"),
                put("/api/users/99").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY),
                delete("/api/users/99"))) {
            mvc.perform(request).andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.detail").value("User with id 99 was not found"))
                    .andExpect(jsonPath("$.instance").value("/api/users/99"));
        }
    }

    @ParameterizedTest
    @MethodSource("invalidBodies")
    void rejectsInvalidCreateAndUpdateBeforeCallingService(String body) throws Exception {
        for (var request : List.of(post("/api/users"), put("/api/users/1"))) {
            mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Request validation failed"))
                    .andExpect(jsonPath("$.errors").isMap())
                    .andExpect(jsonPath("$.errors").isNotEmpty());
        }
        verifyNoInteractions(service);
    }

    static Stream<String> invalidBodies() {
        return Stream.of("{}", "{\"name\":null,\"email\":null}",
                "{\"name\":\"  \",\"email\":\"asha@example.com\"}",
                "{\"name\":\"Asha\",\"email\":\"\"}",
                "{\"name\":\"Asha\",\"email\":\"not-an-email\"}",
                "{\"name\":\"" + "A".repeat(101) + "\",\"email\":\"asha@example.com\"}",
                "{\"name\":\"Asha\",\"email\":\"a@" + "b".repeat(250) + ".com\"}");
    }

    @Test
    void validationErrorsIdentifyFieldsWithoutEchoingInput() throws Exception {
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"private-invalid-value\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("Name is required"))
                .andExpect(jsonPath("$.errors.email").value("Email must be valid"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void rejectsMalformedAndMissingJson() throws Exception {
        for (String body : List.of("{broken", "")) {
            mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value(
                            "Request body must be valid JSON with name and email fields"));
        }
        verifyNoInteractions(service);
    }

    @Test
    void rejectsInvalidIdsBeforeCallingService() throws Exception {
        for (String id : List.of("0", "-1", "abc", "999999999999999999999999")) {
            for (var request : List.of(get("/api/users/" + id), delete("/api/users/" + id),
                    put("/api/users/" + id).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))) {
                mvc.perform(request).andExpect(status().isBadRequest());
            }
        }
        verifyNoInteractions(service);
    }

    @Test
    void preservesMethodAndMediaTypeErrorStatuses() throws Exception {
        mvc.perform(patch("/api/users/1")).andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("PUT")));
        mvc.perform(post("/api/users").contentType(MediaType.TEXT_PLAIN).content(VALID_BODY))
                .andExpect(status().isUnsupportedMediaType());
        verifyNoInteractions(service);
    }

    @Test
    void unexpectedErrorsDoNotExposeInternalDetails() throws Exception {
        when(service.findAll()).thenThrow(new IllegalStateException("private implementation detail"));
        mvc.perform(get("/api/users"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.instance").value("/api/users"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }
}
