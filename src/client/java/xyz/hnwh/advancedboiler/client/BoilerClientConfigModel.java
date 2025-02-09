package xyz.hnwh.advancedboiler.client;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.Hook;
import io.wispforest.owo.config.annotation.Modmenu;

@Modmenu(modId = "advancedboiler")
@Config(name = "boiler-client-config", wrapperName = "BoilerClientConfig")
public class BoilerClientConfigModel {
    @Hook
    public boolean debug = false;
}
