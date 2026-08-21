package fr.pace.common;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ApiAccessFilterTest {
    @Test
    @DisplayName("Devrait refuser une route API sans clé d'accès")
    void doFilter_shouldRejectApiRequest_whenAccessKeyIsMissing() throws Exception {
        // GIVEN
        ApiAccessFilter filter = new ApiAccessFilter(true, "cle-attendue");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/garmin/connexion");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        // WHEN
        filter.doFilter(request, response, chain);

        // THEN
        assertThat(response.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Devrait autoriser une route API avec la bonne clé d'accès")
    void doFilter_shouldAllowApiRequest_whenAccessKeyIsValid() throws Exception {
        // GIVEN
        ApiAccessFilter filter = new ApiAccessFilter(true, "cle-attendue");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/profil");
        request.addHeader("X-Cle-Pace", "cle-attendue");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        // WHEN
        filter.doFilter(request, response, chain);

        // THEN
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("Devrait refuser une écriture sans marqueur de l'interface")
    void doFilter_shouldRejectUnsafeRequest_whenInterfaceHeaderIsMissing() throws Exception {
        // GIVEN
        ApiAccessFilter filter = new ApiAccessFilter(true, "cle-attendue");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/garmin/connexion");
        request.addHeader("X-Cle-Pace", "cle-attendue");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        // WHEN
        filter.doFilter(request, response, chain);

        // THEN
        assertThat(response.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(request, response);
    }
}
