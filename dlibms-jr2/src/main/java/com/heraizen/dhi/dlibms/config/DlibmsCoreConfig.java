/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heraizen.dhi.dlibms.config;

import com.heraizen.ddms.svc.DigitalLibMgmtService;
import com.heraizen.ddms.svc.jcr.DigitalLibMgmtServiceJcrImpl;
import com.heraizen.ddms.svc.jcr.DlibmsRepoSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author Pradeepkm
 */
@Configuration
public class DlibmsCoreConfig {
    
    @Bean
    DigitalLibMgmtService getDigitalLibMgmtService(DlibmsRepoSource repoSource) {
        return DigitalLibMgmtServiceJcrImpl.builder()
                .dlibmsRepoSource(repoSource)
                .build();
    }
}
