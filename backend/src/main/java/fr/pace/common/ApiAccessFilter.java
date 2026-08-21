package fr.pace.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/** Protège l'API privée avec la clé injectée par le frontal authentifié. */
@Component
public class ApiAccessFilter extends OncePerRequestFilter {
    private static final String ACCESS_KEY_HEADER = "X-Cle-Pace";
    private static final String INTERFACE_REQUEST_HEADER = "X-Requete-Pace";

    private final boolean enabled;
    private final byte[] expectedKey;

    public ApiAccessFilter(
            @Value("${pace.access.enabled:true}") boolean enabled,
            @Value("${pace.access.token:}") String token
    ) {
        this.enabled = enabled;
        this.expectedKey = token.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String providedKey = request.getHeader(ACCESS_KEY_HEADER);
        if (providedKey == null || expectedKey.length == 0 || !MessageDigest.isEqual(
                expectedKey,
                providedKey.getBytes(StandardCharsets.UTF_8)
        ) || isUnsafeRequestWithoutInterfaceHeader(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write("{\"title\":\"Accès refusé\",\"status\":401}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static boolean isUnsafeRequestWithoutInterfaceHeader(HttpServletRequest request) {
        return !List.of("GET", "HEAD", "OPTIONS").contains(request.getMethod())
                && !"interface-web".equals(request.getHeader(INTERFACE_REQUEST_HEADER));
    }
}
