package de.fimatas.home.client.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    public static final String LOGIN_URI = "/login";

    public static final String LOGIN_COOKIECHECK_URI = "/loginCookieCheck";

    public static final String LOGIN_FAILED_URI = "/loginFailed";

    public static final String LOGIN_VIA_APP_FAILED_URI = "/loginViaAppFailed";

    public static final String LOGOFF_URI = "/logoff";

    private static final String LOGIN_TEMPLATE = "login";

    private static final String LOGIN_VIA_APP_TEMPLATE = "loginViaAppMock";

    private static final String LAW_LINK = "lawLink";

    private static final String LOGIN_MESSAGE = "loginMessage";

    @Value("${lawLink}")
    String lawLink;

    @GetMapping(LOGIN_URI)
    public String login(Model model) {
        model.addAttribute(LAW_LINK, lawLink);
        model.addAttribute(LOGIN_MESSAGE, "");
        return LOGIN_TEMPLATE;
    }

    @GetMapping(LOGIN_COOKIECHECK_URI)
    public String loginCookieCheck(Model model) {
        model.addAttribute(LAW_LINK, lawLink);
        model.addAttribute(LOGIN_MESSAGE,
            "Sie müssen den Datenschutzbestimmungen zustimmen, um die Anwendung nutzen zu können.");
        return LOGIN_TEMPLATE;
    }

    @GetMapping(LOGIN_FAILED_URI)
    public String loginFailed(Model model) {
        model.addAttribute(LAW_LINK, lawLink);
        model.addAttribute(LOGIN_MESSAGE, "Name und/oder Passwort nicht korrekt.");
        return LOGIN_TEMPLATE;
    }

    @GetMapping(LOGIN_VIA_APP_FAILED_URI)
    public String loginViaAppFailed(Model model) {
        return LOGIN_VIA_APP_TEMPLATE;
    }

    @GetMapping(path = LOGOFF_URI)
    public String logoff(Model model) {
        model.addAttribute(LAW_LINK, lawLink);
        model.addAttribute(LOGIN_MESSAGE, "Sie wurden erfolgreich abgemeldet.");
        return LOGIN_TEMPLATE;
    }
}
