package com.trendly.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves {@link GlobalExceptionHandler} maps Bean Validation failures to
 * 400 and unmapped exceptions to 500, both in the documented error shape,
 * using {@link ThrowawayTestController} (no business endpoints exist yet --
 * see BE-1.2). Security filters are disabled here since this slice only
 * exercises exception mapping, not auth.
 */
@WebMvcTest(controllers = ThrowawayTestController.class)
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void validationFailureReturns400WithDocumentedShape() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/test/validate"))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void unmappedExceptionReturns500WithNoStackTraceOrClassName() throws Exception {
        mockMvc.perform(post("/test/boom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.path").value("/test/boom"))
                .andExpect(jsonPath("$.error").value("Internal server error"))
                .andExpect(jsonPath("$.error", not(containsString("Exception"))));
    }
}
