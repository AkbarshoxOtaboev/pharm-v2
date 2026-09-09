package uz.uwon.pharm.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.system.ApplicationHome;

import java.io.File;

@ConfigurationProperties("storage")
@Getter
@Setter
public class StorageProperties {
    private String uploadsLocation = System.getProperty("user.dir") + "\\uploads\\";
    private String tempLocation = System.getProperty("user.dir") + "\\temp\\";

    public StorageProperties(){
        ApplicationHome home = new ApplicationHome(this.getClass());
        File jarFile = home.getSource();
        File jarDir = home.getDir();
        uploadsLocation = jarDir+"/uploads";
        tempLocation = jarDir+"/temp";
        File uploadDir = new File(jarDir, "uploads");
    }
}
