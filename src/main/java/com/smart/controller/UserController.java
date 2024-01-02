package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Map;
import java.util.Optional;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.smart.dao.ContactRepository;
import com.smart.dao.MyOrderRepository;
import com.smart.dao.UserRepository;
import com.smart.entiy.Contact;
import com.smart.entiy.MyOrder;
import com.smart.entiy.User;
import com.smart.helper.Message;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ContactRepository contactRepository;
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	
	@Autowired
	private MyOrderRepository myOrderRepository;
	
	//method for adding common data to response
	@ModelAttribute
	public void addCommonData(Model model,Principal principal) {
		
		String userName = principal.getName();
		System.out.println("USERNAME " + userName);
		
		//get the user using Username(Email)
		User user = userRepository.getUserByUserName(userName);
		
		System.out.println("USER " + user);
		
		model.addAttribute("user",user);
		
	}
	
	
	//dashboard home after login 
	@RequestMapping("/index")
	public String dashboard(Model model,Principal principal) {
		model.addAttribute("title","User Dashboard");
		return "normal/user_dashboard";
	}
	
	
	//open add form handler
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {
		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());
		return "normal/add_contact_form";
	}
	
	
	//processing add contact form
	@PostMapping("/process-contact")
	public String processContact(
			@ModelAttribute Contact contact,
			@RequestParam("profileImage") MultipartFile file ,
			Principal principal,HttpSession session) 
	{
		try {
		String name = principal.getName();
		User user = this.userRepository.getUserByUserName(name);

		//processing and uploading file
		
		if(file.isEmpty()) {
			
			//if the file is empty
			System.out.println("Image is empty");
			contact.setImage("contact.png");
			
			
		}else {
			//upload the file to folder and update the name to contact database
			
			contact.setImage(file.getOriginalFilename());
			
			File saveFile = new ClassPathResource("static/image").getFile();
			
			Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
			
			Files.copy(file.getInputStream(), path,StandardCopyOption.REPLACE_EXISTING);
			System.out.println("Image is Uploaded");
			
		}
		
		user.getContacts().add(contact);
		contact.setUser(user);
		
		this.userRepository.save(user);
		
		System.out.println("DATA" + contact);
		System.out.println("Added to data base");
		
		//message success
		
		session.setAttribute("message", new Message("Your contact is added!! Add more...","success"));
		
		}catch(Exception e) {
			e.printStackTrace();
			//error message
			session.setAttribute("message", new Message("Something went wrong! Try again","danger"));
		}
		
		return "normal/add_contact_form";
	}
	
	
	//show contact handler
	//per page show only 5[n] contacts
	//current page =0[page]
	
	@GetMapping("/show-contacts/{page}")
	public String shoContacts(@PathVariable("page") Integer page,Model model,Principal principal) {
		
		model.addAttribute("title","Contacts Page");
		
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		Pageable pageable = PageRequest.of(page, 3);
		
		Page<Contact> contacts = this.contactRepository.findContactByUser(user.getId(),pageable);
		
		model.addAttribute("contacts",contacts);
		model.addAttribute("currentPage",page);
		model.addAttribute("totalPages",contacts.getTotalPages());
		
		
		
		return "normal/show_contacts";
	}
	
	
	
	//handler for showing particular contact deatils of the login user
	
	@RequestMapping("/{cid}/contact")
	public String showContactDetail(@PathVariable("cid") Integer cid,Model model,Principal principal) {
		
		Optional<Contact> contactOptional = this.contactRepository.findById(cid);
		Contact contact = contactOptional.get();		
		
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		if(user.getId()==contact.getUser().getId()) {			
			model.addAttribute("contact",contact);
			model.addAttribute("title",contact.getName());
		}
		
		
		return "normal/contact_detail";
	}
	
	
	
	//delete contact handler
	
	@GetMapping("/delete/{cid}")
	public String deleteContact(@PathVariable("cid") Integer cid,Model model,Principal principal,HttpSession session) {
		
		Contact contact = this.contactRepository.findById(cid).get();
		
		
		//check the contact
		User user = this.userRepository.getUserByUserName(principal.getName());
		
		if(user.getId()==contact.getUser().getId()) {
			
			// contact.setUser(null);
			/* this.contactRepository.delete(contact); */
			
			
			user.getContacts().remove(contact);
			this.userRepository.save(user);
			
			
			session.setAttribute("message", new Message("contact deleted successfully", "success"));
			
		}
		
		return "redirect:/user/show-contacts/0";
	}
	
	
	// open update form handler for the contacts
	
	@PostMapping("/update-contact/{cid}")
	public String updateForm(@PathVariable("cid") Integer cid,Model model) {
		
		model.addAttribute("title","Update Contact");
		
		Contact contact = this.contactRepository.findById(cid).get();
		model.addAttribute("contact",contact);
		return "normal/update_form";
	}
	

	
