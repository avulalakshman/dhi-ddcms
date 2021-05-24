package com.heraizen.dhi.dhiddcms.model;

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

	private String barCode;
	private String isbn;
	private Set<String> authorNames;
	private String summary;
	private Set<String> tags;

}
