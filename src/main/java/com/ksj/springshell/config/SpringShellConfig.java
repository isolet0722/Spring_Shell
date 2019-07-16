package com.ksj.springshell.config;

import org.jline.terminal.Terminal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import com.ksj.springshell.ShellHelper;

@Configuration
public class SpringShellConfig {
	
	@Bean
	public ShellHelper shellHelper(@Lazy Terminal terminal) {
		return new ShellHelper(terminal);
	}
    
}
