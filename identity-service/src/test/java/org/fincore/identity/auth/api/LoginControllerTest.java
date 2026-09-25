package org.fincore.identity.auth.api;

import org.fincore.identity.auth.application.exception.InvalidCredentialsException;
import org.fincore.identity.auth.application.result.LoginResult;
import org.fincore.identity.auth.application.usecase.LoginUseCase;
import org.fincore.identity.shared.web.CorrelationIdFilter;
import org.fincore.identity.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;

import static org.mockito.Mockito.when;

@WebMvcTest(LoginController.class)
@Import({
        GlobalExceptionHandler.class,
        CorrelationIdFilter.class
})
public class LoginControllerTest {

    @MockBean
    private LoginUseCase loginUseCase;


    @Test
    void shouldLoginSuccessfully() throws Exception {

        when(loginUseCase.login(any()))
                .thenReturn(
                        new LoginResult("jwt-token")
                );

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "username": "john",
                                          "password": "password123"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken")
                        .value("jwt-token"))
                .andExpect(jsonPath("$.tokenType")
                        .value("Bearer"));
    }

    @Test
    void shouldRejectBlankUsername() throws Exception {

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "username": "",
                                          "password": "password123"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn401ForInvalidCredentials() throws Exception {

        when(loginUseCase.login(any()))
                .thenThrow(
                        new InvalidCredentialsException()
                );

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "username": "john",
                                          "password": "wrong"
                                        }
                                        """)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_CREDENTIALS"));
    }

}
