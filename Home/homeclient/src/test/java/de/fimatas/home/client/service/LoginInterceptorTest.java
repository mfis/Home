package de.fimatas.home.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fimatas.home.client.domain.service.AppViewService;
import de.fimatas.home.client.request.AppRequestMapping;
import de.fimatas.home.client.request.ControllerRequestMapping;
import de.fimatas.home.client.request.HomeRequestMapping;
import de.fimatas.home.library.domain.model.HouseModel;
import de.fimatas.home.library.model.Pages;
import de.fimatas.home.library.util.HomeAppConstants;
import de.fimatas.users.api.TokenResult;
import de.fimatas.users.api.UserAPI;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.ArrayList;
import java.util.List;

import static de.fimatas.users.api.UsersConstants.USERS_CHECK_PIN_PATH;
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

    @MockitoBean
    private UserAPI userAPI;

    @MockitoSpyBean
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
        assertTrue(getLoginInterceptor().isAssetRequest(uri("/a.js")));
        assertTrue(getLoginInterceptor().isAssetRequest(uri("/a.css")));
        assertTrue(getLoginInterceptor().isAssetRequest(uri("/robots.txt")));
    }

    @Test
    void testIsAssetRequestFalse() {
        assertFalse(getLoginInterceptor().isAssetRequest(uri("/")));
        assertFalse(getLoginInterceptor().isAssetRequest(uri("/settings")));
        assertFalse(getLoginInterceptor().isAssetRequest(uri("/js")));
        assertFalse(getLoginInterceptor().isAssetRequest(uri("/?js")));
        assertFalse(getLoginInterceptor().isAssetRequest(uri("/;js")));
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
        for(String path : interceptedPaths()) {
            System.out.println("path: " + path);
            mockMvc.perform(MockMvcRequestBuilders.post("/")
                            .header(LoginInterceptor.USER_AGENT, THE_USER_AGENT)
                            .param(LoginInterceptor.LOGIN_USERNAME, "x")
                            .param(LoginInterceptor.LOGIN_PASSWORD, "y")
                            .param(LoginInterceptor.LOGIN_COOKIEOK, "true"))
                    .andExpect(MockMvcResultMatchers.status().isFound())
                    .andExpect(MockMvcResultMatchers.redirectedUrl("/loginFailed"));
        }
    }

    @Test
    void testRootAuthFailedNoLoginData() throws Exception {
        for(String path : interceptedPaths()) {
            System.out.println("path: " + path);
            mockMvc.perform(MockMvcRequestBuilders.get("/"))
                    .andExpect(MockMvcResultMatchers.status().isFound())
                    .andExpect(MockMvcResultMatchers.redirectedUrl("/login"));
        }
    }

    @Test
    void testAppModelNoLoginDataGeneral() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(AppRequestMapping.URI_GET_APP_MODEL))
                .andExpect(MockMvcResultMatchers.status().isFound())
                .andExpect(MockMvcResultMatchers.redirectedUrl("/login"));
    }

    @Test
    void testAppModelNoLoginDataAppViewTargetWatch() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(AppRequestMapping.URI_GET_APP_MODEL + "?viewTarget=" + AppViewService.AppViewTarget.WATCH.name().toLowerCase()))
                .andExpect(MockMvcResultMatchers.status().isFound())
                .andExpect(MockMvcResultMatchers.redirectedUrl("/login"));
    }

    @Test
    void testAppModelNoLoginDataAppViewTargetComplication() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(AppRequestMapping.URI_GET_APP_MODEL + "?viewTarget=" + AppViewService.AppViewTarget.COMPLICATION.name().toLowerCase()))
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
        int invocationCounter = 0;
        for(String path : interceptedPaths()) {
            System.out.println("path: " + path);
            mockMvc.perform(MockMvcRequestBuilders.get("/")
                            .header(LoginInterceptor.APP_DEVICE, THE_DEVICE)
                            .header(LoginInterceptor.APP_USER_NAME, THE_USER_NAME)
                            .header(LoginInterceptor.APP_USER_TOKEN, "xyz"))
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized());
            invocationCounter++;
            verify(userAPI, times(invocationCounter))
                    .checkToken(anyString(), anyString(), anyString(), anyString(), anyBoolean());
        }
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
        mockMvc.perform(MockMvcRequestBuilders.post(USERS_CHECK_PIN_PATH)
                        .header(LoginInterceptor.USER_AGENT, THE_USER_AGENT))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void testUsersCheckPinWrongToken() throws Exception {
        System.setProperty(HomeAppConstants.CONTROLLER_CLIENT_COMM_TOKEN, THE_CIENT_COMM_TOKEN);
        mockMvc.perform(MockMvcRequestBuilders.post(USERS_CHECK_PIN_PATH)
                        .header(LoginInterceptor.USER_AGENT, THE_USER_AGENT)
                        .header(HomeAppConstants.CONTROLLER_CLIENT_COMM_TOKEN, "xyz"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void testUsersCheckPinCorrectToken() throws Exception {
        System.setProperty(HomeAppConstants.CONTROLLER_CLIENT_COMM_TOKEN, THE_CIENT_COMM_TOKEN);
        mockMvc.perform(MockMvcRequestBuilders.post(USERS_CHECK_PIN_PATH)
                        .header(LoginInterceptor.USER_AGENT, THE_USER_AGENT)
                        .header(HomeAppConstants.CONTROLLER_CLIENT_COMM_TOKEN, THE_CIENT_COMM_TOKEN))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            ControllerRequestMapping.UPLOAD_METHOD_PREFIX + "HouseModel",
            ControllerRequestMapping.CONTROLLER_LONG_POLLING_FOR_AWAIT_MESSAGE_REQUEST
    })
    void testUploadOK(String url) throws Exception {
        System.setProperty(HomeAppConstants.CONTROLLER_CLIENT_COMM_TOKEN, THE_CIENT_COMM_TOKEN);
        mockMvc.perform(MockMvcRequestBuilders.post(url)
                        .header(LoginInterceptor.USER_AGENT, THE_USER_AGENT)
                        .header(HomeAppConstants.CONTROLLER_CLIENT_COMM_TOKEN, THE_CIENT_COMM_TOKEN)
                        .content(new ObjectMapper().writeValueAsString(new HouseModel()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }


    @ParameterizedTest
    @ValueSource(strings = {
            ControllerRequestMapping.UPLOAD_METHOD_PREFIX + "HouseModel",
            ControllerRequestMapping.CONTROLLER_LONG_POLLING_FOR_AWAIT_MESSAGE_REQUEST
    })
    void testUploadWrongToken(String url) throws Exception {
        System.setProperty(HomeAppConstants.CONTROLLER_CLIENT_COMM_TOKEN, THE_CIENT_COMM_TOKEN);
        mockMvc.perform(MockMvcRequestBuilders.post(url)
                        .header(LoginInterceptor.USER_AGENT, THE_USER_AGENT)
                        .header(HomeAppConstants.CONTROLLER_CLIENT_COMM_TOKEN, "xyz")
                        .content(new ObjectMapper().writeValueAsString(new HouseModel()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void testUploadBlocking() throws Exception {
        System.setProperty(HomeAppConstants.CONTROLLER_CLIENT_COMM_TOKEN, THE_CIENT_COMM_TOKEN);
        // correct token
        mockMvc.perform(MockMvcRequestBuilders.post(ControllerRequestMapping.CONTROLLER_LONG_POLLING_FOR_AWAIT_MESSAGE_REQUEST)
                        .header(LoginInterceptor.USER_AGENT, THE_USER_AGENT)
                        .header(HomeAppConstants.CONTROLLER_CLIENT_COMM_TOKEN, THE_CIENT_COMM_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk());
        for (int i = 0; i < 5; i++) {
            // wrong token until blocking
            mockMvc.perform(MockMvcRequestBuilders.post(ControllerRequestMapping.CONTROLLER_LONG_POLLING_FOR_AWAIT_MESSAGE_REQUEST)
                            .header(LoginInterceptor.USER_AGENT, THE_USER_AGENT)
                            .header(HomeAppConstants.CONTROLLER_CLIENT_COMM_TOKEN, "xyz")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized());
        }
        // still blocking with correct token
        mockMvc.perform(MockMvcRequestBuilders.post(ControllerRequestMapping.CONTROLLER_LONG_POLLING_FOR_AWAIT_MESSAGE_REQUEST)
                        .header(LoginInterceptor.USER_AGENT, THE_USER_AGENT)
                        .header(HomeAppConstants.CONTROLLER_CLIENT_COMM_TOKEN, THE_CIENT_COMM_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/main.js", "/?a=b"})
    void testValidURI(String input){
        assertFalse(getLoginInterceptor().isInvalidRequest(MockMvcRequestBuilders.get(input).buildRequest(new MockServletContext())));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/:", "/;main.js", "/%12"})
    void testInValidURI(String input){
        assertTrue(getLoginInterceptor().isInvalidRequest(MockMvcRequestBuilders.get(input).buildRequest(new MockServletContext())));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/a.js", "/b.css"})
    void testIsAssetRequest(String input){
        assertTrue(getLoginInterceptor().isAssetRequest(MockMvcRequestBuilders.get(input).buildRequest(new MockServletContext())));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/?x=a.js", "/message", "/"})
    void testIsNoAssetRequest(String input){
        assertFalse(getLoginInterceptor().isAssetRequest(MockMvcRequestBuilders.get(input).buildRequest(new MockServletContext())));
    }

    @Nonnull
    @SneakyThrows
    private static LoginInterceptor getLoginInterceptor() {
        var li = new LoginInterceptor();
        ReflectionTestUtils.setField(li, "appdistributionWebUrl", "http://localhost/");
        li.postConstruct();
        return li;
    }

    private HttpServletRequest uri(String uri) {
        return new MockHttpServletRequest("get", uri);
    }

    private List<String> interceptedPaths(){
        var list = new ArrayList<String>();
        list.add(Pages.PATH_HOME);
        list.addAll(HomeRequestMapping.ALL_NON_PAGE_HOME_URIS);
        list.addAll(AppRequestMapping.ALL_NON_LOGIN_APP_URIS);
        return list;
    }
}
