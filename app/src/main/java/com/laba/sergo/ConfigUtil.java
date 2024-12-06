package com.laba.sergo;

import android.content.Context;
import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigUtil {

    // Method to load properties from the raw folder
    public static Properties loadProperties(Context context) {
        Properties properties = new Properties();
        try {
            // Access the properties file from the raw folder
            InputStream inputStream = context.getResources().openRawResource(R.raw.config);
            properties.load(inputStream);
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }
}
