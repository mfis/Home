package de.fimatas.home.client.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import javax.servlet.http.HttpServletRequest;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class LoginInterceptorTest {

    @Test
    public void testIsAssetRequestTrue() {
        assertTrue(LoginInterceptor.isAssetRequest(uri("/a.js")));
        assertTrue(LoginInterceptor.isAssetRequest(uri("/a.css")));
        assertTrue(LoginInterceptor.isAssetRequest(uri("/robots.txt")));
    }

    @Test
    public void testIsAssetRequestFalse() {
        assertFalse(LoginInterceptor.isAssetRequest(uri("/")));
        assertFalse(LoginInterceptor.isAssetRequest(uri("/settings")));
        assertFalse(LoginInterceptor.isAssetRequest(uri("/js")));
    }

    private HttpServletRequest uri(String uri) {
        return new MockHttpServletRequest("get", uri);
    }
}
