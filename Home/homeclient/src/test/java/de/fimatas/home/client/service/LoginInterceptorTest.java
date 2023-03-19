package de.fimatas.home.client.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import mfi.files.api.DeviceType;
import mfi.files.api.TokenResult;
import mfi.files.api.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest
@AutoConfigureMockMvc
class LoginInterceptorTest {

    static final String THE_USER_NAME = "theUserName";
    static final String THE_DEVICE = "theDevice";
    static final String THE_PASSWORD = "thePassword";
    static final String THE_USER_AGENT = "theUserAgent";
    static final String THE_TOKEN = "theToken";
    static final String THE_NEW_TOKEN = "theNewToken";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @BeforeEach
    public void beforeEach(){

        given(userService.userNameFromLoginCookie(anyString())).willReturn(null);
        given(userService.userNameFromLoginCookie(THE_TOKEN)).willReturn(THE_USER_NAME);
        given(userService.userNameFromLoginCookie(THE_NEW_TOKEN)).willReturn(THE_USER_NAME);

        given(userService.deleteToken(THE_USER_NAME, THE_USER_AGENT, DeviceType.BROWSER)).willReturn(true);

        given(userService.createToken(anyString(), anyString(), anyString(), any(DeviceType.class)))
                .willReturn(new TokenResult(false, false,null));
        given(userService.createToken(THE_USER_NAME, THE_PASSWORD, THE_USER_AGENT, DeviceType.BROWSER))
                .willReturn(new TokenResult(true, false, THE_TOKEN));

        given(userService.checkToken(isNull(), anyString(), anyString(), any(DeviceType.class), anyBoolean()))
                .willReturn(new TokenResult(false, false,null));
        given(userService.checkToken(anyString(), anyString(), anyString(), any(DeviceType.class), anyBoolean()))
                .willReturn(new TokenResult(false, false,null));
        given(userService.checkToken(THE_USER_NAME, THE_TOKEN, THE_USER_AGENT, DeviceType.BROWSER, false))
                .willReturn(new TokenResult(true, false,null));
        given(userService.checkToken(THE_USER_NAME, THE_TOKEN, THE_USER_AGENT, DeviceType.BROWSER, true))
                .willReturn(new TokenResult(true, false,THE_NEW_TOKEN));
        given(userService.checkToken(THE_USER_NAME, THE_TOKEN, THE_DEVICE, DeviceType.APP, false))
                .willReturn(new TokenResult(true, false,null));

        given(userService.readExternalKey(anyString(), anyString(), anyString(), any(DeviceType.class), anyString()))
                .willReturn(new TokenResult(false, false,null));
    }

    @Test
    void testIsAssetRequestTrue() {
        assertTrue(new LoginInterceptor().isAssetRequest(uri("/a.js")));
        assertTrue(new LoginInterceptor().isAssetRequest(uri("/a.css")));
        assertTrue(new LoginInterceptor().isAssetRequest(uri("/robots.txt")));
    }

    @Test
    void testIsAssetRequestFalse() {
        assertFalse(new LoginInterceptor().isAssetRequest(uri("/")));
        assertFalse(new LoginInterceptor().isAssetRequest(uri("/settings")));
        assertFalse(new LoginInterceptor().isAssetRequest(uri("/js")));
    }

