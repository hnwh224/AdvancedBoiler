package xyz.hnwh.advancedboiler.client;

import java.util.ArrayList;
import java.util.List;

public class BoilerDisplayManager {

    private final ArrayList<BoilerDisplay> displays = new ArrayList<>();

    public BoilerDisplayManager() {

    }

    public void registerDisplay(BoilerDisplay display) {
        displays.add(display);
    }

    public BoilerDisplay getDisplay(int displayId) {
        for(BoilerDisplay d : displays) {
            if(d.getId() == displayId) {
                return d;
            }
        }
        return null;
    }

    public void removeDisplay(int displayId) {
        BoilerDisplay display = null;
        for(BoilerDisplay d : displays) {
            if(d.getId() == displayId) {
                display = d;
                break;
            }
        }

        if(display == null) return;

        if(display.getRenderer() != null) display.getRenderer().stop();
        displays.remove(display);
    }

    public void removeAll() {
        displays.forEach(display -> {
            display.getRenderer().stop();
        });

        displays.clear();
    }

    public List<BoilerDisplay> getDisplays() {
        return displays.stream().toList();
    }

}
