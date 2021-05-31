package com.heraizen.dhi.dhiddcms.model;

import java.util.HashSet;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Metadata {

    private String title;
    private String volume;
    private String publisher;
    private String barCode;
    private String isbn;
    private Set<String> authorNames = new HashSet<>();
    private String summary;
    private Set<String> tags = new HashSet<>();
}
