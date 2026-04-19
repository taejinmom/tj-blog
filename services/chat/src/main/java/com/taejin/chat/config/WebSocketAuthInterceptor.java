package com.taejin.chat.config;

import com.taejin.chat.security.JwtTokenProvider;
import com.taejin.chat.service.UserProvisioningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final String PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final UserProvisioningService userProvisioningService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(PREFIX)) {
                throw new MessageDeliveryException("Missing or invalid Authorization header");
            }

            String token = authHeader.substring(PREFIX.length());

            if (!tokenProvider.validateToken(token)) {
                throw new MessageDeliveryException("Invalid JWT token");
            }

            Long userId = tokenProvider.getUserId(token);
            String email = tokenProvider.getEmail(token);
            userProvisioningService.provisionIfMissing(userId, email);

            List<String> roles = tokenProvider.getRoles(token);

            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userId.toString(), null, authorities);
            accessor.setUser(auth);

            log.info("WebSocket CONNECT authenticated for userId: {}", userId);
        }

        return message;
    }
}
