package com.dimon.catanbackend.config.file;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
/**
 * Configuration class for file storage properties. This class is used to bind file-related
 * properties from the application configuration file (e.g., `application.properties` or `application.yml`)
 * using the prefix `file`.
 *
 * The `uploadDir` property represents the directory where files will be uploaded and stored.
 *
 * Annotations used:
 * - {@link Component} to mark this class as a Spring-managed bean.
 * - {@link ConfigurationProperties} to bind properties with the prefix `file` to this class.
 * - {@link Getter} and {@link Setter} to generate getters and setters for the `uploadDir` property using Lombok.
 *
 * Example configuration in `application.properties`:
 * <pre>
 * file.upload-dir=uploads/
 * </pre>
 *
 * Example usage:
 * <pre>
 * {@code
 * @Autowired
 * private FileStorageProperties fileStorageProperties;
 *
 * String uploadDir = fileStorageProperties.getUploadDir();
 * }
 * </pre>
 *
 * @see Component
 * @see ConfigurationProperties
 * @see Getter
 * @see Setter
 *
 */
@Getter
@Component
@Setter
@ConfigurationProperties(prefix = "file")
public class FileStorageProperties {
    private String uploadDir;

}
