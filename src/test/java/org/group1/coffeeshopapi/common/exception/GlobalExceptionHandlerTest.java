package org.group1.coffeeshopapi.common.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.group1.coffeeshopapi.common.enums.OrderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.sql.SQLException;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The error contract both frontends rely on: one shape, with a per-field list when fields are invalid.
class GlobalExceptionHandlerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void invalidFieldsAreListedOneByOneWithTheirPaths() throws Exception {
        mvc.perform(post("/probe/validate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"orderType\":\"DINE_IN\",\"items\":[{\"quantity\":0}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.path").value("/probe/validate"))
                .andExpect(jsonPath("$.timeStamp").exists())
                .andExpect(jsonPath("$.errors", hasSize(2)))
                .andExpect(jsonPath("$.errors[?(@.field == 'name')].message").value("Name is required"))
                .andExpect(jsonPath("$.errors[?(@.field == 'items[0].quantity')].message")
                        .value("Quantity must be at least 1"))
                .andExpect(jsonPath("$.message", containsString("Name is required")));
    }

    @Test
    void aDefaultConstraintMessageIsPrefixedWithTheFieldName() throws Exception {
        mvc.perform(post("/probe/validate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dara\",\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("orderType"))
                .andExpect(jsonPath("$.errors[0].message").value("Order type must not be null"));
    }

    @Test
    void anUnknownEnumValueNamesTheFieldAndTheAllowedValues() throws Exception {
        mvc.perform(post("/probe/validate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dara\",\"orderType\":\"DELIVERY\",\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("orderType"))
                .andExpect(jsonPath("$.message").value(
                        "Invalid value 'DELIVERY' for orderType — use one of: DINE_IN, TAKE_AWAY"));
    }

    @Test
    void anErrorAboutNoParticularFieldHasNoErrorsList() throws Exception {
        mvc.perform(post("/probe/fail").param("state", "23505"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void databaseErrorsAreReportedByWhatWentWrong() throws Exception {
        mvc.perform(post("/probe/fail").param("state", "22001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A value is too long — shorten it and try again"));
        mvc.perform(post("/probe/fail").param("state", "23503"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("can't be deleted")));
        mvc.perform(post("/probe/fail").param("state", "23502"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A required value is missing"));
    }

    @Test
    void anOversizeUploadSaysSoWithA413() throws Exception {
        mvc.perform(post("/probe/upload"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.message").value("The file is too large. The maximum size is 5 MB"));
    }

    record Item(@NotNull @Min(value = 1, message = "Quantity must be at least 1") Integer quantity) {
    }

    record Probe(@NotBlank(message = "Name is required") @Size(max = 10) String name,
                 @NotNull OrderType orderType,
                 @Valid List<Item> items) {
    }

    @RestController
    static class ProbeController {
        @PostMapping("/probe/validate")
        void validate(@Valid @RequestBody Probe probe) {
        }

        @PostMapping("/probe/fail")
        void fail(@RequestParam String state) {
            throw new DataIntegrityViolationException("fail", new SQLException("fail", state));
        }

        @PostMapping("/probe/upload")
        void upload() {
            throw new MaxUploadSizeExceededException(5L * 1024 * 1024);
        }
    }
}
