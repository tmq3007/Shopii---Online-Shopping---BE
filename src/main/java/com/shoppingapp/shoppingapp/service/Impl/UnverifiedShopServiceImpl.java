package com.shoppingapp.shoppingapp.service.Impl;

import com.shoppingapp.shoppingapp.dto.request.UnverifiedShopCreationRequest;
import com.shoppingapp.shoppingapp.exceptions.AppException;
import com.shoppingapp.shoppingapp.exceptions.ErrorCode;
import com.shoppingapp.shoppingapp.mapper.UnverifiedShopMapper;
import com.shoppingapp.shoppingapp.models.Shop;
import com.shoppingapp.shoppingapp.models.UnverifiedShop;
import com.shoppingapp.shoppingapp.repository.ShopRepository;
import com.shoppingapp.shoppingapp.repository.UnverifiedShopRepository;
import com.shoppingapp.shoppingapp.repository.UserRepository;
import com.shoppingapp.shoppingapp.service.UnverifiedShopService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j

public class UnverifiedShopServiceImpl implements UnverifiedShopService {


    private final UnverifiedShopRepository unverifiedShopRepository;
    private final ShopRepository shopRepository;
    UnverifiedShopMapper unverifiedShopMapper;
    private final UserRepository userRepository;

    @Override
    public UnverifiedShop addUnverifiedShop(UnverifiedShopCreationRequest request ) {
        //shop exist
        if(shopRepository.existsByShopName(request.getShopName())) {
            throw new AppException(ErrorCode.SHOP_EXISTED);
        }

        if(unverifiedShopRepository.existsByShopName(request.getShopName())) {
            throw new AppException(ErrorCode.SHOP_REQUEST_EXISTED);
        }

        UnverifiedShop unverifiedShop = unverifiedShopMapper.toUnverifiedShop(request);

        var userOp = userRepository.findById(request.getUser());
        if (userOp.isEmpty()) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);  // Xử lý khi không tìm thấy user
        }

        unverifiedShop.setUser(userOp.get());
        return unverifiedShopRepository.save(unverifiedShop);
    }

    @Override
    public UnverifiedShop getUnverifiedShop() {
        return null;
    }

    @Override
    public void verifyShop(Long unverifiedShopId) {
        //check id
        var unverifiedShop = unverifiedShopRepository.findById(unverifiedShopId).orElseThrow(
                () -> new AppException(ErrorCode.SHOP_REQUEST_NOT_EXISTED));

        Shop shop = new Shop();
        shop.setShopName(unverifiedShop.getShopName());
        shop.setUser(userRepository.findById(unverifiedShop.getUser().getId()).orElseThrow());
        shop.setAddress(unverifiedShop.getAddress());
        shop.setCity(unverifiedShop.getCity());
        shop.setState(unverifiedShop.getState());
        shop.setCountry(unverifiedShop.getCountry());
        shop.setPhone(unverifiedShop.getPhone());
        shop.setDescription(unverifiedShop.getDescription());
        shop.setLogo(unverifiedShop.getLogo());
        shop.setCover(unverifiedShop.getCover());

        shopRepository.save(shop);

        unverifiedShopRepository.delete(unverifiedShop);
    }

}
