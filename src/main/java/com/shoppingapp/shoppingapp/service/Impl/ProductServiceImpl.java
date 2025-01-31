package com.shoppingapp.shoppingapp.service.Impl;

import com.shoppingapp.shoppingapp.dto.request.ProductCreationRequest;
import com.shoppingapp.shoppingapp.dto.request.ProductUpdateRequest;
import com.shoppingapp.shoppingapp.dto.response.ProductResponse;
import com.shoppingapp.shoppingapp.exceptions.AppException;
import com.shoppingapp.shoppingapp.exceptions.ErrorCode;
import com.shoppingapp.shoppingapp.mapper.ProductMapper;
import com.shoppingapp.shoppingapp.models.Product;
import com.shoppingapp.shoppingapp.repository.CategoryRepository;
import com.shoppingapp.shoppingapp.repository.ProductRepository;
import com.shoppingapp.shoppingapp.repository.ShopRepository;
import com.shoppingapp.shoppingapp.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j

public class ProductServiceImpl implements ProductService {



    @Override
    public String deleteProductById(Long productId) {
        productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_EXISTED));
        productRepository.deleteById(productId);
        return "";
    }

    @Autowired
    private ProductMapper productMapper;


    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ShopRepository shopRepository;

    @Override
    public List<Product> getAllProducts() {
        return (List<Product>) productRepository.findAll();
    }



    @Override
    public Product getProductById(Long ProductId) {
        return productRepository.findById(ProductId).orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_EXISTED));
    }

    @Override
    public Product createProduct(ProductCreationRequest request) {

        // Kiểm tra shopId
        var shopOptional = shopRepository.findById(request.getShop());
        if (!shopOptional.isPresent()) {
            throw new AppException(ErrorCode.SHOP_NOT_EXISTED);
        }

        // Kiểm tra categoryId
        var categoryOptional = categoryRepository.findById(request.getCategory());
        if (!categoryOptional.isPresent()) {
            throw new AppException(ErrorCode.CATEGORY_NOT_EXISTED);
        }

        // Check productName để tránh trùng lặp
        if (productRepository.existsByProductName(request.getProductName())) {
            throw new AppException(ErrorCode.PRODUCT_EXISTED);
        }

        // Tạo sản phẩm
        Product product = productMapper.toProduct(request);
        product.setShop(shopOptional.get());
        product.setCategory(categoryOptional.get());
        product.setAverageRating(0.0);
        return productRepository.save(product);
    }


    @Override
    public ProductResponse updateProduct(Long productId, ProductUpdateRequest request) {
        if (productId == null) {
            throw new AppException(ErrorCode.PRODUCT_NOT_EXISTED);
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_EXISTED));

        productMapper.updateProduct(product, request);

        // Check if shop ID is provided before updating shop
        if (request.getShop() != null) {
            var shopOptional = shopRepository.findById(request.getShop());
            if (!shopOptional.isPresent()) {
                throw new AppException(ErrorCode.SHOP_NOT_EXISTED);
            }
            product.setShop(shopOptional.get());
        }

        if (request.getCategory() != null) {
            var categoryOptional = categoryRepository.findById(request.getCategory());
            if (!categoryOptional.isPresent()) {
                throw new AppException(ErrorCode.CATEGORY_NOT_EXISTED);
            }
            product.setCategory(categoryOptional.get());
        }

        return productMapper.toProductResponse(productRepository.save(product));
    }

    @Override
    public List<Product> getAllProductsByShopId(Long shopId) {
        return productRepository.findAllProductsByShopId(shopId);
    }

    @Override
    public String deleteAmountAfterMadeOrder(Long productId, int amount) {
        Product product = productRepository.findById(productId).orElse(null);
        if(product == null) {
            throw new AppException(ErrorCode.PRODUCT_NOT_EXISTED);
        }else{
            product.setStock(product.getStock() - amount);
            productRepository.save(product);
            return "Saved successfully";
        }
    }

    @Override
    public List<Product> getTop10ByHighestAverageRating() {
        return productRepository.findTop10ByHighestAverageRating();
    }

    @Override
    public List<Product> getTop10ByMostSold() {
        return productRepository.findTop10ByMostSold();
    }
}
