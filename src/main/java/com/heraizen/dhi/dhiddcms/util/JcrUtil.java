/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heraizen.dhi.dhiddcms.util;

import com.heraizen.dhi.dhiddcms.service.JcrException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Pradeepkm
 */
@Slf4j
public class JcrUtil {

    /**
     * Recursively outputs the contents of the given node.
     * @param node
     * 
     */
    public static void dump(Node node)  {
        // First output the node path 
        try {
            System.out.println(node.getPath());
            // Skip the virtual (and large!) jcr:system subtree 
            if (node.getName().equals("jcr:system")) {
                return;
            }

            // Then output the properties 
            PropertyIterator properties = node.getProperties();
            while (properties.hasNext()) {
                Property property = properties.nextProperty();
                if (property.getDefinition().isMultiple()) {
                    // A multi-valued property, print all values 
                    Value[] values = property.getValues();
                    for (int i = 0; i < values.length; i++) {
                        System.out.println(
                                property.getPath() + " = " + values[i].getString());
                    }
                } else {
                    // A single-valued property 
                    System.out.println(
                            property.getPath() + " = " + property.getString());
                }
            }

            // Finally output all the child nodes recursively 
            NodeIterator nodes = node.getNodes();
            while (nodes.hasNext()) {
                dump(nodes.nextNode());
            }
        } catch(RepositoryException re) {
            log.error("Error while dumping node ", re.getMessage());
            log.debug("Error stack trace :", re);
            throw new JcrException("Error while dumping Node ", re);
        }
    }
}
