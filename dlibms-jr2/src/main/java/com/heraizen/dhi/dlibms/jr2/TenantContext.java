/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heraizen.dhi.dlibms.jr2;

/**
 *
 * @author Pradeepkm
 */
public class TenantContext {
    
    private static ThreadLocal<String> currentTenant = new ThreadLocal<>();
    
    private TenantContext(){
    }
    
    public static String getCurrentTenant() {
        return currentTenant.get();
    }
    
    public static void setTenant(String tenant) {
        currentTenant.set(tenant);
    }
    
    // Method is used to remove current tenant
    public void resetTenant() {
        currentTenant.remove();
    }
}
