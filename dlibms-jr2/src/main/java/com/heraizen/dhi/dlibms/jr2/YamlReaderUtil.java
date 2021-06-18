package com.heraizen.dhi.dlibms.jr2;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class YamlReaderUtil {

    private final List<TenantRepoDetails> tenantRepoDetails = new ArrayList<>();

    @PostConstruct
    public void loadTenantRepoDetails() {
        Yaml yaml = new Yaml(new Constructor(TenantRepoDetails.class));
        try (InputStream in = YamlReaderUtil.class.getResourceAsStream("/tenantrepo.yaml")) {
            Iterable<Object> obj = yaml.loadAll(in);
            for (Object o : obj) {
                TenantRepoDetails trd = (TenantRepoDetails) o;
                getTenantRepoDetails().add(trd);
            }
        } catch (Exception e) {
            log.error("While loading tenant repo yaml {}", e);
        }
    }

    public List<TenantRepoDetails> getTenantRepoDetails() {
        return tenantRepoDetails;
    }
}
