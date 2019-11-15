package model;

import java.util.Collections;
import java.util.List;

public class Composite extends Component {
    private List<Component> components;

    public List<Component> getComponents() {
        return Collections.unmodifiableList(components);
    }

    public void addComponent(Component component) {
        components.add(component);
    }

    public void removeComponent(Component component) {
        components.remove(component);
    }
}
