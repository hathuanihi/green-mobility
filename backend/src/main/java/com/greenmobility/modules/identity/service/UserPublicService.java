package com.greenmobility.modules.identity.service;

import com.greenmobility.modules.identity.dto.UserPublicDto;
import com.greenmobility.modules.identity.entity.User;
import com.greenmobility.modules.identity.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserPublicService {

    private final UserRepository userRepository;

    public UserPublicService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Optional<UserPublicDto> findById(UUID userId) {
        if (userId == null) return Optional.empty();
        return userRepository.findById(userId).map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Map<UUID, UserPublicDto> findUsersByIds(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) return Collections.emptyMap();
        List<User> users = userRepository.findAllById(userIds);
        return users.stream()
                .map(this::mapToDto)
                .collect(Collectors.toMap(UserPublicDto::getId, u -> u));
    }

    private UserPublicDto mapToDto(User user) {
        return new UserPublicDto(
                user.getId(),
                user.getPhoneNumber(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name(),
                user.getStatus().name()
        );
    }
}
