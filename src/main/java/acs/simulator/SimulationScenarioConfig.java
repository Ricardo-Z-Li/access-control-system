package acs.simulator;

import java.util.ArrayList;
import java.util.List;

public class SimulationScenarioConfig {
    private boolean enabled = true;
    private Integer stepDelayMs;
    private List<SimulationPath> paths = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getStepDelayMs() {
        return stepDelayMs;
    }

    public void setStepDelayMs(Integer stepDelayMs) {
        this.stepDelayMs = stepDelayMs;
    }

    public List<SimulationPath> getPaths() {
        return paths;
    }

    public void setPaths(List<SimulationPath> paths) {
        this.paths = paths;
    }
}
