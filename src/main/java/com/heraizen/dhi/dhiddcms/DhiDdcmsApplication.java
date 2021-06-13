package com.heraizen.dhi.dhiddcms;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.heraizen.dhi.dhiddcms.util.YamlReaderUtil;

@SpringBootApplication
public class DhiDdcmsApplication{

	@Autowired
	private YamlReaderUtil yamlReaderUtil;
	
	public static void main(String[] args) {
			SpringApplication.run(DhiDdcmsApplication.class, args);
	}
	
	@Bean
	public CommandLineRunner runner() {
		return (String... args)->{
			System.out.println(yamlReaderUtil.getTenantDetails().size());
		};
	}

}

