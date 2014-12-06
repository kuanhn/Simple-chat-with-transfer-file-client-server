package main.Helpers;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.ResourceBundle;

/* custome ResourceBundle with get and put Object */
public class CustomBundle extends ResourceBundle {
    private HashMap<String, Object> bundle = new HashMap<String, Object>();

    @Override
    protected Object handleGetObject(String key) {
        return bundle.get(key);
    }

    @Override
    public Enumeration<String> getKeys() {
        return null;
    }

    public void put(String key, Object value){
        bundle.put(key, value);
    }
}