package com.dimon.catanbackend.config.file;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Getter
@Component
@Setter
@ConfigurationProperties(prefix = "file")
public class FileStorageProperties {
    private String uploadDir;

}
