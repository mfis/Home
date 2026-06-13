package de.fimatas.home.client.request;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.Strings;

public class ControllerUtil {

    private ControllerUtil() {
        super();
    }

    private static final String USER_AGENT_APP_WEB_VIEW = "HomeClientAppWebView";

    public static void setEssentialHeader(HttpServletResponse response) {

        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("content-security-policy", "frame-ancestors 'none';");
        response.setHeader("X-Frame-Options", "deny");
        response.setHeader("X-Content-Type-Options", "nosniff");
    }

    public static boolean isWebViewApp(String userAgent) {
        return Strings.CS.startsWith(userAgent, ControllerUtil.USER_AGENT_APP_WEB_VIEW);
    }
}
