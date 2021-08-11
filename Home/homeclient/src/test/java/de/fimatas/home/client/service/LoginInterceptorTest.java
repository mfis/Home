package de.fimatas.home.client.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import javax.servlet.http.HttpServletRequest;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class LoginInterceptorTest {

    @Test
    public void testIsAssetRequestTrue() {
        assertTrue(new LoginInterceptor().isAssetRequest(uri("/a.js")));
        assertTrue(new LoginInterceptor().isAssetRequest(uri("/a.css")));
        assertTrue(new LoginInterceptor().isAssetRequest(uri("/robots.txt")));
    }

    @Test
    public void testIsAssetRequestFalse() {
        assertFalse(new LoginInterceptor().isAssetRequest(uri("/")));
        assertFalse(new LoginInterceptor().isAssetRequest(uri("/settings")));
        assertFalse(new LoginInterceptor().isAssetRequest(uri("/js")));
    }

    private HttpServletRequest uri(String uri) {
        return new MockHttpServletRequest("get", uri);
    }
}
