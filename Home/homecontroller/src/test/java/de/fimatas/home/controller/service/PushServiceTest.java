package de.fimatas.home.controller.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PushServiceTest {

    @InjectMocks
    private PushService pushService;

    @Mock
    private SettingsService settingsService;

    @Test
    public void testFormatMessagesAllNew() {
        //
    }


}