    @Test
    void testLoginSiteSuccessful() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/login"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    void testRootAuthSuccessful() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/")
                        .header(LoginInterceptor.USER_AGENT, THE_USER_AGENT)
                        .param(LoginInterceptor.LOGIN_USERNAME, THE_USER_NAME)
                        .param(LoginInterceptor.LOGIN_PASSWORD, THE_PASSWORD)
                        .param(LoginInterceptor.LOGIN_COOKIEOK, "true"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    void testRootAuthFailedHasCookiesNotAccepted() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/")
                        .header(LoginInterceptor.USER_AGENT, THE_USER_AGENT)
                        .param(LoginInterceptor.LOGIN_USERNAME, THE_USER_NAME)
                        .param(LoginInterceptor.LOGIN_PASSWORD, THE_PASSWORD)
                        .param(LoginInterceptor.LOGIN_COOKIEOK, "null"))
                .andExpect(MockMvcResultMatchers.status().isFound())
                .andExpect(MockMvcResultMatchers.redirectedUrl("/loginCookieCheck"));
    }

    @Test
    void testRootAuthFailedWrongLoginData() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/")
                        .header(LoginInterceptor.USER_AGENT, THE_USER_AGENT)
                        .param(LoginInterceptor.LOGIN_USERNAME, "x")
                        .param(LoginInterceptor.LOGIN_PASSWORD, "y")
                        .param(LoginInterceptor.LOGIN_COOKIEOK, "true"))
                .andExpect(MockMvcResultMatchers.status().isFound())
                .andExpect(MockMvcResultMatchers.redirectedUrl("/loginFailed"));
    }

    @Test
    void testRootAuthFailedNoLoginData() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/"))
                .andExpect(MockMvcResultMatchers.status().isFound())
                .andExpect(MockMvcResultMatchers.redirectedUrl("/login"));
    }

    @Test
    void testRootAuthWithCookie() throws Exception {
        // Login with credentials, creates cookie
        mockMvc.perform(MockMvcRequestBuilders.post("/")
                        .header(LoginInterceptor.USER_AGENT, THE_USER_AGENT)
                        .param(LoginInterceptor.LOGIN_USERNAME, THE_USER_NAME)
                        .param(LoginInterceptor.LOGIN_PASSWORD, THE_PASSWORD)
                        .param(LoginInterceptor.LOGIN_COOKIEOK, "true"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(cookie().exists(LoginInterceptor.COOKIE_NAME));
        // Second Request, provides cookie for auth
        Cookie loginCookie = new Cookie(LoginInterceptor.COOKIE_NAME, THE_TOKEN);
        loginCookie.setMaxAge(60 * 60 * 24 * 92);
        mockMvc.perform(MockMvcRequestBuilders.get("/")
                        .cookie(loginCookie)
                        .header(LoginInterceptor.USER_AGENT, THE_USER_AGENT))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    void testRootAuthWithWrongCookie() throws Exception {
        // Login with credentials, creates cookie
        mockMvc.perform(MockMvcRequestBuilders.post("/")
                        .header(LoginInterceptor.USER_AGENT, THE_USER_AGENT)
                        .param(LoginInterceptor.LOGIN_USERNAME, THE_USER_NAME)
                        .param(LoginInterceptor.LOGIN_PASSWORD, THE_PASSWORD)
                        .param(LoginInterceptor.LOGIN_COOKIEOK, "true"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(cookie().exists(LoginInterceptor.COOKIE_NAME));
        // Second Request, provides cookie for auth
        Cookie loginCookie = new Cookie(LoginInterceptor.COOKIE_NAME, "xyz");
        loginCookie.setMaxAge(60 * 60 * 24 * 92);
        mockMvc.perform(MockMvcRequestBuilders.get("/")
                        .cookie(loginCookie)
                        .header(LoginInterceptor.USER_AGENT, THE_USER_AGENT))
                .andExpect(MockMvcResultMatchers.status().isFound())
                .andExpect(MockMvcResultMatchers.redirectedUrl("/loginFailed"));
    }

    @Test
    void testRootAuthWithToken() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/")
                            .header(LoginInterceptor.APP_DEVICE, THE_DEVICE)
                            .header(LoginInterceptor.APP_USER_NAME, THE_USER_NAME)
                            .header(LoginInterceptor.APP_USER_TOKEN, THE_TOKEN))
                            .andExpect(MockMvcResultMatchers.status().isOk());
        verify(userService, times(1))
                .checkToken(anyString(), anyString(), anyString(), any(DeviceType.class), anyBoolean());
    }

    @Test
    void testRootAuthWithWrongToken() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/")
                            .header(LoginInterceptor.APP_DEVICE, THE_DEVICE)
                            .header(LoginInterceptor.APP_USER_NAME, THE_USER_NAME)
                            .header(LoginInterceptor.APP_USER_TOKEN, "xyz"))
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized());
        verify(userService, times(1))
                .checkToken(anyString(), anyString(), anyString(), any(DeviceType.class), anyBoolean());
    }

    private HttpServletRequest uri(String uri) {
        return new MockHttpServletRequest("get", uri);
    }
}
