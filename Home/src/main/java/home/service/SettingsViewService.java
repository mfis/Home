package home.service;

import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import home.domain.model.SwitchView;
import homecontroller.domain.model.SettingsModel;

@Component
public class SettingsViewService {

	public void fillSettings(Model model, SettingsModel settingsModel) {

		SwitchView view = new SwitchView();
		view.setId("pushHints");
		view.setName("Versand");
		view.setState(settingsModel.isPushActive() ? "Eingeschaltet" : "Ausgeschaltet");
		view.setLabel(settingsModel.isPushActive() ? "ausschalten" : "einschalten");
		view.setIcon("far fa-envelope");
		view.setLink("/settingspushtoggle?");
		model.addAttribute("switchPush", view);

		model.addAttribute("textPushoverDevice", settingsModel.getPushoverDevice());
		model.addAttribute("switchPushover_link", "/settingpushoverdevice");
	}

}
