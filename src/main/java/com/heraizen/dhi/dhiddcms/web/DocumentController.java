package com.heraizen.dhi.dhiddcms.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Optional;
import java.util.Set;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;

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
    
    @GetMapping("{tenant}/metadata/{docname}")
    public ResponseEntity<?> getDocMetadata(@PathVariable String tenant, @PathVariable String docname) {
        log.debug("Invoked getDocMetaData for tenant {} and doc name {}", tenant, docname);
        TenantContext.setTenant(tenant);
        Optional<Metadata> md = digiLibSvc.getDocumentMetadata(docname);
        if ( md.isPresent() ) {
            return new ResponseEntity<>(md.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Document by name "+ docname + "Not found for Tenant " + tenant, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("{tenant}/{docname}")
    public ResponseEntity<?> getDoc(@PathVariable String tenant, @PathVariable String docname) throws FileNotFoundException {
        log.debug("Invoked getDocMetaData for tenant {} and doc name {}", tenant, docname);
        TenantContext.setTenant(tenant);
        Optional<Document> doc = digiLibSvc.getDocument(docname);
        log.info("Returning Document {}", doc);
        if (doc.isPresent()) {
            try {
                Document d = doc.get();
                log.debug("Mime type : {}", d.getMimeType());
                MediaType mt = MediaType.valueOf(d.getMimeType());
                log.debug("Media Type : {}", mt);
                InputStreamResource resource = new InputStreamResource(new FileInputStream(doc.get().getFile()));
                return ResponseEntity.ok()
                // Content-Disposition
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + d.getName())
                    // Content-Type
                    .contentType(mt)
                    // Contet-Length
                    .contentLength(d.getFile().length()) //
                    .body(resource);
            }catch ( FileNotFoundException fe) {
                log.error("Error while sending the Document ", fe);
                return new ResponseEntity<>("Document by name " + docname + " for tenant " + tenant + " Not found" , HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity<>("Document by name " + docname + " for tenant " + tenant + " Not found" , HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("{docid}/")
    public ResponseEntity<?> updateMetadata(@PathVariable String docid, @RequestBody Metadata metadata) {
        return new ResponseEntity<>("Not Yet Implemented", HttpStatus.BAD_REQUEST);
    }

    @GetMapping("{tenant}/search/{searchkeyword}")
    public ResponseEntity<?> search(@PathVariable String tenant, @PathVariable String searchkeyword) {
        log.debug("Invoked search for tenant {} and search string {}", tenant, searchkeyword);
        TenantContext.setTenant(tenant);
        Set<String> docNames = digiLibSvc.search(searchkeyword);
        return new ResponseEntity<>(docNames, HttpStatus.OK);
    }
}
