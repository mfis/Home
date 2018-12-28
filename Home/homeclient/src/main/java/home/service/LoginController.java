package home.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class LoginController {

	private static final String LOGIN = "login";

	private static final String LAW_LINK = "lawLink";

	private static final String LOGIN_MESSAGE = "loginMessage";

	@Value("${lawLink}")
	String lawLink;

	@RequestMapping("/login")
	public String login(Model model) {
		model.addAttribute(LAW_LINK, lawLink);
		model.addAttribute(LOGIN_MESSAGE, "");
		return LOGIN;
	}

	@RequestMapping("/loginCookieCheck")
	public String loginCookieCheck(Model model) {
		model.addAttribute(LAW_LINK, lawLink);
		model.addAttribute(LOGIN_MESSAGE,
				"Sie müssen den Datenschutzbestimmungen zustimmen, um die Anwendung nutzen zu können.");
		return LOGIN;
	}

	@RequestMapping("/loginFailed")
	public String loginFailed(Model model) {
		model.addAttribute(LAW_LINK, lawLink);
		model.addAttribute(LOGIN_MESSAGE, "Name und/oder Passwort nicht korrekt.");
		return LOGIN;
	}

	@RequestMapping("/logoff")
	public String logoff(Model model) {
		model.addAttribute(LAW_LINK, lawLink);
		model.addAttribute(LOGIN_MESSAGE, "Sie wurden erfolgreich abgemeldet.");
		return LOGIN;
	}
}
