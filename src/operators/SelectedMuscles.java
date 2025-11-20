package operators;

import java.util.ArrayList;
import java.util.List;

public class SelectedMuscles {
    List<String> names = new ArrayList<>();
    private ArrayList <Integer> num = new ArrayList<Integer>();
    int channels;

    public SelectedMuscles(List<String> names, ArrayList<Integer> num) {
        this.names = names;
        this.num = num;
    }

    public SelectedMuscles() {

    }

    public List<String> getNames() {
        return names;
    }

    public void setNames(List<String> names) {
        this.names = names;
    }

    public int getChannels() {
        return channels;
    }

    public int getNMuscles(){
        return names.size();
    }

    public void setChannels(int channels) {
        this.channels = channels;
    }

    public void add(String name, int index) {
        names.add(name);
        num.add(index);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Selected Muscles:\n");
        for (int i = 0; i < names.size(); i++) {
            sb.append((i + 1)).append(". ").append(names.get(i));
            sb.append("\n");
        }
        return sb.toString();
    }
}
