package load;

import model.Rule;

import java.util.List;

public interface Loader {
    List<Rule> load(String str) throws LoaderException;
}
