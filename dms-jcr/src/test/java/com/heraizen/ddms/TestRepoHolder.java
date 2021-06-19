/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heraizen.ddms;

import com.heraizen.ddms.svc.jcr.JcrWrapper;
import com.heraizen.ddms.svc.jcr.RepoHolder;

/**
 *
 * @author Pradeepkm
 */
public class TestRepoHolder implements RepoHolder {

    private JcrWrapper repo;
    
    public TestRepoHolder(JcrWrapper wrapper) {
        this.repo = wrapper;
    }
    
    @Override
    public JcrWrapper getRepo() {
        return repo;
    }
    
}
