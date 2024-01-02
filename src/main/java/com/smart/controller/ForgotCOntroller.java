package com.smart.controller;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.service.EmailService;

import jakarta.servlet.http.HttpSession;

import com.smart.dao.UserRepository;
import com.smart.entiy.User;
import com.smart.helper.Message;

@Controller
public class ForgotCOntroller {
	Random random = new Random(1000);
	
	@Autowired
	private EmailService emailService;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	//email id form open handler
	@RequestMapping("/forgot")
	public String openEmailForm() {
		return "forgot_email_form";
	}
	
//	
	@PostMapping("/send-otp")
	public String sendOtp(@RequestParam("email") String email,HttpSession session ) {
		
		//generating otp of 4digit number
		int otp = random.nextInt(99999);
		
		//sending otp to email
		
		String subject="OTP From SCM";
		String message =""
				+"<div style='border:1px solid #e2e2e2; padding:20px'>"
				+"<h1>"
				+"OTP"
				+"&nbsp;"
				+"is"
				+"&nbsp;"
				+"<b>"
				+otp
				+"</n>"
				+"</h1>"
				+"</div>";
		String to=email;
		
		
		boolean flag = this.emailService.sendEmail(subject, message, to);
		
		if(flag) {
		
			session.setAttribute("myotp", otp);
			session.setAttribute("email", email);
			return "verify_otp";
		
		}else {
			
			session.setAttribute("message", "chech your email id!!");
			return "forgot_email_form";
		}
		
		
	}
	
	//verify otp
	@PostMapping("/verify-otp")
	public String verifyOTP(@RequestParam("otp") int otp,HttpSession session) {
		
		int myotp = (int) session.getAttribute("myotp");
		String email = (String)session.getAttribute("email");
		
		if(myotp==otp) {
			
			//password change form
			
			User user = this.userRepository.getUserByUserName(email);
			
			if(user==null) {
				//send error message
				
				session.setAttribute("message", "User does not exist with this email!!");
				return "forgot_email_form";
				
			}else {
				//change password form
				
			}
			
			
			return "password_change_form";
		}else {
			
			session.setAttribute("message", new Message("You have entered Wrong OTP!!","danger"));
			return "verify_otp";
		}
	}
	
	//change password in login page
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("newpassword") String newpassword,HttpSession session) {
		
		String email = (String)session.getAttribute("email");
		User user = this.userRepository.getUserByUserName(email);
		user.setPassword(this.bCryptPasswordEncoder.encode(newpassword));
		this.userRepository.save(user);
		
		return "redirect:/signin?change=password changed successfully..";
	}
	
}
