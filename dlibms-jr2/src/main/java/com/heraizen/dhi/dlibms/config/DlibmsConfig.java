/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heraizen.dhi.dlibms.config;

import com.heraizen.ddms.svc.jcr.DlibmsRepoSource;
import com.heraizen.dhi.dlibms.jr2.DlibmsSingleTenantRepoSource;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.jcr.Credentials;

/**
 *
 * @author Pradeepkm
 */
@Configuration
public class DlibmsConfig {

    @Value(value = "${dlib.location}")
    String repoLocation;

    Credentials repoCredentials = new SimpleCredentials("admin", "admin".toCharArray());
    
    @Bean
    public DlibmsRepoSource getRepoSource() throws RepositoryException {
        return DlibmsSingleTenantRepoSource.tenantRepoSourceBuilder()
                .repoLocation(repoLocation)
                .credentials(repoCredentials)
                .build();
    }
}
