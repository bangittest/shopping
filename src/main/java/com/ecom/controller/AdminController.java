package com.ecom.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

import com.ecom.dto.ProductDTO;
import com.ecom.exception.ApiResponse;
import com.ecom.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.ecom.model.Category;
import com.ecom.model.Product;
import com.ecom.model.ProductOrder;
import com.ecom.model.UserDtls;
import com.ecom.service.CartService;
import com.ecom.service.CategoryService;
import com.ecom.service.OrderService;
import com.ecom.service.ProductService;
import com.ecom.service.UserService;
import com.ecom.util.CommonUtil;
import com.ecom.util.OrderStatus;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CommonUtil commonUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

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

    @GetMapping("/")
    public String index() {
        return "admin/index";
    }

    @GetMapping("/loadAddProduct")
    public String loadAddProduct(Model m) {
        List<Category> categories = categoryService.getAllCategory();
        m.addAttribute("categories", categories);
        return "admin/add_product";
    }

    @GetMapping("/category")
    public String category(Model m, @RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
                           @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        // m.addAttribute("categorys", categoryService.getAllCategory());
        Page<Category> page = categoryService.getAllCategorPagination(pageNo, pageSize);
        List<Category> categorys = page.getContent();
        m.addAttribute("categorys", categorys);

        m.addAttribute("pageNo", page.getNumber());
        m.addAttribute("pageSize", pageSize);
        m.addAttribute("totalElements", page.getTotalElements());
        m.addAttribute("totalPages", page.getTotalPages());
        m.addAttribute("isFirst", page.isFirst());
        m.addAttribute("isLast", page.isLast());

        return "admin/category";
    }

    @GetMapping("/saveCategory")
    public String editCategory(Model m) {
        Category category = new Category();
        m.addAttribute("category", category);
        return "admin/add_category";
    }

    @PostMapping("/saveCategory")
    @ResponseBody
    public ResponseEntity<?> saveCategory(@ModelAttribute Category category) throws IOException {

        // Kiểm tra xem danh mục đã tồn tại hay chưa
        Boolean existCategory = categoryService.existCategory(category.getName());

        // Nếu danh mục đã tồn tại, trả về thông báo lỗi
        if (existCategory) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Danh mục đã tồn tại."));
        } else {
            // Nếu danh mục chưa tồn tại, lưu danh mục mới
            Category saveCategory = categoryService.saveCategory(category);
            return ResponseEntity.ok(new ApiResponse(true, "Thêm mới danh mục thành công."));
        }
    }


    @GetMapping("/deleteCategory/{id}")
    public String deleteCategory(@PathVariable int id, HttpSession session) {
        Boolean deleteCategory = categoryService.deleteCategory(id);
        if (Boolean.TRUE.equals(deleteCategory)) {
            session.setAttribute("succMsg", "category delete success");
        } else {
            session.setAttribute("errorMsg", "something wrong on server");
        }
        return "redirect:/admin/category";
    }


    @GetMapping("/loadEditCategory/{id}")
    public String loadEditCategory(@PathVariable int id, Model m) {
        m.addAttribute("category", categoryService.getCategoryById(id));
        return "admin/edit_category";
    }

    @PostMapping("/updateCategory")
    public ResponseEntity<ApiResponse> updateCategory(@ModelAttribute Category category) {
        // Lấy danh mục cũ theo ID
        Category oldCategory = categoryService.getCategoryById(category.getId());

        if (oldCategory == null) {
            return ResponseEntity.ok(new ApiResponse(false, "Danh mục không tồn tại."));
        }

        boolean isExist = categoryService.existCategory(category.getName());
        if (isExist && !oldCategory.getName().equalsIgnoreCase(category.getName())) {
            return ResponseEntity.ok(new ApiResponse(false, "Tên danh mục đã tồn tại, vui lòng chọn tên khác."));
        }

        // Cập nhật dữ liệu
        oldCategory.setName(category.getName());
        oldCategory.setIsActive(category.getIsActive());

        Category updated = categoryService.saveCategory(oldCategory);
        if (updated != null) {
            return ResponseEntity.ok(new ApiResponse(true, "Cập nhật danh mục thành công."));
        } else {
            return ResponseEntity.ok(new ApiResponse(false, "Có lỗi xảy ra trong quá trình cập nhật."));
        }
    }


    @PostMapping("/saveProduct")
    @ResponseBody
    public ResponseEntity<?> saveProduct(@ModelAttribute Product product,
                                         @RequestParam("file") MultipartFile image,
                                         HttpSession session) throws IOException {

        String imageName = image.isEmpty() ? "default.jpg" : image.getOriginalFilename();

        product.setImage(imageName);
        product.setDiscount(0);
        product.setDiscountPrice(product.getPrice());

        Product saveProduct = productService.saveProduct(product);

        if (!ObjectUtils.isEmpty(saveProduct)) {
            // Đảm bảo thư mục tồn tại
            File saveFile = new File("src/main/resources/static/img/product_img");
            if (!saveFile.exists()) {
                saveFile.mkdirs();  // Tạo thư mục nếu chưa tồn tại
            }

            Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + imageName);

            // Lưu file vào thư mục
            Files.copy(image.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

            session.setAttribute("succMsg", "Product Saved Success");

            // Trả về JSON với thông báo thành công
            return ResponseEntity.ok(new ApiResponse(true, "Product saved successfully"));
        } else {
            session.setAttribute("errorMsg", "Something went wrong on server");

            // Trả về JSON với thông báo lỗi
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to save product"));
        }
    }


    @GetMapping("/products")
    public String loadViewProduct(Model m, @RequestParam(defaultValue = "") String ch,
                                  @RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
                                  @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {

//		List<Product> products = null;
//		if (ch != null && ch.length() > 0) {
//			products = productService.searchProduct(ch);
//		} else {
//			products = productService.getAllProducts();
//		}
//		m.addAttribute("products", products);

        Page<Product> page = null;
        if (ch != null && ch.length() > 0) {
            page = productService.searchProductPagination(pageNo, pageSize, ch);
        } else {
            page = productService.getAllProductsPagination(pageNo, pageSize);
        }
        m.addAttribute("products", page.getContent().stream().map(p -> {
            ProductDTO productDTO = new ProductDTO(p);
            NumberFormat vnFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
            String formattedPrice = vnFormat.format(p.getPrice());
            String formattedDiscount = vnFormat.format(p.getDiscountPrice());
            productDTO.setPrice(formattedPrice);
            productDTO.setDiscountPrice(formattedDiscount);
            return productDTO;
        }).collect(Collectors.toList()));

        m.addAttribute("pageNo", page.getNumber());
        m.addAttribute("pageSize", pageSize);
        m.addAttribute("totalElements", page.getTotalElements());
        m.addAttribute("totalPages", page.getTotalPages());
        m.addAttribute("isFirst", page.isFirst());
        m.addAttribute("isLast", page.isLast());

        return "admin/products";
    }

    @GetMapping("/deleteProduct/{id}")
    public String deleteProduct(@PathVariable int id, HttpSession session) {
        Boolean deleteProduct = productService.deleteProduct(id);
        if (Boolean.TRUE.equals(deleteProduct)) {
            session.setAttribute("succMsg", "Product delete success");
        } else {
            session.setAttribute("errorMsg", "Something wrong on server");
        }
        return "redirect:/admin/products";
    }

    @GetMapping("/editProduct/{id}")
    public String editProduct(@PathVariable int id, Model m) {
        m.addAttribute("product", productService.getProductById(id));
        m.addAttribute("categories", categoryService.getAllCategory());
        return "admin/edit_product";
    }

    @PostMapping("/updateProduct")
    @ResponseBody
    public ResponseEntity<?> updateProduct(@ModelAttribute Product product,
                                           @RequestParam("file") MultipartFile image,
                                           HttpSession session) throws IOException {

        // Kiểm tra giảm giá hợp lệ
        if (product.getDiscount() < 0 || product.getDiscount() > 100) {
            return ResponseEntity
                    .badRequest()
                    .body(new ApiResponse(false, "Invalid Discount"));
        }

        // Cập nhật sản phẩm
        Product updatedProduct = productService.updateProduct(product, image);

        if (updatedProduct != null) {
            // Thành công
            return ResponseEntity.ok(new ApiResponse(true, "Thêm mới thành công"));
        } else {
            // Lỗi
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Something went wrong on the server"));
        }
    }


    @GetMapping("/users")
    public String getAllUsers(Model m) {
        List<UserDtls> users = userService.getUsers();

//        if (type == 1) {
//            users = userService.getUsers("ROLE_USER");
//        } else {
//            users = userService.getUsers("ROLE_ADMIN");
//        }
        m.addAttribute("users", users);
        return "/admin/users";
    }

    //    @GetMapping("/updateSts")
//    public String updateUserAccountStatus(@RequestParam Boolean status, @RequestParam Integer id, @RequestParam Integer type, HttpSession session) {
//        Boolean f = userService.updateAccountStatus(id, status);
//        if (Boolean.TRUE.equals(f)) {
//            session.setAttribute("succMsg", "Account Status Updated");
//        } else {
//            session.setAttribute("errorMsg", "Something wrong on server");
//        }
//        return "redirect:/admin/users?type=" + type;
//    }
    @PostMapping("/updateStatus")
    @ResponseBody
    public ResponseEntity<ApiResponse> updateStatus(@RequestParam("id") Integer userId, @RequestParam("status") Boolean status) {
        boolean success = userService.updateAccountStatus(userId, status);
        if (success) {
            return ResponseEntity.ok(new ApiResponse(true, "Cập nhật trạng thái thành công!"));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Cập nhật trạng thái thất bại!"));
        }
    }

    @PostMapping("/assignRole")
    @ResponseBody
    public ResponseEntity<ApiResponse> assignRole(@RequestParam("id") Integer userId, @RequestParam("role") String role) {

        UserDtls userDtls = userRepository.findById(userId).orElse(null);
        if (Objects.nonNull(userDtls) && userDtls.getIsEnable().equals(Boolean.FALSE)) {
            return ResponseEntity.ok(new ApiResponse(false, "Trạng thái đang khóa, Phân quyền thất bại!"));
        }
        boolean success = userService.assignRole(userId, role);
        if (success) {
            return ResponseEntity.ok(new ApiResponse(true, "Phân quyền thành công!"));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Phân quyền thất bại!"));
        }

    }


    @GetMapping("/orders")
    public String getAllOrders(Model m, @RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
                               @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
//		List<ProductOrder> allOrders = orderService.getAllOrders();
//		m.addAttribute("orders", allOrders);
//		m.addAttribute("srch", false);

        Page<ProductOrder> page = orderService.getAllOrdersPagination(pageNo, pageSize);
        m.addAttribute("orders", page.getContent());
        m.addAttribute("srch", false);

        m.addAttribute("pageNo", page.getNumber());
        m.addAttribute("pageSize", pageSize);
        m.addAttribute("totalElements", page.getTotalElements());
        m.addAttribute("totalPages", page.getTotalPages());
        m.addAttribute("isFirst", page.isFirst());
        m.addAttribute("isLast", page.isLast());

        return "/admin/orders";
    }

    @PostMapping("/update-order-status")
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
            session.setAttribute("succMsg", "Status Updated");
        } else {
            session.setAttribute("errorMsg", "status not updated");
        }
        return "redirect:/admin/orders";
    }

    @GetMapping("/search-order")
    public String searchProduct(@RequestParam String orderId, Model m, HttpSession session,
                                @RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
                                @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {

        if (orderId != null && orderId.length() > 0) {

            ProductOrder order = orderService.getOrdersByOrderId(orderId.trim());

            if (ObjectUtils.isEmpty(order)) {
                session.setAttribute("errorMsg", "Incorrect orderId");
                m.addAttribute("orderDtls", null);
            } else {
                m.addAttribute("orderDtls", order);
            }

            m.addAttribute("srch", true);
        } else {
//			List<ProductOrder> allOrders = orderService.getAllOrders();
//			m.addAttribute("orders", allOrders);
//			m.addAttribute("srch", false);

            Page<ProductOrder> page = orderService.getAllOrdersPagination(pageNo, pageSize);
            m.addAttribute("orders", page);
            m.addAttribute("srch", false);

            m.addAttribute("pageNo", page.getNumber());
            m.addAttribute("pageSize", pageSize);
            m.addAttribute("totalElements", page.getTotalElements());
            m.addAttribute("totalPages", page.getTotalPages());
            m.addAttribute("isFirst", page.isFirst());
            m.addAttribute("isLast", page.isLast());

        }
        return "/admin/orders";

    }

    @GetMapping("/add-admin")
    public String loadAdminAdd() {
        return "/admin/add_admin";
    }

    @PostMapping("/save-admin")
    public String saveAdmin(@ModelAttribute UserDtls user, @RequestParam("img") MultipartFile file, HttpSession session)
            throws IOException {

        String imageName = file.isEmpty() ? "default.jpg" : file.getOriginalFilename();
        user.setProfileImage(imageName);
        UserDtls saveUser = userService.saveAdmin(user);

        if (!ObjectUtils.isEmpty(saveUser)) {
            if (!file.isEmpty()) {
                File saveFile = new ClassPathResource("static/img").getFile();

                Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "profile_img" + File.separator
                        + file.getOriginalFilename());

//				System.out.println(path);
                Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            }
            session.setAttribute("succMsg", "Register successfully");
        } else {
            session.setAttribute("errorMsg", "something wrong on server");
        }

        return "redirect:/admin/add-admin";
    }

    @GetMapping("/profile")
    public String profile() {
        return "/admin/profile";
    }


    @PostMapping("/update-profile")
    public String updateProfile(@ModelAttribute UserDtls user, @RequestParam MultipartFile img, HttpSession session) {
        UserDtls updateUserProfile = userService.updateUserProfile(user, img);
        if (ObjectUtils.isEmpty(updateUserProfile)) {
            session.setAttribute("errorMsg", "Profile not updated");
        } else {
            session.setAttribute("succMsg", "Profile Updated");
        }
        return "redirect:/admin/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String newPassword, @RequestParam String currentPassword, Principal p,
                                 HttpSession session) {
        UserDtls loggedInUserDetails = commonUtil.getLoggedInUserDetails(p);

        boolean matches = passwordEncoder.matches(currentPassword, loggedInUserDetails.getPassword());

        if (matches) {
            String encodePassword = passwordEncoder.encode(newPassword);
            loggedInUserDetails.setPassword(encodePassword);
            UserDtls updateUser = userService.updateUser(loggedInUserDetails);
            if (ObjectUtils.isEmpty(updateUser)) {
                session.setAttribute("errorMsg", "Password not updated !! Error in server");
            } else {
                session.setAttribute("succMsg", "Password Updated sucessfully");
            }
        } else {
            session.setAttribute("errorMsg", "Current Password incorrect");
        }

        return "redirect:/admin/profile";
    }

}
