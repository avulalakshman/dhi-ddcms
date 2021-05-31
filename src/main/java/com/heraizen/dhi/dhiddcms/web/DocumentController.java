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
import java.util.Set;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/")
@Slf4j
public class DocumentController {

    @Autowired
    private DigitalLibMgmtService digiLibSvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Document toDocument(MultipartFile multipartFile, String metadataStr) {
        try {
            Path tempFile = Files.createTempFile("dlib", "tmp");
            String fileName = multipartFile.getOriginalFilename();
            String mimeType = multipartFile.getContentType();
            String encoding = "UTF-8";
            Metadata metadata = objectMapper.readValue(metadataStr, Metadata.class);
            log.debug("Extracting document fileName: {} \n mimeType: {} \n encoding : {} \n MetaData : {} \n ", fileName, mimeType, encoding, metadata);
            multipartFile.transferTo(tempFile);
            return Document.builder()
                    .file(tempFile.toFile())
                    .name(fileName)
                    .mimeType(mimeType)
                    .encoding(encoding)
                    .metadata(metadata)
                    .build();
        } catch (JsonProcessingException jpe) {
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
        Document doc = toDocument(multipartFile, metadata);
        if (Objects.isNull(doc)) {
            return new ResponseEntity<>("Could not read the Document OR MetaData of Request",
                    HttpStatus.BAD_REQUEST);
        }
        digiLibSvc.saveDoc(doc);
        return new ResponseEntity<>(String.format("Document %s successfully Uploaded", doc.getName()),
                HttpStatus.CREATED);
    }

    @GetMapping("{tenant}/metadata/{docname}")
    public ResponseEntity<?> getDocMetadata(@PathVariable String tenant, @PathVariable String docname) {
        log.debug("Invoked getDocMetaData for tenant {} and doc name {}", tenant, docname);
        TenantContext.setTenant(tenant);
        return digiLibSvc.getDocumentMetadata(docname)
                .map(md->new ResponseEntity<>(md, HttpStatus.OK))
                .orElseGet(()->(ResponseEntity)new ResponseEntity<>("Document %s not found", 
                        HttpStatus.NOT_FOUND));
    }

    @GetMapping("{tenant}/{docname}")
    public ResponseEntity<?> getDoc(@PathVariable String tenant, @PathVariable String docname) {
        log.debug("Invoked getDocMetaData for tenant {} and doc name {}", tenant, docname);
        TenantContext.setTenant(tenant);
        return digiLibSvc.getDocument(docname).map(d -> {
            log.info("Found Document {} in Digi Lib...Returning", d.getName());
            log.debug("Mime type : {}", d.getMimeType());
            MediaType mt = MediaType.valueOf(d.getMimeType());
            log.debug("Media Type : {}", mt);
            ResponseEntity<?> retEntity;
            try (FileInputStream fileStream = new FileInputStream(d.getFile())) {
                InputStreamResource resource = new InputStreamResource(fileStream);
                retEntity = ResponseEntity.ok()
                        // Content-Disposition
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + d.getName())
                        // Content-Type
                        .contentType(mt)
                        // Contet-Length
                        .contentLength(d.getFile().length()) //
                        .body(resource);
            } catch (IOException ex) {
                String errMsg = String.format("Something wrong with IO while returning document %s error: %s",
                        docname, ex.getMessage());
                log.error(errMsg);
                log.debug("Stack trace:", ex);
                retEntity = new ResponseEntity<>(errMsg, HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return retEntity;
        }).orElseGet(() -> (ResponseEntity) new ResponseEntity<>(String.format("Document by name %s not found"
                + " for tenant %s", docname, tenant),
                HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{tenant}/update_metadata/{docname}")
    public ResponseEntity<?> updateMetadata(@PathVariable String tenant, @PathVariable String docname, @RequestBody Metadata metadata) {
        TenantContext.setTenant(tenant);
        return digiLibSvc.updateDocumentMetadata(docname, metadata)
                .map(md -> new ResponseEntity<>(String.format("Updated %s Metada with %s", docname, md),
                HttpStatus.ACCEPTED))
                .orElse(new ResponseEntity<>(String.format("Something is wrong, "
                        + "could not update the doc %s metadata %s...", docname, metadata),
                        HttpStatus.BAD_REQUEST));
    }

    @GetMapping("{tenant}/search")
    public ResponseEntity<?> search(@PathVariable String tenant, @RequestParam String searchStr) {
        log.debug("Invoked search for tenant {} and search string {}", tenant, searchStr);
        TenantContext.setTenant(tenant);
        Set<String> docNames = digiLibSvc.search(searchStr);
        return new ResponseEntity<>(docNames, HttpStatus.OK);
    }

    @PostMapping("{tenant}/deletedoc/{docname}")
    public ResponseEntity<?> deleteDoc(@PathVariable String tenant, @PathVariable String docname) {
        log.debug("Invoked delete document {} for tenant {}");
        TenantContext.setTenant(tenant);
        digiLibSvc.deleteDoc(docname);
        return new ResponseEntity<>(docname + " deleted for tenant " + tenant, HttpStatus.OK);
    }

    @GetMapping("{tenant}/dumpws")
    public ResponseEntity<?> dumpWorkspace(@PathVariable String tenant) {
        log.debug("Invoked dump ws for tenant {}", tenant);
        TenantContext.setTenant(tenant);
        digiLibSvc.dumpWorkspace();
        return new ResponseEntity<>("Done", HttpStatus.OK);
    }
}
