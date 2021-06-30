/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heraizen.dhi.dlibms.config;

import com.heraizen.ddms.svc.jcr.DlibmsRepoSource;
import com.heraizen.dhi.dlibms.jr2.MultiTenantRepoSource;
import com.heraizen.dhi.dlibms.jr2.TenantContext;
import com.heraizen.dhi.dlibms.jr2.TenantRepoDetails;
import com.heraizen.dhi.dlibms.jr2.YamlReaderUtil;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 *
 * @author Pradeepkm
 */
@Configuration
@Profile("multi-tenant")
@Slf4j
public class DlibmsMultiTenantConfig {

    @Value(value = "${dlib.multitenancy.config}")
    String mtConfigFile;

    @Bean
    public DlibmsRepoSource getMultiTenantRepoSource() {
        List<TenantRepoDetails> repoConfig = YamlReaderUtil.loadTenantRepoDetailsFromCp(mtConfigFile);
        return MultiTenantRepoSource.builder()
                .tenantRepoDetails(repoConfig)
                .tenantResolver(() -> TenantContext.getCurrentTenant())
                .build();
    }
    
    @Bean
    public TenantResolvingFilter getTenantResolvingFilter() {
        return new TenantResolvingFilter();
    }

    FilterRegistrationBean<TenantResolvingFilter> filterRegistrationBean() {
        FilterRegistrationBean<TenantResolvingFilter> filterRegBean = new FilterRegistrationBean<>();
        filterRegBean.setFilter(getTenantResolvingFilter());
        return filterRegBean;
    }
    
}
