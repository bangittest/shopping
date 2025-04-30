package com.ecom.util;

import java.io.UnsupportedEncodingException;
import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.ecom.model.ProductOrder;
import com.ecom.model.UserDtls;
import com.ecom.service.UserService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class CommonUtil {

	@Autowired
	private JavaMailSender mailSender;
	
	@Autowired
	private UserService userService;

	public Boolean sendMail(String url, String reciepentEmail) throws UnsupportedEncodingException, MessagingException {

		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);

		helper.setFrom("thao55@gmail.com", "Shopping Cart");
		helper.setTo(reciepentEmail);

		String content = "<p>Hello,</p>" + "<p>Bạn đã yêu cầu đặt lại mật khẩu.</p>"
				+ "<p>Nhấp vào liên kết bên dưới để thay đổi mật khẩu của bạn:</p>" + "<p><a href=\"" + url
				+ "\">Thay đổi mật khẩu của bạn</a></p>";
		helper.setSubject("Đặt lại mật khẩu");
		helper.setText(content, true);
		mailSender.send(message);
		return true;
	}

	public static String generateUrl(HttpServletRequest request) {

		// http://localhost:8080/forgot-password
		String siteUrl = request.getRequestURL().toString();

		return siteUrl.replace(request.getServletPath(), "");
	}
	
	String msg=null;
	
	public Boolean sendMailForProductOrder(ProductOrder order,String status) throws Exception
	{

		msg = """
<style>
    .email-container {
        font-family: Arial, sans-serif;
        padding: 20px;
        color: #333;
        line-height: 1.6;
        background-color: #f9f9f9;
    }
    .email-header {
        background-color: #f05e8a;
        color: white;
        padding: 15px;
        text-align: center;
        font-size: 20px;
        border-radius: 5px 5px 0 0;
    }
    .email-body {
        background-color: white;
        padding: 20px;
        border: 1px solid #ddd;
        border-top: none;
        border-radius: 0 0 5px 5px;
    }
    .email-body p {
        margin: 8px 0;
    }
    .product-info {
        margin-top: 15px;
        padding: 10px;
        background-color: #f0f0f0;
        border-radius: 5px;
    }
    .footer {
        margin-top: 20px;
        font-size: 12px;
        color: #777;
        text-align: center;
    }
</style>

<div class='email-container'>
    <div class='email-header'>
        Shopping Cart - Order Confirmation
    </div>
    <div class='email-body'>
        <p>Hello <b>[[name]]</b>,</p>
        <p>Thank you for your order. Your order status is: <b>[[orderStatus]]</b>.</p>

        <div class='product-info'>
            <p><b>Product Name:</b> [[productName]]</p>
            <p><b>Category:</b> [[category]]</p>
            <p><b>Quantity:</b> [[quantity]]</p>
            <p><b>Price:</b> [[price]]</p>
            <p><b>Payment Type:</b> [[paymentType]]</p>
        </div>

        <p>If you have any questions, please contact our support team.</p>
    </div>
    <div class='footer'>
        &copy; 2025 Shopping Cart. All rights reserved.
    </div>
</div>
""";


		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);

		helper.setFrom("thao@gmail.com", "Shooping Cart");
		helper.setTo(order.getOrderAddress().getEmail());

		msg=msg.replace("[[name]]",order.getOrderAddress().getFirstName());
		msg=msg.replace("[[orderStatus]]",status);
		msg=msg.replace("[[productName]]", order.getProduct().getTitle());
		msg=msg.replace("[[category]]", order.getProduct().getCategory());
		msg=msg.replace("[[quantity]]", order.getQuantity().toString());
		msg=msg.replace("[[price]]", order.getPrice().toString());
		msg=msg.replace("[[paymentType]]", order.getPaymentType());
		
		helper.setSubject("Product Order Status");
		helper.setText(msg, true);
		mailSender.send(message);
		return true;
	}
	
	public UserDtls getLoggedInUserDetails(Principal p) {
		String email = p.getName();
		UserDtls userDtls = userService.getUserByEmail(email);
		return userDtls;
	}
	

}
