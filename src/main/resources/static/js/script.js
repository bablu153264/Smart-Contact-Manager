console.log("this is script file")


const toggleSidebar=()=>{
	
	if($('.sidebar').is(":visible")){
		
		//true (closing the sidebar)
		
		$(".sidebar").css("display","none");
		$(".content").css("margin-left","0%");
		
		
	}else{
		//false (showing the sidebar)
		$(".sidebar").css("display","block");
		$(".content").css("margin-left","20%");
		
	}
	
};

const search = () => {
	/*console.log("searching....");*/
	
	let query=$("#search-input").val();
	
	if(query==''){
		
		$(".search-result").hide();
		
	}else{
		//search
		//sesnding request to server
		
		let url= `http://localhost:1909/search/${query}`;
		
		fetch(url).then((response) =>{
			
			return response.json();
			
		}).then((data) => {
			
			//accessing data...
			let text=`<div class='list-group'>`;
			
			data.forEach((contact) => {
 text += `<a href='/user/${contact.cid}/contact' class='list-group-item list-group-item-action d-flex align-items-center'>
                <img src='/image/${contact.image}' alt='${contact.name}' class='mr-2' style='max-width: 20px; max-height: 30px; object-fit: cover; border-radius: 75%;'>
                ${contact.name}
            </a>`;
			})
			
			
			text += `</div>`;
			
			$(".search-result").html(text);
			$(".search-result").show();
		});
		
	}
	
};




/*payment gateway*/
/*first request to server to create order*/

const paymentStart=() => {
	
	console.log("payment started");
	
	let amount=$("#payment_field").val();
	console.log(amount);
	
	if(amount=="" || amount==null){
		
		swal("Failed","amount is required","error");
		return;
	}
	
	
	//code
	//we will use ajax to send request to server to create order
	
	
	$.ajax({
		
		url:'/user/create_order',
		data:JSON.stringify({amount:amount,info:'order_request'}),
		contentType:'application/json',
		type:'POST',
		dataType:'json',
		success:function(response){
			
			//invoked when success
			console.log(response)
			if(response.status == "created"){
				
				//open payment form
				let options = {
					
					key:'rzp_test_jALStMrdk5bDvE',
					amount:response.amount,
					currency:'INR',
					name:'Smart Contact Manager',
					description:'Donation',
					image:"https://cdn.iconscout.com/icon/premium/png-256-thumb/contact-management-4883312-4062125.png",
					order_id:response.id,
					handler:function(response){
						console.log(response.razorpay_payment_id);
						console.log(response.razorpay_order_id);
						console.log(response.razorpay_signature);
						console.log("Payment Successful");
						
						updatePaymentOnServer(response.razorpay_payment_id,response.razorpay_order_id,"paid")
						swal("Good job!","congrats !! payemnt successfull","success");
					},
					
					prefill:{
						name:"",
						email:"",
						contact:"",
					},
					
					notes:{
						
						address:"SpringProject",
						
					},
					
					theme:{
						color:"#3399cc",
					},
				};
				
				let rzp = new Razorpay(options);
				
				rzp.on("payment.failed", function (response) {
					console.log(response.error.code);
					console.log(response.error.description);
					console.log(response.error.souce);
					console.log(response.error.step);
					console.log(response.error.reason);
					console.log(response.error.metadata.order_id);
					console.log(response.error.metadata.payment_id);
					alert("Oops Payment failed!!");
					swal("Failed","Oops Payment failed!!","error");
					
				});
				
				rzp.open();
			}
			
		},
		error:function(error){
			
			//invoked when error
			console.log(error)
			alert("something went wrong !!")
		}
		
	})
	
};


function updatePaymentOnServer(payment_id,order_id,status)
{
	$.ajax({
		
		url:'/user/update_order',
		data:JSON.stringify({payment_id:payment_id,order_id:order_id,status: status,}),
		contentType:'application/json',
		type:'POST',
		dataType:'json',
		success: function(response){
			swal("Good job!","congrates !! payment successful !!","success");
		},
		error:function(error){
			swal("Failed !!","Your payment is successful, but we did not get on server, we will contact you as soon as posssible", "error");
		}
		});
}

