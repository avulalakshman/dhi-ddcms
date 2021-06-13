/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heraizen.dhi.dhiddcms.exceptions;

/**
 *
 * @author Pradeepkm
 */
public class DocNotFoundException extends RuntimeException {

    public DocNotFoundException(String message) {
        super(message);
    }

    public DocNotFoundException(String message,
            Throwable exception) {
        super(message, exception);
    }
}
