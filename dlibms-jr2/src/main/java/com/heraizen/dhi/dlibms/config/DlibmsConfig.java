/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heraizen.dhi.dlibms.config;

import com.heraizen.ddms.svc.jcr.JcrInitializer;
import com.heraizen.ddms.svc.jcr.RepoHolder;
import com.heraizen.dhi.dlibms.jr2.DigitalLibRepoInitializer;
import com.heraizen.dhi.dlibms.jr2.DlibmsRepoHolder;
import javax.jcr.SimpleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author Pradeepkm
 */
@Configuration
public class DlibmsConfig {

    @Value(value = "${dlib.location}")
    String repoLocation;
    
    @Bean
    public JcrInitializer getJcrInitializer() {
        return new DigitalLibRepoInitializer();
    }

    @Bean
    public RepoHolder getRepoHolder(JcrInitializer dlibInitializer) {
        return new DlibmsRepoHolder(repoLocation, dlibInitializer, new SimpleCredentials("admin", "admin".toCharArray()));
    }
    
}
