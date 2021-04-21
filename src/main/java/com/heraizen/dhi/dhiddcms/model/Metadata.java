package com.heraizen.dhi.dhiddcms.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Metadata {

	private String barCode;
	private String isbn;
	private String authorName;
	private String summary;
	private List<String> tags;

}
