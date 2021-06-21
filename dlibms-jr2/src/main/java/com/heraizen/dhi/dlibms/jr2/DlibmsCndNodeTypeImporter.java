/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heraizen.dhi.dlibms.jr2;

import com.heraizen.ddms.svc.jcr.DlibmsNodeTypeProvider;
import com.heraizen.ddms.svc.exceptions.DocLibRepoException;
import static com.heraizen.ddms.svc.jcr.DlibmsRepoInitializer.DLIB_MD_CND_FILENAME;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.springframework.core.io.ClassPathResource;

/**
 *
 * @author Pradeepkm
 */
@Slf4j
public class DlibmsCndNodeTypeImporter implements DlibmsNodeTypeProvider {

    @Override
    public NodeType[] registerDlibNodeType(Session dlibWsSession) throws RepositoryException {
        ClassPathResource resource = new ClassPathResource(DLIB_MD_CND_FILENAME);
        try (Reader reader = new InputStreamReader(resource.getInputStream())) {
            return CndImporter.registerNodeTypes(reader, dlibWsSession);
        } catch (IOException | ParseException ex) {
            String errMsg = String.format("Error while registering a Node type "
                    + "for Dlib Workspace and error is: %s", ex.getMessage());
            log.error(errMsg);
            log.debug("Stack trace: ", ex);
            throw new DocLibRepoException(errMsg, ex);
        }
    }
}
