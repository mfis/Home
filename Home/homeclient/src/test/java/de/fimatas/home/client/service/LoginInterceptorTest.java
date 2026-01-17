package de.fimatas.home.client.service;

import de.fimatas.home.library.util.HomeAppConstants;
import de.fimatas.users.api.TokenResult;
import de.fimatas.users.api.UserAPI;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;

@SpringBootTest
@AutoConfigureMockMvc
class LoginInterceptorTest {

    static final String THE_USER_NAME = "theUserName";
    static final String THE_DEVICE = "theDevice";
    static final String THE_PASSWORD = "thePassword";
    static final String THE_PIN = "123456";
    static final String THE_USER_AGENT = "theUserAgent";
    static final String THE_TOKEN = "theToken";
    static final String THE_NEW_TOKEN = "theNewToken";
    static final String THE_APPLICATION = "de_fimatas_homeclient";
    static final String THE_CIENT_COMM_TOKEN = "clientCommToken";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserAPI userAPI;

    @SpyBean
    private Environment env;

    @BeforeEach
    public void beforeEach(){

        given(userAPI.userNameFromLoginCookie(anyString())).willReturn(null);
        given(userAPI.userNameFromLoginCookie(THE_TOKEN)).willReturn(THE_USER_NAME);
        given(userAPI.userNameFromLoginCookie(THE_NEW_TOKEN)).willReturn(THE_USER_NAME);

        given(userAPI.createToken(anyString(), anyString(), anyString(), anyString()))
                .willReturn(new TokenResult(false, false,null));
        given(userAPI.createToken(THE_USER_NAME, THE_PASSWORD, THE_APPLICATION, THE_USER_AGENT))
                .willReturn(new TokenResult(true, false, THE_TOKEN));

        given(userAPI.checkToken(isNull(), anyString(), anyString(), anyString(), anyBoolean()))
                .willReturn(new TokenResult(false, false,null));
        given(userAPI.checkToken(anyString(), anyString(), anyString(), anyString(), anyBoolean()))
                .willReturn(new TokenResult(false, false,null));
        given(userAPI.checkToken(THE_USER_NAME, THE_TOKEN, THE_APPLICATION, THE_USER_AGENT, false))
                .willReturn(new TokenResult(true, false,null));
        given(userAPI.checkToken(THE_USER_NAME, THE_TOKEN, THE_APPLICATION, THE_USER_AGENT, true))
                .willReturn(new TokenResult(true, false,THE_NEW_TOKEN));
        given(userAPI.checkToken(THE_USER_NAME, THE_TOKEN, THE_APPLICATION, THE_DEVICE, false))
                .willReturn(new TokenResult(true, false,null));
        given(userAPI.checkPin(null, null)).willReturn(true);
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
        verify(userAPI, times(1))
                .checkToken(anyString(), anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    void testRootAuthWithWrongToken() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/")
                            .header(LoginInterceptor.APP_DEVICE, THE_DEVICE)
                            .header(LoginInterceptor.APP_USER_NAME, THE_USER_NAME)
                            .header(LoginInterceptor.APP_USER_TOKEN, "xyz"))
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized());
        verify(userAPI, times(1))
                .checkToken(anyString(), anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    void testUsersSiteOK() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/users")
                        .header(LoginInterceptor.USER_AGENT, THE_USER_AGENT))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    void testUsersCheckPinWithoutToken() throws Exception {
        System.setProperty(HomeAppConstants.CONTROLLER_CLIENT_COMM_TOKEN, THE_CIENT_COMM_TOKEN);
        mockMvc.perform(MockMvcRequestBuilders.post("/users/checkPIN")
                        .header(LoginInterceptor.USER_AGENT, THE_USER_AGENT))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void testUsersCheckPinWrongToken() throws Exception {
        System.setProperty(HomeAppConstants.CONTROLLER_CLIENT_COMM_TOKEN, THE_CIENT_COMM_TOKEN);
        mockMvc.perform(MockMvcRequestBuilders.post("/users/checkPIN")
                        .header(LoginInterceptor.USER_AGENT, THE_USER_AGENT)
                        .header(HomeAppConstants.CONTROLLER_CLIENT_COMM_TOKEN, "xyz"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void testUsersCheckPinCorrectToken() throws Exception {
        System.setProperty(HomeAppConstants.CONTROLLER_CLIENT_COMM_TOKEN, THE_CIENT_COMM_TOKEN);
        mockMvc.perform(MockMvcRequestBuilders.post("/users/checkPIN")
                        .header(LoginInterceptor.USER_AGENT, THE_USER_AGENT)
                        .header(HomeAppConstants.CONTROLLER_CLIENT_COMM_TOKEN, THE_CIENT_COMM_TOKEN))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    private HttpServletRequest uri(String uri) {
        return new MockHttpServletRequest("get", uri);
    }
}
