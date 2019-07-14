package home.service;

import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import home.domain.model.SwitchView;
import home.domain.service.HouseViewService;
import home.model.MessageType;
import homecontroller.domain.model.SettingsModel;

@Component
public class SettingsViewService {

	public void fillSettings(Model model, SettingsModel settingsModel) {

		model.addAttribute("clientName", settingsModel.getClientName());
		model.addAttribute("linkClientName",
				HouseViewService.MESSAGEPATH + "type=" + MessageType.SETTINGS_CLIENTNAME + "&value=");

		SwitchView switchPushHints = new SwitchView();
		switchPushHints.setId("pushHints");
		switchPushHints.setName("Versand");
		switchPushHints.setState(settingsModel.isPushHints() ? "Eingeschaltet" : "Ausgeschaltet");
		switchPushHints.setLabel(settingsModel.isPushHints() ? "ausschalten" : "einschalten");
		switchPushHints.setIcon("far fa-envelope");
		switchPushHints.setLink(HouseViewService.MESSAGEPATH + "type=" + MessageType.SETTINGS_PUSH_HINTS
				+ "&value=" + !settingsModel.isPushHints());
		model.addAttribute("pushHints", switchPushHints);

		SwitchView switchPushHintsHysteresis = new SwitchView();
		switchPushHintsHysteresis.setId("pushHintsHysteresis");
		switchPushHintsHysteresis.setName("Versand");
		switchPushHintsHysteresis
				.setState(settingsModel.isHintsHysteresis() ? "Eingeschaltet" : "Ausgeschaltet");
		switchPushHintsHysteresis.setLabel(settingsModel.isHintsHysteresis() ? "ausschalten" : "einschalten");
		switchPushHintsHysteresis.setIcon("far fa-envelope");
		switchPushHintsHysteresis
				.setLink(HouseViewService.MESSAGEPATH + "type=" + MessageType.SETTINGS_PUSH_HINTS_HYSTERESIS
						+ "&value=" + !settingsModel.isHintsHysteresis());
		model.addAttribute("pushHintsHysteresis", switchPushHintsHysteresis);

		SwitchView switchPushDoorbell = new SwitchView();
		switchPushDoorbell.setId("pushDoorbell");
		switchPushDoorbell.setName("Versand");
		switchPushDoorbell.setState(settingsModel.isPushDoorbell() ? "Eingeschaltet" : "Ausgeschaltet");
		switchPushDoorbell.setLabel(settingsModel.isPushDoorbell() ? "ausschalten" : "einschalten");
		switchPushDoorbell.setIcon("far fa-envelope");
		switchPushDoorbell.setLink(HouseViewService.MESSAGEPATH + "type=" + MessageType.SETTINGS_PUSH_DOORBELL
				+ "&value=" + !settingsModel.isPushDoorbell());
		model.addAttribute("pushDoorbell", switchPushDoorbell);
	}

}