//	update contact handler
	
	@PostMapping("/process-update")
	public String updateHandler(@ModelAttribute Contact contact,@RequestParam("profileImage") MultipartFile file,Model model,HttpSession session,Principal principal) {
		
		try {
			
			//old contact details for old photo name 
			Contact oldConatctDetails = this.contactRepository.findById(contact.getCid()).get();
			
			//checking image
			if(!file.isEmpty()) {
				//file work..
				//rewrite the file
				
				// 1)delete old photo
				
				File deleteFile = new ClassPathResource("static/image").getFile();
				File file1= new File(deleteFile,oldConatctDetails.getImage());
				file1.delete();
				
				
				
				
				// 2)update new photo
				File saveFile = new ClassPathResource("static/image").getFile();
				
				Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				
				Files.copy(file.getInputStream(), path,StandardCopyOption.REPLACE_EXISTING);
				
				contact.setImage(file.getOriginalFilename());
				
			}else {
				contact.setImage(oldConatctDetails.getImage());
			}
			
			User user = this.userRepository.getUserByUserName(principal.getName());
			
			contact.setUser(user);
			
			this.contactRepository.save(contact);
			
			session.setAttribute("message", new Message("Your contact is updated","success"));
			
			
		} catch (Exception e) {
			e.printStackTrace();		
			}
		
		return "redirect:/user/"+contact.getCid()+"/contact";
	}
	
	
	
	//profile handler
	@GetMapping("/profile")
	public String yourProfile(Model model) {
		
		model.addAttribute("title","Profile Page");
		return "normal/profile";
	}
	
	//open update form handler for the user
	@PostMapping("/update-user")
	public String updateUserForm(Model model) {
		
		model.addAttribute("title","Update user");
		return "normal/update_user_form";
	}
	
//	update user handler
	@PostMapping("/process-user-update")
	public String updateUserHandler(@ModelAttribute User user,@RequestParam("profileImage") MultipartFile file,Model model,Principal principal,HttpSession session) {
		
		User oldUserDetails = this.userRepository.findById(user.getId()).get();
		try {
			// 1)delete old photo
			if(!file.isEmpty()) {
				File deleteFile = new ClassPathResource("static/image").getFile();
				File file1 = new File(deleteFile,oldUserDetails.getImageUrl());
				file1.delete();
				
			// 2)update new photo
				
				File saveFile = new ClassPathResource("static/image").getFile();
				
				Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				
				Files.copy(file.getInputStream(), path,StandardCopyOption.REPLACE_EXISTING);
				
				user.setImageUrl(file.getOriginalFilename());
			}else {
				user.setImageUrl(oldUserDetails.getImageUrl());
			}
			
			
			this.userRepository.save(user);
			
			session.setAttribute("message", new Message("Your profile is updated","success"));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return "redirect:/user/profile";
	}
	
	
	//open settings handler
	
	@GetMapping("/settings")
	public String openSettings(Model model) {
		
		model.addAttribute("title","Settings Page");
		return "normal/settings";
	}
	
	//change userName
	
	@PostMapping("/change-userName")
	public String changeUserName(@ModelAttribute User user,@RequestParam("olduserName") String olduserName,@RequestParam("newuserName") String newuserName,Principal principal,HttpSession session) {
		
		String UserName = principal.getName();
		User currentuser = this.userRepository.getUserByUserName(UserName);
		
		if(this.bCryptPasswordEncoder.matches(olduserName,currentuser.getPassword())) {
			
			currentuser.setEmail(newuserName);
			this.userRepository.save(currentuser);
			session.setAttribute("message", new Message("Your userName has been chnaged","success"));
		}else {
			session.setAttribute("message", new Message("Please enter correct old userName!!","danger"));
		}
		
		return "redirect:/user/settings";

		
	}
	
	
	//change password handler
	
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldPassword,@RequestParam("newPassword") String newPassword,Principal principal,HttpSession session) {
		
		String UserName = principal.getName();
		User currentuser = this.userRepository.getUserByUserName(UserName);
	
		
		
		if(this.bCryptPasswordEncoder.matches(oldPassword,currentuser.getPassword())) {
			
			//change the password
			
			currentuser.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
			this.userRepository.save(currentuser);
			session.setAttribute("message", new Message("Your Password has been chnaged","success"));
			
		}else {
			
			//error....
			session.setAttribute("message", new Message("Please enter correct old password!!","danger"));
		}
		
		
		return "redirect:/user/settings";
	}
	
	
	
	//creating order for payment
	@PostMapping("/create_order")
	@ResponseBody
	public String createOrder(@RequestBody Map<String, Object> data,Principal principal) throws Exception {
		
		System.out.println(data);
		
		int amt = Integer.parseInt(data.get("amount").toString());
		
		var client = new RazorpayClient("rzp_test_jALStMrdk5bDvE", "WG77Hy85qDq7eplGLQNQXXy6");
		
		JSONObject ob = new JSONObject();
		ob.put("amount", amt*100);
		ob.put("currency", "INR");
		ob.put("receipt", "txn_123456");
		
		
		
		//creating new order
		Order order = client.orders.create(ob);
		System.out.println(order);
		
		
		//saving the order to the database 
		MyOrder myOrder = new MyOrder();
		myOrder.setAmount(order.get("amount")+"");
		myOrder.setMyOrderId(order.get("order_id"));
		myOrder.setPaymentId(null);
		myOrder.setStatus("created");
		myOrder.setUser(this.userRepository.getUserByUserName(principal.getName()));
		myOrder.setReceipt(order.get("receipt"));
		
		this.myOrderRepository.save(myOrder);
		
		
		return order.toString();
	}
	
	@PostMapping("/update_order")
	public ResponseEntity<?> updateOrder(@RequestBody Map<String, Object> data){
		
		MyOrder myOrder = this.myOrderRepository.findByOrderId(data.get("order_id").toString());
		myOrder.setPaymentId(data.get("payment_id").toString());
		myOrder.setStatus(data.get("status").toString());
		
		this.myOrderRepository.save(myOrder);
		
		return ResponseEntity.ok(Map.of("msg","updated"));
	}
	
}

