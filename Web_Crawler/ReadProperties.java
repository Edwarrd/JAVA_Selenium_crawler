package Web_Crawler;

import java.io.FileInputStream;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


class ReadProperties {
    private Properties properties;
    public static Logger log = LogManager.getLogger(ReadProperties.class.getName());

    ReadProperties(){
        this.properties=readProperties();
    }

    private Properties readProperties() {
        Properties properties = new Properties();
        try{
            properties.load(new FileInputStream("src/main/resources/config.properties"));
        }catch(Exception e){
            log.error("fail to read config file"+e);
        }
        return properties;
    }

    String getValue(String Key){
        return properties.getProperty(Key);
    }
}
