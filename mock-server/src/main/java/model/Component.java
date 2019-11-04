package model;

import java.util.Collections;
import java.util.List;

public abstract class Component {
    private List<Rule> rules;

    public List<Rule> getRules() {
        return Collections.unmodifiableList(rules);
    }

    public void addRule(Rule rule) {
        rules.add(rule);
    }

    public void removeRule(Rule rule) {
        rules.remove(rule);
    }
}
