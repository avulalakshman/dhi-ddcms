package com.heraizen.dhi.dhiddcms.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

@SpringBootTest
public class MultiTenantRepoHolderTest {

    @Autowired
    private MultiTenantRepoHolder multiTenantRepoHolder;

    @Test
    public void testNoOp() {
        
    }
    
//    @Test
//    public void getTenantRepoLocationForExistingTenantId(){
//        Optional<String> opt = multiTenantRepoHolder.getTenantRepoLocation("NCET");
//        Assertions.assertThat(opt.get()).isNotNull().as("/opt/NCET");
//
//    }
    
//    @Test
//    public void getTenantRepoLocationForNonExistingTenantId(){
//        Optional<String> opt = multiTenantRepoHolder.getTenantRepoLocation("AICTE");
//        Assertions.assertThat(opt).isNotPresent();
//
//    }
}




