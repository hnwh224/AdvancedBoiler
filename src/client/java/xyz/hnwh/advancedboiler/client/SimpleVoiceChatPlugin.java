package xyz.hnwh.advancedboiler.client;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatClientApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.ClientVoicechatInitializationEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;

public class SimpleVoiceChatPlugin implements VoicechatPlugin {

    public static VoicechatClientApi clientApi;

    @Override
    public String getPluginId() {
        return AdvancedboilerClient.MOD_ID;
    }

    @Override
    public void initialize(VoicechatApi api) {

    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(ClientVoicechatInitializationEvent.class, e -> {
            clientApi = e.getVoicechat();
        });
    }
}
