package com.flowgate.backend.user.mapper;

import com.flowgate.backend.user.dto.UserDto;
import com.flowgate.backend.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(User user);
    User toEntity(UserDto dto);
}
