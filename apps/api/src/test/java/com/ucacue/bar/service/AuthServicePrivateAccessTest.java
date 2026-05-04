package com.ucacue.bar.service;

import com.ucacue.bar.dto.auth.RegisterTenantRequest;
import com.ucacue.bar.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServicePrivateAccessTest {

    @Test
    void registerTenantIsDisabledByDefault() {
        AuthService service = new AuthService(null, null, null, null, null, null);
        RegisterTenantRequest request = new RegisterTenantRequest();

        assertThatThrownBy(() -> service.registerTenant(request, "127.0.0.1", "test"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("registro publico");
    }
}
