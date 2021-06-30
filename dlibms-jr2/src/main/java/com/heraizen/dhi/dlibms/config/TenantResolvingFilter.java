/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heraizen.dhi.dlibms.config;

import com.heraizen.dhi.dlibms.jr2.TenantContext;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.util.StringUtils;

/**
 * NOTE: This is a temporary filter until security is added to the Service
 * TenantResolvingFilter will resolve the tenant based on tenant-id header or 
 * tenant-id query parameter.
 * @author Pradeepkm
 */
@Slf4j
public class TenantResolvingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest sreq, ServletResponse sresp, FilterChain fchain) throws IOException, ServletException {
        log.debug("Trying to resolve tenant....");
        String tenantId = ((HttpServletRequest) sreq).getHeader("tenant-id");
        if (!StringUtils.hasText(tenantId)) {
            log.debug("Header tenant-id is NOT set! checking query Param");
            tenantId = ((HttpServletRequest) sreq).getParameter("tenant-id");
            if (!StringUtils.hasText(tenantId)) {
                log.error("Tenant ID is not defined! Expecting tenant id either in header OR query param 'tenant-id");
                ((HttpServletResponse) sresp).sendError(HttpStatus.SC_BAD_REQUEST, "Could not resolve Tenant! Please define either header 'tenant-id' or query param 'tenant-id' ");
                return;//let's not continue the chain, and return from here...
            }
        }
        TenantContext.setTenant(tenantId.trim());
        log.info("Resolved tenant as {} and TenantContext is appropriately set",
                tenantId);
        fchain.doFilter(sreq, sresp);
    }

}
