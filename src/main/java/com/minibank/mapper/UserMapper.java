package com.minibank.mapper;

import com.minibank.dto.response.UserResponse;
import com.minibank.entity.User;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {

    UserResponse toResponse(User user);

    List<UserResponse> toResponses(List<User> users);
}
