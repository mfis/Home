package home.service;

import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import home.domain.model.SwitchView;
import home.domain.service.HouseViewService;
import home.model.MessageType;
import homecontroller.domain.model.SettingsModel;

@Component
public class SettingsViewService {

	private static final String EINSCHALTEN = "einschalten";
	private static final String AUSSCHALTEN = "ausschalten";
	private static final String AUSGESCHALTET = "Ausgeschaltet";
	private static final String EINGESCHALTET = "Eingeschaltet";
	private static final String ICON_ENVELOPE = "far fa-envelope";
	private static final String AND_VALUE_IS = "&value=";
	private static final String TYPE_IS = "type=";

	public void fillSettings(Model model, SettingsModel settingsModel) {

		model.addAttribute("clientName", settingsModel.getClientName());
		model.addAttribute("linkClientName",
				HouseViewService.MESSAGEPATH + TYPE_IS + MessageType.SETTINGS_CLIENTNAME + AND_VALUE_IS);

		SwitchView switchPushHints = new SwitchView();
		switchPushHints.setId("pushHints");
		switchPushHints.setName("Push Empfehlungen");
		switchPushHints.setState(settingsModel.isPushHints() ? EINGESCHALTET : AUSGESCHALTET);
		switchPushHints.setLabel(settingsModel.isPushHints() ? AUSSCHALTEN : EINSCHALTEN);
		switchPushHints.setIcon(ICON_ENVELOPE);
		switchPushHints.setLink(HouseViewService.MESSAGEPATH + TYPE_IS + MessageType.SETTINGS_PUSH_HINTS
				+ AND_VALUE_IS + !settingsModel.isPushHints());
		model.addAttribute("pushHints", switchPushHints);

		SwitchView switchPushHintsHysteresis = new SwitchView();
		switchPushHintsHysteresis.setId("pushHintsHysteresis");
		switchPushHintsHysteresis.setName("Empfehlungen Hysterese");
		switchPushHintsHysteresis
				.setState(settingsModel.isHintsHysteresis() ? EINGESCHALTET : AUSGESCHALTET);
		switchPushHintsHysteresis.setLabel(settingsModel.isHintsHysteresis() ? AUSSCHALTEN : EINSCHALTEN);
		switchPushHintsHysteresis.setIcon(ICON_ENVELOPE);
		switchPushHintsHysteresis
				.setLink(HouseViewService.MESSAGEPATH + TYPE_IS + MessageType.SETTINGS_PUSH_HINTS_HYSTERESIS
						+ AND_VALUE_IS + !settingsModel.isHintsHysteresis());
		model.addAttribute("pushHintsHysteresis", switchPushHintsHysteresis);

		SwitchView switchPushDoorbell = new SwitchView();
		switchPushDoorbell.setId("pushDoorbell");
		switchPushDoorbell.setName("Haustür Klingel");
		switchPushDoorbell.setState(settingsModel.isPushDoorbell() ? EINGESCHALTET : AUSGESCHALTET);
		switchPushDoorbell.setLabel(settingsModel.isPushDoorbell() ? AUSSCHALTEN : EINSCHALTEN);
		switchPushDoorbell.setIcon(ICON_ENVELOPE);
		switchPushDoorbell.setLink(HouseViewService.MESSAGEPATH + TYPE_IS + MessageType.SETTINGS_PUSH_DOORBELL
				+ AND_VALUE_IS + !settingsModel.isPushDoorbell());
		model.addAttribute("pushDoorbell", switchPushDoorbell);
	}

}
