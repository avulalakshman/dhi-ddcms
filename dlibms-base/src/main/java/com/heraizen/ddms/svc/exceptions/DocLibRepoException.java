/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heraizen.ddms.svc.exceptions;

/**
 *
 * @author Pradeepkm
 */
public class DocLibRepoException extends RuntimeException {

    public DocLibRepoException(String message) {
        super(message);
    }

    public DocLibRepoException(String string, Throwable thrwbl) {
        super(string, thrwbl);
    }

}
