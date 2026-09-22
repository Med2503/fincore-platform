package org.fincore.identity.user.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.fincore.identity.user.application.exception.DuplicateUsernameException;
import org.fincore.identity.user.application.usecase.CreateUserUseCase;
import org.fincore.identity.user.domain.model.User;
import org.fincore.identity.user.domain.model.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({
        org.fincore.identity.shared.web.GlobalExceptionHandler.class,
        org.fincore.identity.shared.web.CorrelationIdFilter.class
})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateUserUseCase createUserUseCase;

    @Test
    void shouldCreateUser() throws Exception {

        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.now();

        User user = new User(
                userId,
                "john",
                "$2a$12$hash",
                UserStatus.PENDING,
                createdAt
        );

        when(createUserUseCase.create(any()))
                .thenReturn(user);

        CreateUserRequest request =
                new CreateUserRequest(
                        "john",
                        "Password123!"
                );

        mockMvc.perform(
                        post("/api/v1/users")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id")
                        .value(userId.toString()))
                .andExpect(jsonPath("$.username")
                        .value("john"))
                .andExpect(jsonPath("$.status")
                        .value("PENDING"))
                .andExpect(jsonPath("$.createdAt")
                        .exists())
                .andExpect(jsonPath("$.password")
                        .doesNotExist())
                .andExpect(jsonPath("$.passwordHash")
                        .doesNotExist());

        verify(createUserUseCase)
                .create(any());
    }

    @Test
    void shouldReturn400WhenUsernameIsBlank()
            throws Exception {

        CreateUserRequest request =
                new CreateUserRequest(
                        "",
                        "Password123!"
                );

        mockMvc.perform(
                        post("/api/v1/users")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"));

        verify(createUserUseCase, never())
                .create(any());
    }

    @Test
    void shouldReturn400WhenPasswordIsTooShort()
            throws Exception {

        CreateUserRequest request =
                new CreateUserRequest(
                        "john",
                        "123"
                );

        mockMvc.perform(
                        post("/api/v1/users")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"));

        verify(createUserUseCase, never())
                .create(any());
    }

    @Test
    void shouldReturn409WhenUsernameAlreadyExists()
            throws Exception {

        when(createUserUseCase.create(any()))
                .thenThrow(
                        new DuplicateUsernameException("john")
                );

        CreateUserRequest request =
                new CreateUserRequest(
                        "john",
                        "Password123!"
                );

        mockMvc.perform(
                        post("/api/v1/users")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("DUPLICATE_USERNAME"))
                .andExpect(jsonPath("$.status")
                        .value(409));
    }

    @Test
    void shouldPropagateCorrelationId()
            throws Exception {

        UUID userId = UUID.randomUUID();

        User user = new User(
                userId,
                "john",
                "$2a$12$hash",
                UserStatus.PENDING,
                Instant.now()
        );

        when(createUserUseCase.create(any()))
                .thenReturn(user);

        String correlationId =
                "corr-test-123";

        CreateUserRequest request =
                new CreateUserRequest(
                        "john",
                        "Password123!"
                );

        mockMvc.perform(
                        post("/api/v1/users")
                                .header(
                                        "X-Correlation-Id",
                                        correlationId
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(
                        header().string(
                                "X-Correlation-Id",
                                correlationId
                        )
                );
    }

    @Test
    void shouldGenerateCorrelationIdWhenMissing()
            throws Exception {

        UUID userId = UUID.randomUUID();

        User user = new User(
                userId,
                "john",
                "$2a$12$hash",
                UserStatus.PENDING,
                Instant.now()
        );

        when(createUserUseCase.create(any()))
                .thenReturn(user);

        CreateUserRequest request =
                new CreateUserRequest(
                        "john",
                        "Password123!"
                );

        mockMvc.perform(
                        post("/api/v1/users")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(
                        header().exists(
                                "X-Correlation-Id"
                        )
                );
    }
}