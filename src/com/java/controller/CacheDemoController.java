package com.java.controller;

import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CacheDemoController {

	@RequestMapping("/caching")
	public String data(HttpServletResponse response, Model model) {
	
		/*
		 * Server sends  Expires: <expiration-data> header with the response. 
		 * Here the expiration data has been set to one hour from the time the response was sent out.
		 */
		System.out.println("In controller");
		//10 min
		long expires= System.currentTimeMillis()+600000;
		//Will expire after diff in these 2 headers
		response.setDateHeader("Expires", expires);
		response.setDateHeader("Date", System.currentTimeMillis());
		model.addAttribute("message", "From caching controller"+ LocalDateTime.now());
		return "data";
	}
}
