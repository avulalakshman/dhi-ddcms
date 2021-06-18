/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heraizen.ddms.svc.jcr;

import com.heraizen.ddms.svc.exceptions.DocLibRepoException;
import java.util.Collection;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Pradeepkm
 */
@Slf4j
public class DlibUtil {

    public static String[] toStringArray(Collection<String> c, Function<String, String> transformer) {
        return c.stream().map(transformer).toArray(i -> new String[i]);
    }

    public static Set<String> toStringSet(Supplier<Set<String>> supplier, Value[] vals) throws RepositoryException {
        Set<String> coll = supplier.get();
        for (Value v : vals) {
            coll.add(v.getString());
        }
        return coll;
    }

    /**
     * Recursively outputs the contents of the given node.
     *
     * @param node
     *
     */
    public static void dump(Node node) {
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
                    for (Value value : values) {
                        System.out.println(String.format("%s = %s",
                                property.getPath(),
                                value.getType() == PropertyType.BINARY ? "BINARY" : value.getString()));
                    }
                } else {
                    // A single-valued property 
                    System.out.println(String.format("%s = %s",
                            property.getPath(),
                            property.getType() == PropertyType.BINARY ? "BINARY" : property.getString()));
                }
            }

            // Finally output all the child nodes recursively 
            NodeIterator nodes = node.getNodes();
            while (nodes.hasNext()) {
                dump(nodes.nextNode());
            }
        } catch (RepositoryException re) {
            log.error("Error while dumping node ", re.getMessage());
            log.debug("Error stack trace :", re);
            throw new DocLibRepoException("Error while dumping Node ", re);
        }
    }

}
