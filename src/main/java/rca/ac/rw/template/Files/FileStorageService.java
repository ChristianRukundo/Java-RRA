package rca.ac.rw.template.Files;

import rca.ac.rw.template.commons.exceptions.AppException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.Stream;

@Service
public class FileStorageService {


    @Value("${uploads.directory}")
    private String root;

    @Value("${uploads.directory.customer_profiles}")
    private String userProfilesFolder;

    @Value("${uploads.directory.docs}")
    private String docsFolder;


    @Bean
    public void init() {
        try {
            Files.createDirectories(Paths.get(root, userProfilesFolder, docsFolder));
        } catch (IOException e) {
            throw new AppException(e.getMessage());
        }
    }

    public String save(MultipartFile file, String directory, String filename) {
        try {
            Path path = Paths.get(directory);
            Files.copy(file.getInputStream(), path.resolve(Objects.requireNonNull(filename)));
            return path + "/" + filename;
        } catch (Exception e) {
            throw new AppException(e.getMessage());
        }
    }


    public Resource load(String uploadDirectory, String fileName) {
        Path path = Paths.get(uploadDirectory);

        try {
            Path file = path.resolve(fileName);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Could not read the file!");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    public void removeFileOnDisk(String filePath) {
        try {
            FileSystemUtils.deleteRecursively(Paths.get(filePath));
        } catch (IOException e) {
            throw new AppException(e.getMessage());
        }
    }

    public void deleteAll() {
        FileSystemUtils.deleteRecursively(Paths.get(root).toFile());
    }


    public Stream<Path> loadAll() {
        try {
            return Files.walk(Paths.get(this.root), 1).filter(path -> !path.equals(this.root)).map(Paths.get(this.root)::relativize);
        } catch (IOException e) {
            throw new AppException(e.getMessage());
        }
    }
}
