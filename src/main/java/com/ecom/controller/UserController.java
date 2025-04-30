package com.ecom.controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ecom.repository.CartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.ecom.model.Cart;
import com.ecom.model.Category;
import com.ecom.model.OrderRequest;
import com.ecom.model.ProductOrder;
import com.ecom.model.UserDtls;
import com.ecom.repository.UserRepository;
import com.ecom.service.CartService;
import com.ecom.service.CategoryService;
import com.ecom.service.OrderService;
import com.ecom.service.UserService;
import com.ecom.util.CommonUtil;
import com.ecom.util.OrderStatus;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {
	@Autowired
	private UserService userService;
	@Autowired
	private CategoryService categoryService;

	@Autowired
	private CartService cartService;

	@Autowired
	private OrderService orderService;

	@Autowired
	private CommonUtil commonUtil;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private CartRepository cartRepository;


	@GetMapping("/")
	public String home() {
		return "user/home";
	}

	@ModelAttribute
	public void getUserDetails(Principal p, Model m) {
		if (p != null) {
			String email = p.getName();
			UserDtls userDtls = userService.getUserByEmail(email);
			m.addAttribute("user", userDtls);
			Integer countCart = cartService.getCountCart(userDtls.getId());
			m.addAttribute("countCart", countCart);
		}

		List<Category> allActiveCategory = categoryService.getAllActiveCategory();
		m.addAttribute("categorys", allActiveCategory);
	}

//	@GetMapping("/addCart")
//	public String addToCart(@RequestParam Integer pid, @RequestParam Integer uid, HttpSession session) {
//		Cart saveCart = cartService.saveCart(pid, uid);
//
//		if (ObjectUtils.isEmpty(saveCart)) {
//			session.setAttribute("errorMsg", "Product add to cart failed");
//		} else {
//			session.setAttribute("succMsg", "Product added to cart");
//		}
//		return "redirect:/product/" + pid;
//	}

	@PostMapping("addCart")
	public ResponseEntity<?> addCart(@RequestParam Integer pid, @RequestParam Integer uid) {
		try {
			Cart saveCart = cartService.saveCart(pid, uid);
			return ResponseEntity.ok().build();
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error");
		}
	}


	@PostMapping("deleteCartItem/{id}")
	@ResponseBody
	public ResponseEntity<?> deleteCartItem(@PathVariable Integer id) {
		try {
			cartRepository.deleteById(id);
			return ResponseEntity.ok().build();
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Xóa thất bại");
		}
	}




	@GetMapping("/cart")
	public String loadCartPage(Principal p, Model m) {

		UserDtls user = getLoggedInUserDetails(p);
		List<Cart> carts = cartService.getCartsByUser(user.getId());
		m.addAttribute("carts", carts);
		if (carts.size() > 0) {
			Double totalOrderPrice = carts.get(carts.size() - 1).getTotalOrderPrice();
			m.addAttribute("totalOrderPrice", totalOrderPrice);
		}
		return "/user/cart";
	}

	@GetMapping("/cartQuantityUpdate")
	public String updateCartQuantity(@RequestParam String sy, @RequestParam Integer cid) {
		cartService.updateQuantity(sy, cid);
		return "redirect:/user/cart";
	}

	private UserDtls getLoggedInUserDetails(Principal p) {
		String email = p.getName();
		UserDtls userDtls = userService.getUserByEmail(email);
		return userDtls;
	}

	@GetMapping("/orders")
	public String orderPage(Principal p, Model m) {
		UserDtls user = getLoggedInUserDetails(p);
		List<Cart> carts = cartService.getCartsByUser(user.getId());
		m.addAttribute("carts", carts);
		if (carts.size() > 0) {
			Double orderPrice = carts.get(carts.size() - 1).getTotalOrderPrice();
			Double totalOrderPrice = carts.get(carts.size() - 1).getTotalOrderPrice() + 250 + 100;
			m.addAttribute("orderPrice", orderPrice);
			m.addAttribute("totalOrderPrice", totalOrderPrice);
		}
		return "/user/order";
	}

	@PostMapping("/save-order")
	public String saveOrder(@ModelAttribute OrderRequest request, Principal p) throws Exception {
		// System.out.println(request);
		UserDtls user = getLoggedInUserDetails(p);
		orderService.saveOrder(user.getId(), request);
		cartService.removeCountCart(user.getId());

		return "redirect:/user/success";
	}

	@GetMapping("/success")
	public String loadSuccess() {
		return "/user/success";
	}

	@GetMapping("/user-orders")
	public String myOrder(Model m, Principal p) {
		UserDtls loginUser = getLoggedInUserDetails(p);
		List<ProductOrder> orders = orderService.getOrdersByUser(loginUser.getId());
		m.addAttribute("orders", orders);
		return "/user/my_orders";
	}

	@GetMapping("/update-status")
	public String updateOrderStatus(@RequestParam Integer id, @RequestParam Integer st, HttpSession session) {

		OrderStatus[] values = OrderStatus.values();
		String status = null;

		for (OrderStatus orderSt : values) {
			if (orderSt.getId().equals(st)) {
				status = orderSt.getName();
			}
		}

		ProductOrder updateOrder = orderService.updateOrderStatus(id, status);
		
		try {
			commonUtil.sendMailForProductOrder(updateOrder, status);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (!ObjectUtils.isEmpty(updateOrder)) {
			session.setAttribute("succMsg", "Cập nhật trạng thái thành công");
		} else {
			session.setAttribute("errorMsg", "Cập nhật trạng thái thất bại");
		}

		return "redirect:/user/user-orders";
	}

	@GetMapping("/profile")
	public String profile() {
		return "/user/profile";
	}

	@GetMapping("/new-password")
	public String changePass() {
		return "/user/new_password";
	}

	@PostMapping("/update-profile")
	public String updateProfile(@ModelAttribute UserDtls user, @RequestParam MultipartFile img, HttpSession session) {
		UserDtls updateUserProfile = userService.updateUserProfile(user, img);
		if (ObjectUtils.isEmpty(updateUserProfile)) {
			session.setAttribute("errorMsg", "Cập nhật hồ sơ thất bại");
		} else {
			session.setAttribute("succMsg", "Cập nhật hồ sơ thành công");
		}

		return "redirect:/user/profile";
	}

	@PostMapping("/change-password")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> changePassword(
			@RequestParam String currentPassword,
			@RequestParam String newPassword,
			@RequestParam String confirmPassword,
			Principal principal) {

		Map<String, Object> response = new HashMap<>();

		try {
			// Validate đầu vào
			if (newPassword.length() < 6) {
				throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự");
			}

			if (!newPassword.equals(confirmPassword)) {
				throw new IllegalArgumentException("Mật khẩu mới và xác nhận không khớp");
			}

			// Lấy thông tin user
			UserDtls user = userService.getUserByEmail(principal.getName());

			// Kiểm tra mật khẩu hiện tại
			if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
				throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
			}

			// Cập nhật mật khẩu mới
			user.setPassword(passwordEncoder.encode(newPassword));
			UserDtls updatedUser = userService.updateUser(user);

			if (updatedUser != null) {
				response.put("success", true);
				return ResponseEntity.ok(response);
			} else {
				throw new RuntimeException("Cập nhật mật khẩu thất bại");
			}

		} catch (Exception e) {
			response.put("success", false);
			response.put("message", e.getMessage());
			return ResponseEntity.badRequest().body(response);
		}
	}


}
