package com.polatholding.procurementsystem.service;

import com.polatholding.procurementsystem.model.RequestHistory;
import com.polatholding.procurementsystem.model.User;
import com.polatholding.procurementsystem.dto.RequestHistoryDto;
import com.polatholding.procurementsystem.repository.RequestHistoryRepository;
import com.polatholding.procurementsystem.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RequestHistoryServiceImpl implements RequestHistoryService {

    private final RequestHistoryRepository requestHistoryRepository;
    private final UserRepository userRepository;

    public RequestHistoryServiceImpl(RequestHistoryRepository requestHistoryRepository,
                                     UserRepository userRepository) {
        this.requestHistoryRepository = requestHistoryRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void logAction(int requestId, String userEmail, String action, String details) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));
        RequestHistory history = new RequestHistory();
        history.setRequestId(requestId);
        history.setUser(user);
        history.setAction(action);
        history.setDetails(details);
        history.setEventDate(LocalDateTime.now());
        requestHistoryRepository.save(history);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<RequestHistoryDto> getAllHistory() {
        return requestHistoryRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(java.util.stream.Collectors.toList());
    }

    private RequestHistoryDto convertToDto(RequestHistory history) {
        RequestHistoryDto dto = new RequestHistoryDto();
        dto.setHistoryId(history.getHistoryId());
        dto.setRequestId(history.getRequestId());
        dto.setAction(history.getAction());
        dto.setDetails(history.getDetails());
        dto.setEventDate(history.getEventDate());
        if (history.getUser() != null) {
            dto.setUserEmail(history.getUser().getEmail());
        }
        return dto;
    }
}
