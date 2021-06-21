/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heraizen.ddms.svc.jcr;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

/**
 *
 * @author Pradeepkm
 */
public interface DlibmsNodeTypeProvider {
    public NodeType[] registerDlibNodeType(Session wsSession) throws RepositoryException;
}
