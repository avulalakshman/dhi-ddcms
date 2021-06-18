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
public class DocLibIOException extends RuntimeException {
    
    public DocLibIOException(String message) {
        super(message);
    }
    
    public DocLibIOException(String message, Throwable actualException) {
        super(message, actualException);
    }
}
