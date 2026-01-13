package acs.simulator;

import java.util.ArrayList;
import java.util.List;

public class SimulationPath {
    private String name;
    private List<String> resourceIds = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getResourceIds() {
        return resourceIds;
    }

    public void setResourceIds(List<String> resourceIds) {
        this.resourceIds = resourceIds;
    }
}
