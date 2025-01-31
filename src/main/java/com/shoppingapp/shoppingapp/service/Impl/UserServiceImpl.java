package com.shoppingapp.shoppingapp.service.Impl;

import com.shoppingapp.shoppingapp.dto.request.ChangePasswordRequest;
import com.shoppingapp.shoppingapp.dto.request.ProfileUpdateRequest;
import com.shoppingapp.shoppingapp.dto.request.UserCreationRequest;
import com.shoppingapp.shoppingapp.dto.request.UserUpdateRequest;
import com.shoppingapp.shoppingapp.dto.response.CustomerResponse;
import com.shoppingapp.shoppingapp.dto.response.UserResponse;
import com.shoppingapp.shoppingapp.dto.response.VendorResponse;
import com.shoppingapp.shoppingapp.exceptions.ErrorCode;
import com.shoppingapp.shoppingapp.exceptions.AppException;
import com.shoppingapp.shoppingapp.mapper.UserMapper;
import com.shoppingapp.shoppingapp.models.Role;
import com.shoppingapp.shoppingapp.models.Shop;
import com.shoppingapp.shoppingapp.models.UnverifiedShop;
import com.shoppingapp.shoppingapp.models.User;

import com.shoppingapp.shoppingapp.repository.*;
import com.shoppingapp.shoppingapp.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserServiceImpl implements UserService {


    UserRepository userRepository;
    RoleRepository roleRepository;
    ShopRepository shopRepository;
    UnverifiedShopRepository unverifiedShopRepository;
    OrderRepository orderRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;

    @Override
    public UserResponse createUser(UserCreationRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USERNAME_EXISTED);
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }

        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        var roles = roleRepository.findAllById(request.getRoles());
        user.setRoles(new HashSet<>(roles));


        user.setIsActive(true);

        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        return userMapper.toUserResponse(user);
    }

    @Override
    public UserResponse getMyInfo() {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        User user = userRepository.findByUsername(name).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return userMapper.toUserResponse(user);
    }

    @Override
    @PostAuthorize("returnObject.username == authentication.name")
    public UserResponse updateUser(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        System.out.println("User "+user.getUsername());
        System.out.println("Requ "+request.getUsername());
        userMapper.updateUser(user, request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        System.out.println("Phon "+user.getPhone());
        Set<Role> roles = new HashSet<>(roleRepository.findAllById(request.getRoles()));
        if (roles.size() != request.getRoles().size()) {
            throw new AppException(ErrorCode.ROLE_NOT_FOUND); // New Error Code
        }
        user.setRoles(new HashSet<>(roles));

        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Override
    public String updateUserPhone(Long id, String phone) {
        User user = userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        user.setPhone(phone);
        userRepository.save(user);
        return "Update phone successful!";
    }


    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    @Override
    public String userName(Long id) {
        User user = userRepository.findById(id).orElse(null);
        System.out.println("Result: "+user+", "+user.getId());
        return user.getFirstName() + user.getLastName();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> getAll() {
        log.info("In method get Users");
        return userRepository.findAll().stream().map(userMapper::toUserResponse).toList();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public List<VendorResponse> getVendors() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRoles().stream().anyMatch(role -> role.getName().equals("VENDOR")))
                .map(user -> {
                    Optional<Shop> shop = shopRepository.findByUserId(user.getId()); // Assuming there's a method to find a shop by user ID
                    Shop shop1 = shop.orElse(null);

                    Optional<UnverifiedShop> unverifiedShop = unverifiedShopRepository.findByUserId(user.getId());
                    UnverifiedShop unverifiedShop1 = unverifiedShop.orElse(null);

                    return VendorResponse.builder()
                            .id(user.getId())
                            .firstName(user.getFirstName())
                            .lastName(user.getLastName())
                            .email(user.getEmail())
                            .phone(user.getPhone())
                            .isActive(user.getIsActive())
                            .shop(shop1)
                            .unverifiedShop(unverifiedShop1)
                            .build();
                })
                .toList();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public List<CustomerResponse> getCustomers() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRoles().stream().anyMatch(role -> role.getName().equals("CUSTOMER")))
                .map(user -> {

                    Long totalOrder = (long) orderRepository.countOrderByUserId(user.getId()); // Assuming there's a method to get the total order of a customer

                   return CustomerResponse.builder()
                            .id(user.getId())
                            .firstName(user.getFirstName())
                            .lastName(user.getLastName())
                            .email(user.getEmail())
                            .phone(user.getPhone())
                            .isActive(user.getIsActive())
                            .totalOrder(totalOrder) // Assuming there's a method to get the total order of a customer
                            .build();
                })
                .toList();
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    public UserResponse getUserById(Long id) {
        return userMapper.toUserResponse(
                userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)));
    }

    @Override
    public int getTotalVendors() {
        List<User> userList = userRepository.findAll();
        return (int) userList.stream().filter(user -> user.getRoles().stream().anyMatch(role -> role.getName().equals("VENDOR"))).count();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void banUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        user.setIsActive(false);
        userRepository.save(user);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void unbanUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        user.setIsActive(true);
        userRepository.save(user);
    }



    @Override
    public void changePassword(Long id, ChangePasswordRequest request) {
        User user = userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.OLD_PASSWORD_IS_INCORRECT);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);


    }

    @Override
    //@PostAuthorize("returnObject.username == authentication.name")
    public void updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        userRepository.save(user);
    }


}