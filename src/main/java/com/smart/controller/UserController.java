package com.smart.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entites.Contact;
import com.smart.entites.User;
import com.smart.helper.Message;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ContactRepository contactRepository;
	
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
	public String processContact(@ModelAttribute Contact contact, 
			@RequestParam("profileImage") MultipartFile file, 
			Principal principal,
			HttpSession session) {
		
		try {
			String name = principal.getName();
			User user = this.userRepository.getUserByUserName(name);
			
//			if(3 > 2) {
//				throw new Exception();
//			}
			
			// processing and Uploading File
			
			if(file.isEmpty()) {
				System.out.println("File is Empty");
				contact.setImage("contact.png");
			}else {
				// Upload the file to the folder and update tha name to contact
				
				contact.setImage(file.getOriginalFilename());
				File saveFile = new ClassPathResource("static/img").getFile();
				
				Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			
				System.out.println("Image is Uploaded Successfully");
			}
			
			
			
			contact.setUser(user);
			user.getContacts().add(contact);
			
			this.userRepository.save(user);
			
			System.out.println("Data "+contact);
			
			System.out.println("Added To Data base");
			
			// message Success 
			session.setAttribute("message", new Message("Your Contact is added !! Add More", "success"));
			
		}catch(Exception e) {
			System.out.println("ERROR "+e.getMessage());
			e.printStackTrace();
			
			// error Message 
			session.setAttribute("message", new Message("Something Went wrong ! Try Again ...", "danger"));
			
		}
		
		
		return "normal/add_contact_form";
	}
	
	// Show Contact Handler
	// per Page = 5 contact (n)
	// Current page = 0 (page)
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page ,Model m, Principal principal) {
		
		m.addAttribute("title", "Show User Contacts");
		
		// Contact ki list ko bhejni hai 
		String userName = principal.getName();
		
		User user = this.userRepository.getUserByUserName(userName);
		// per Page = 5 contact (n)
		// Current page = 0 (page)
		Pageable pageable = PageRequest.of(page, 5);
		
		Page<Contact> contacts = this.contactRepository.findContactsByUser(user.getId(), pageable);
		m.addAttribute("contacts", contacts);
		m.addAttribute("currentPage", page);
		m.addAttribute("totalPages", contacts.getTotalPages());
		return "normal/show_contacts";
	}
	
	// Showing Particuler Contact Detail
	
	@GetMapping("/{cId}/contact")
	public String showContactDetail(@PathVariable("cId") Integer cId, 
								Model model, Principal principal) {
		
		System.out.println("CId "+cId);
		
		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();
		
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		
		if(user.getId() == contact.getUser().getId()) {
			model.addAttribute("contact", contact);
			model.addAttribute("title", contact.getName());
		}
				return "normal/contact_detail";
		
	}
	
	// Delete Contact Handler
	@GetMapping("/delete/{cid}")
	public String deleteContact(@PathVariable("cid") Integer cId, Model model,
						Principal principal, HttpSession session) throws IOException {
		
		System.out.println("CID "+cId);
		Contact contact = this.contactRepository.findById(cId).get();
		
		System.out.println("Contact is "+contact.getUser());
		System.out.println("Contact  User ID is "+contact.getUser().getId());
		
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		int contactUserId = contact.getUser().getId();
		
		//contact.setUser(null);
		
		System.out.println("User ID is "+user.getId());
		
		user.getContacts().remove(contact);
		
		this.userRepository.save(user);
		
		//check
//		
		int userId = user.getId();
		File saveFile = new ClassPathResource("static/img").getFile();
		Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+contact.getImage());
		
		Files.delete(path); 

		
		session.setAttribute("message", new Message("Contact Deleted Successfully...", "success"));
		
		return "redirect:/user/show-contacts/0";
	}
	
	// open Update Form Handler
	@PostMapping("/update-contact/{cid}")
	public String updateForm(@PathVariable("cid") Integer cid, Model m) {
		
		m.addAttribute("title", "Update Contact Form");
		
		Contact contact = this.contactRepository.findById(cid).get();
		
		m.addAttribute("contact", contact);
		return "normal/update_form";
	}
	
	// Update Contact Handler
	
	@PostMapping("/process-update")
	public String updateHandler(@ModelAttribute Contact contact, 
						@RequestParam("profileImage") MultipartFile file,
						Model m, HttpSession session,
						Principal principal) {
		
		try {
			
			// old contact detail
			
			Contact oldContactDetail = this.contactRepository.findById(contact.getcId()).get();
			
			if(!file.isEmpty()) {
				// File reWrite
				
				// delete old photo
				File deleteFile = new ClassPathResource("static/img").getFile();
				File file1 = new File(deleteFile, oldContactDetail.getImage());
				file1.delete();
				// update new photo
				
				File saveFile = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				
				contact.setImage(file.getOriginalFilename());
				
			}else {
				contact.setImage(oldContactDetail.getImage());
				
			}
			User user = this.userRepository.getUserByUserName(principal.getName());
			contact.setUser(user);
			this.contactRepository.save(contact);
			
			session.setAttribute("message", new Message("Your Contact is Updated..", "success"));
		}catch(Exception e) {
			e.printStackTrace();
		}
		System.out.println("CONTACT NAME "+contact.getName());
		System.out.println("CONTACT ID "+contact.getcId());
		
		return "redirect:/user/"+contact.getcId()+"/contact";
	}
	
	
	// Your Profile 
	
	@GetMapping("/profile")
	public String yourProfile(Model model) {
		
		model.addAttribute("title", "Profile Image");
		return "normal/profile";
	}
	
}
