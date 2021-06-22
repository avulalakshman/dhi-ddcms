package com.heraizen.dhi.dlibms.jr2;

import com.heraizen.ddms.svc.exceptions.DocLibIOException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

@Slf4j
public class YamlReaderUtil {

    private static List<TenantRepoDetails> loadTenantDetails(Resource yamlResource) {
        Yaml yaml = new Yaml(new Constructor(TenantRepoDetails.class));
        List<TenantRepoDetails> tenantRepoDetails = new ArrayList<>();
        try (InputStream in = yamlResource.getInputStream()) {
            yaml.loadAll(in)
                    .forEach(o -> tenantRepoDetails.add((TenantRepoDetails) o));
        } catch (IOException ex) {
            String errMsg = String.format("Error reading YAML Tenant configuration file %s", ex.getMessage());
            log.error(errMsg);
            log.debug("Stack trace :\n", ex);
            throw new DocLibIOException(errMsg, ex);
        }
        return tenantRepoDetails;
    }

    public static List<TenantRepoDetails> loadTenantRepoDetailsFromCp(String yamlFile) {
        return loadTenantDetails(new ClassPathResource(yamlFile));
    }

    public static List<TenantRepoDetails> loadTenantRepoDetailsFromPath(Path filePath) {
        return loadTenantDetails(new FileSystemResource(filePath));
    }

}
