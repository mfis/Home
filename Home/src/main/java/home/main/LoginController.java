package home.main;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class LoginController {

	@Value("${lawLink}")
	String lawLink;

	@RequestMapping("/login")
	public String login(Model model) {
		model.addAttribute("lawLink", lawLink);
		model.addAttribute("loginMessage", "");
		return "login";
	}

	@RequestMapping("/loginCookieCheck")
	public String loginCookieCheck(Model model) {
		model.addAttribute("lawLink", lawLink);
		model.addAttribute("loginMessage", "Sie müssen den Datenschutzbestimmungen zustimmen, um die Anwendung nutzen zu können.");
		return "login";
	}

	@RequestMapping("/loginFailed")
	public String loginFailed(Model model) {
		model.addAttribute("lawLink", lawLink);
		model.addAttribute("loginMessage", "Name und/oder Passwort nicht korrekt.");
		return "login";
	}

	@RequestMapping("/logoff")
	public String logoff(Model model) {
		model.addAttribute("lawLink", lawLink);
		model.addAttribute("loginMessage", "Sie wurden erfolgreich abgemeldet.");
		return "login";
	}
}
