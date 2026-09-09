package uz.uwon.pharm.storage;


import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class FileSystemStorageService implements StorageService {
    private final Path uploadLocation = Paths.get(new StorageProperties().getUploadsLocation());
    private final Path tempLocation = Paths.get(new StorageProperties().getTempLocation());

    @PostConstruct
    public void initialize() {
        init();
    }
    @Override
    public void init() {
        log.info("Initializing storage service...");
        try {
            Files.createDirectories(uploadLocation);
            Files.createDirectories(tempLocation);
            System.out.println("Location : "+uploadLocation.toAbsolutePath());
        }
        catch (IOException e) {
            throw new StorageException("Could not initialize storage", e);
        }

    }

    @Override
    public String store(MultipartFile file) {
        log.info("Storing file {}",file.getOriginalFilename());
        String filename = getAndValidateFilename(file);
        try {
            Files.copy(file.getInputStream(), this.uploadLocation.resolve(filename),
                    StandardCopyOption.REPLACE_EXISTING);
            return filename;
        } catch (IOException e) {
            throw new StorageException("Fayl yuklashda xatolik bo`ldi! ", e);
        }
    }

    @Override
    public Path load(String filename) {
        log.info("Loading file {}",filename);
        return uploadLocation.resolve(filename);
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            Path file = load(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            }
            else {
                throw new StorageFileNotFoundException(
                        "Could not read file: " + filename);

            }
        }
        catch (MalformedURLException e) {
            throw new StorageFileNotFoundException("Could not read file: " + filename, e);
        }
    }

    @Override
    public void delete(String filename) {
        log.info("Delete file {}", filename);
        try {
            Files.deleteIfExists(load(filename));
        } catch (IOException e) {
            throw new RuntimeException("Could not delete file: " + filename);
        }
    }

    private String getAndValidateFilename(MultipartFile file) {
        log.info("Validation file {}",file.getOriginalFilename());
        if (file.isEmpty()) {
            return "no_image.png";
        }
        String name = file.getOriginalFilename();
        int ind = -1;
        assert name != null;
        for (int i = name.length()-1; i > 0; i--){
            if (name.charAt(i) =='.'){
                ind = i;
                break;
            }
        }
        if(ind < 0){
            throw new StorageException("Faylni yuklashdaa xatolik");
        }
        return UUID.randomUUID()+name.substring(ind);
    }
    @Bean
    public StorageProperties createStorageProperites(){
        return new StorageProperties();
    }
}
