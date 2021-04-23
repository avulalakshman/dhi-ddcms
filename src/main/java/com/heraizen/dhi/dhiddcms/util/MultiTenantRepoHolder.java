package com.heraizen.dhi.dhiddcms.util;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Configuration
@ConfigurationProperties(prefix = "dhiddcms")
@PropertySource(value = "classpath:tenantrepo.yaml", factory = YamlPropertySourceFactory.class)
@Slf4j
public class MultiTenantRepoHolder {

    private List<TenantRepoDetails> tenantRepoDetails;

    private Map<String,String> tenantRepMap;
    public List<TenantRepoDetails> getTenantRepoDetails() {
        return tenantRepoDetails;
    }

    public void setTenantRepoDetails(List<TenantRepoDetails> tenantRepoDetails) {
        this.tenantRepoDetails = tenantRepoDetails;
    }

    @PostConstruct
    public void init(){
        tenantRepMap = getTenantRepoDetails().stream()
                                            .collect(Collectors.toMap(TenantRepoDetails::getTenantId,
                                                                      TenantRepoDetails::getTenantLocation));
        log.info("Total Tenant count is :{}",tenantRepMap.size());
    }
    public Optional getTenantRepoLocation(String tenantId){
        return Optional.ofNullable(tenantRepMap.get(tenantId));
    }
}
