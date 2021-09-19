package de.fimatas.home.client.request;

import javax.servlet.http.HttpServletResponse;

public class ControllerUtil {

    private ControllerUtil() {
        super();
    }

    public static final String USER_AGENT_APP_WEB_VIEW = "HomeClientAppWebView";

    public static void setEssentialHeader(HttpServletResponse response) {

        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("content-security-policy", "frame-ancestors 'none';");
        response.setHeader("X-Frame-Options", "deny");
        response.setHeader("X-Content-Type-Options", "nosniff");
    }
}
