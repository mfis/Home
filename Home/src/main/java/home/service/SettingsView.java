package home.service;

import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import homecontroller.domain.model.SettingsModel;

@Component
public class SettingsView {

	public void fillSettings(Model model, SettingsModel settingsModel) {

		model.addAttribute("switchPush_icon", "far fa-envelope");
		model.addAttribute("switchPush", (settingsModel.isPushActive() ? "Eingeschaltet" : "Ausgeschaltet"));
		model.addAttribute("switchPush_link", "/settingspushtoggle?");
		model.addAttribute("switchPush_label", (settingsModel.isPushActive() ? "ausschalten" : "einschalten"));

		model.addAttribute("textPushoverApi", settingsModel.getPushoverApiToken());
		model.addAttribute("textPushoverUser", settingsModel.getPushoverUserId());
		model.addAttribute("textPushoverDevice", settingsModel.getPushoverDevice());

		model.addAttribute("switchPushover_link", "/settingspushover");
	}

}
