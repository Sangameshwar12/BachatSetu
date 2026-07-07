package in.bachatsetu.backend.auth.interfaces.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.bachatsetu.backend.auth.application.usecase.GenerateOtpUseCase;
import in.bachatsetu.backend.auth.application.usecase.InvalidateOtpUseCase;
import in.bachatsetu.backend.auth.application.usecase.ResendOtpUseCase;
import in.bachatsetu.backend.auth.application.usecase.VerifyOtpUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "bachatsetu.persistence.auditing.enabled=false",
    "bachatsetu.persistence.repositories.enabled=false",
    "bachatsetu.group.rest.enabled=false",
    "bachatsetu.member.rest.enabled=false",
    "bachatsetu.payment.rest.enabled=false",
    "bachatsetu.draw.rest.enabled=false",
    "bachatsetu.receipt.rest.enabled=false",
    "spring.autoconfigure.exclude="
            + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
            + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
            + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,"
            + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
            + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
            + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration"
})
@AutoConfigureMockMvc
class AuthenticationOpenApiSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GenerateOtpUseCase generateOtp;

    @MockBean
    private VerifyOtpUseCase verifyOtp;

    @MockBean
    private ResendOtpUseCase resendOtp;

    @MockBean
    private InvalidateOtpUseCase invalidateOtp;

    @Test
    void publishesAuthenticationContract() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("BachatSetu API"))
                .andExpect(jsonPath("$.info.version").value("v1"))
                .andExpect(jsonPath("$.paths['/api/v1/auth/otp/request'].post.summary")
                        .value("Request an OTP"))
                .andExpect(jsonPath("$.paths['/api/v1/auth/otp/verify'].post.responses['422']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/otp/resend'].post.responses['429']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/otp/invalidate'].post").exists());
    }
}
