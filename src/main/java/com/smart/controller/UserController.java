package com.smart.controller;

import java.security.Principal;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.smart.dao.UserRepository;
import com.smart.entites.Contact;
import com.smart.entites.User;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private UserRepository userRepository;
	
	// Method for adding common data 
	
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String userName = principal.getName();
		System.out.println("Username "+userName);
		// Get the user using UserName(Email)
		User user = userRepository.getUserByUserName(userName);
		System.out.println("USER "+user);
		
		model.addAttribute("user", user);
	}
	
	
	// Dashboard Home
	
	@RequestMapping("/index")
	public String dashboard(Model model, Principal principal) {
	
		return "normal/user_dashboard";
	}
	
	// open add Form Handler
	
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {
		
		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());
		return "normal/add_contact_form";
	}
	
	// processing  add Contact Form
	
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact, Principal principal) {
		String name = principal.getName();
		User user = this.userRepository.getUserByUserName(name);
		contact.setUser(user);
		user.getContacts().add(contact);
		this.userRepository.save(user);
		
		System.out.println("Data "+contact);
		
		System.out.println("Added To Data base");
		return "normal/add_contact_form";
	}
}
