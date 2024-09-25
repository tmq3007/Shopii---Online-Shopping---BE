package com.shoppingapp.shoppingapp.service.Impl;

import com.shoppingapp.shoppingapp.dto.request.ShopCreationRequest;
import com.shoppingapp.shoppingapp.dto.request.ShopUpdateRequest;
import com.shoppingapp.shoppingapp.dto.response.ShopResponse;
import com.shoppingapp.shoppingapp.exceptions.AppException;
import com.shoppingapp.shoppingapp.exceptions.ErrorCode;
import com.shoppingapp.shoppingapp.mapper.ShopMapper;
import com.shoppingapp.shoppingapp.models.Orders;
import com.shoppingapp.shoppingapp.models.Product;
import com.shoppingapp.shoppingapp.models.Shop;
import com.shoppingapp.shoppingapp.repository.*;
import com.shoppingapp.shoppingapp.service.ShopService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ShopServiceImp implements ShopService {
    @Autowired
    private ShopRepository shopRepository;
    @Autowired
    private ShopMapper shopMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ProductRepository productRepository;


    @Override
    public List<Shop> getAllShops() {
        return (List<Shop>) shopRepository.findAll();
    }

    @Override
    public ShopResponse getShopById(long shopId) {
        return shopMapper.toShopResponse(shopRepository.findById(shopId).orElseThrow(()-> new RuntimeException("Shop not found")));
    }

    @Override
    public Shop createShop(ShopCreationRequest request) {
        if(shopRepository.existsByShopName(request.getShopName())){
            throw new AppException(ErrorCode.SHOP_EXISTED);
        }
        Shop shop = shopMapper.toShop(request);
        var userOp = userRepository.findById(Long.valueOf(request.getUser()));
        if (!userOp.isPresent()) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);  // Xử lý khi không tìm thấy user
        }

        shop.setUser(userOp.get());

        Set<Long> productIds = request.getProducts().stream()
                .map(Long::valueOf)
                .collect(Collectors.toSet());;  // Assuming the request has product IDs to link
        Set<Product> products = new HashSet<>(productRepository.findAllById(productIds));
        shop.setProducts(products);

        Set<Long> orderIds = request.getOrder().stream()
                .map(Long::valueOf)
                .collect(Collectors.toSet());;  // Assuming the request has product IDs to link
        Set<Orders> orders = new HashSet<>(orderRepository.findAllById(orderIds));
        shop.setOrder(orders);
        return shopRepository.save(shop);
    }

    @Override
    public ShopResponse updateShop(ShopUpdateRequest request, long shopId) {
        Shop shop = shopRepository.findById(shopId).orElseThrow(()-> new RuntimeException("Shop not found"));
        shopMapper.updateShop(shop,request);
        var userOp = userRepository.findById(Long.valueOf(request.getUser()));
        if (!userOp.isPresent()) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);  // Xử lý khi không tìm thấy user
        }

        shop.setUser(userOp.get());

        Set<Long> productIds = request.getProducts().stream()
                .map(Long::valueOf)
                .collect(Collectors.toSet());;  // Assuming the request has product IDs to link
        Set<Product> products = new HashSet<>(productRepository.findAllById(productIds));
        shop.setProducts(products);

        Set<Long> orderIds = request.getOrder().stream()
                .map(Long::valueOf)
                .collect(Collectors.toSet());;  // Assuming the request has product IDs to link
        Set<Orders> orders = new HashSet<>(orderRepository.findAllById(orderIds));
        shop.setOrder(orders);
        return shopMapper.toShopResponse(shopRepository.save(shop));
    }

    @Override
    public String deleteShop(Long shopId) {
        shopRepository.deleteById(shopId);
        return "Deleted";
    }




}
