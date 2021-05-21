package com.heraizen.dhi.dhiddcms.web;

//<<<<<<< HEAD
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
//=======
//>>>>>>> 55286555478c84ab7292054d68f7028f391ae6b4
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.heraizen.dhi.dhiddcms.model.Metadata;
import com.heraizen.dhi.dhiddcms.service.DigitalLibMgmtService;
import com.heraizen.dhi.dhiddcms.util.TenantContext;

import com.heraizen.dhi.dhiddcms.model.Document;

@RestController
@RequestMapping("/api/v1/")
@Slf4j
public class DocumentController {

    @Autowired
    private DigitalLibMgmtService digiLibSvc;
    
    @Autowired
    private ObjectMapper objectMapper;

    private Document getDocument(MultipartFile multipartFile, String metadataStr) {
        try {
            Path tempFile = Files.createTempFile("dlib", "tmp");
            String fileName = multipartFile.getOriginalFilename();
            String mimeType = multipartFile.getContentType();
            Metadata metadata = objectMapper.readValue(metadataStr, Metadata.class);
            log.debug("Extracting document fileName: {} \n mimeType: {} \n encoding : UTF_8 \n MetaData : {} \n ", fileName, mimeType, metadata);
            multipartFile.transferTo(tempFile);
            return Document.builder()
                    .file(tempFile.toFile())
                    .name(fileName)
                    .mimeType(multipartFile.getContentType())
                    .encoding("UTF_8")
                    .metadata(metadata)
                    .build();
        } catch (JsonProcessingException jpe ) {
            log.error("Error while parsing metadata", jpe.getMessage());
            log.debug("Stack trace :", jpe);
        } catch (IOException ie) {
            log.error("Error while reading Multi part file :", ie.getMessage());
            log.debug("Stack trace :", ie);
        }
        return null;
    }

    @PostMapping(value = "{tenant}/save", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> save(@PathVariable String tenant, @RequestPart("file") MultipartFile multipartFile,
            @RequestPart("metadata") String metadata) {
        TenantContext.setTenant(tenant);
        Document doc = getDocument(multipartFile, metadata);
        if ( Objects.isNull(doc)) {
            return new ResponseEntity<>("Could not Read the Document OR MetaData of Request", 
                    HttpStatus.BAD_REQUEST);
        }
        digiLibSvc.saveDoc(doc);
        return new ResponseEntity<>("Document successfull Uploaded", HttpStatus.CREATED);
    }

    @GetMapping("{tenant}/dumpws")
    public ResponseEntity<?> dumpWorkspace(@PathVariable String tenant) {
        log.debug("Invoked dump ws for tenant {}", tenant);
        TenantContext.setTenant(tenant);
        log.debug("Getting Tenant as {}", TenantContext.getCurrentTenant());
        digiLibSvc.dumpWorkspace();
        return new ResponseEntity<>("Done", HttpStatus.OK);
    }
    
    @GetMapping("metadata/{docname}")
    public ResponseEntity<?> getDocMetadata(@PathVariable String docname) {
        return new ResponseEntity<>("Not Yet Implemented", HttpStatus.BAD_REQUEST);
    }

    @GetMapping("{docid}")
    public ResponseEntity<?> getDoc(@PathVariable String docid) {
        return new ResponseEntity<>("Not Yet Implemented", HttpStatus.BAD_REQUEST);
    }

    @PutMapping("{docid}/")
    public ResponseEntity<?> updateMetadata(@PathVariable String docid, @RequestBody Metadata metadata) {
        return new ResponseEntity<>("Not Yet Implemented", HttpStatus.BAD_REQUEST);
    }

    @GetMapping("search/{searchkeyword}")
    public ResponseEntity<?> search(@PathVariable String searchkeyword) {
        return new ResponseEntity<>("Not Yet Implemented", HttpStatus.BAD_REQUEST);
    }
}
