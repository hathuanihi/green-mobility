package com.greenmobility.config;

import com.greenmobility.common.security.CustomUserDetailsService;
import com.greenmobility.common.security.JwtTokenProvider;
import com.greenmobility.modules.drivervehicle.entity.DriverProfile;
import com.greenmobility.modules.drivervehicle.repository.DriverProfileRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final DriverProfileRepository driverProfileRepository;

    public WebSocketConfig(JwtTokenProvider tokenProvider,
                           CustomUserDetailsService userDetailsService,
                           DriverProfileRepository driverProfileRepository) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
        this.driverProfileRepository = driverProfileRepository;
    }

    public static class StompPrincipalToken extends UsernamePasswordAuthenticationToken {
        private final String principalName;

        public StompPrincipalToken(Object principal, Object credentials,
                                   Collection<? extends GrantedAuthority> authorities,
                                   String principalName) {
            super(principal, credentials, authorities);
            this.principalName = principalName;
        }

        @Override
        public String getName() {
            return principalName;
        }
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Native WebSocket endpoint for Flutter Mobile App
        registry.addEndpoint("/ws-connect")
                .setAllowedOriginPatterns("*");

        // SockJS fallback endpoint for Web Browsers / Admin
        registry.addEndpoint("/ws-connect")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Topic for broadcast (e.g. /topic/trip/{tripId}), Queue for user destinations
        registry.enableSimpleBroker("/topic", "/queue");
        // Prefix for client messages sent to server (e.g. /app/driver/location-ping)
        registry.setApplicationDestinationPrefixes("/app");
        // Prefix for private user messages (e.g. /user/queue/ride-dispatch)
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                        String jwt = authHeader.substring(7);
                        if (tokenProvider.validateToken(jwt)) {
                            UUID userId = tokenProvider.getUserIdFromToken(jwt);
                            UserDetails userDetails = userDetailsService.loadUserById(userId);

                            // Resolve principal identifier:
                            // For drivers: map to driverProfile.id so /user/queue/ride-dispatch matches candidate.driverId
                            // For other users: map to userId.toString()
                            String principalName = userId.toString();
                            Optional<DriverProfile> driverProfileOpt = driverProfileRepository.findByUserId(userId);
                            if (driverProfileOpt.isPresent()) {
                                principalName = driverProfileOpt.get().getId().toString();
                            }

                            StompPrincipalToken authentication =
                                    new StompPrincipalToken(userDetails, null, userDetails.getAuthorities(), principalName);
                            accessor.setUser(authentication);
                        }
                    }
                }
                return message;
            }
        });
    }
}
