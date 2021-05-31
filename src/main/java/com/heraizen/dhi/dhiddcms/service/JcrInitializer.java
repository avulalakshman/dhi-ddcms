/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heraizen.dhi.dhiddcms.service;

import javax.jcr.Credentials;
import javax.jcr.Repository;

/**
 *
 * @author Pradeepkm
 */
public interface JcrInitializer {
    public void initializeRepo(Repository repo, Credentials cred);
}
