package com.minibank.mapper;

import com.minibank.dto.response.UserResponse;
import com.minibank.entity.User;
import org.mapstruct.Mapper;

@Mapper
public interface UserMapper {

    UserResponse toResponse(User user);
}
