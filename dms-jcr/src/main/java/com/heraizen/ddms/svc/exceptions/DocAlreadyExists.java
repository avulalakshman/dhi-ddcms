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
public class DocAlreadyExists extends RuntimeException{
    public DocAlreadyExists(String message) {
        super(message);
    }
}
