package helpers;

public class ValuePos {

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    int position;
    String value;

    public ValuePos(String value, int position){
        this.position = position;
        this.value = value;
    }
}
