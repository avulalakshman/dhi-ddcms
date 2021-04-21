package com.heraizen.dhi.dhiddcms.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Document {


	private String id;
	private byte file[];
	private String name;
	private Metadata metadata;

}
