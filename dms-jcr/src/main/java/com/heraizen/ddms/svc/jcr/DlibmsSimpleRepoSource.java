/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heraizen.ddms.svc.jcr;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import lombok.Builder;

/**
 *
 * @author Pradeepkm
 */
public class DlibmsSimpleRepoSource extends DlibmsRepo implements DlibmsRepoSource {
    
    @Builder(builderMethodName = "repoSourceBuilder")
    public DlibmsSimpleRepoSource(Repository repo, Credentials repoCredentials, DlibmsNodeTypeProvider dlibmsNtRegistrationMaker) {
        super(repo, repoCredentials, dlibmsNtRegistrationMaker);
    }
    
    @Override
    public DlibmsRepo getRepo() {
        return this;  //this is also DlibmsRepo
    }    
}
