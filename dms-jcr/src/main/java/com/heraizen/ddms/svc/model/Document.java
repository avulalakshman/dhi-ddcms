/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heraizen.ddms.svc.model;

import java.io.File;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 *
 * @author Pradeepkm
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class Document {
    private String id;
    private String name;
    private File file;
    private String mimeType;
    private String encoding;
    private Metadata metadata;
}
