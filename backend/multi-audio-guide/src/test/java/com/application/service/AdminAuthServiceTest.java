package com.application.service;
import com.application.dto.AdminAuthDto.*;
import com.application.repository.AdminAuthRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class AdminAuthServiceTest {
    @Test void badPasswordCannotCreateSessionAndValidLoginStoresOnlyTokenHash() {
        var repository=mock(AdminAuthRepository.class);var service=new AdminAuthService(repository);
        when(repository.account("editor")).thenReturn(Optional.of(new Account("editor",new BCryptPasswordEncoder().encode("password"),"EDITOR")));
        assertThatThrownBy(()->service.login(new Login("editor","wrong"))).hasMessageContaining("401");
        verify(repository,never()).session(anyString(),anyString(),any());
        var session=service.login(new Login("editor","password"));
        assertThat(session.token()).hasSize(43);assertThat(session.role()).isEqualTo("EDITOR");
        verify(repository).session(argThat(hash->hash.matches("[0-9a-f]{64}")&&!hash.equals(session.token())),eq("editor"),eq(session.expiresAt()));
    }
}
