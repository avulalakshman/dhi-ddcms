/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heraizen.dhi.dhiddcms.service;

/**
 *
 * @author Pradeepkm
 */
public class JcrException extends RuntimeException {

    public JcrException(String string) {
        super(string);
    }

    public JcrException(String string, Throwable thrwbl) {
        super(string, thrwbl);
    }

}
