package com.budget.config;

import com.budget.dao.PersonDao;
import com.budget.model.Person;
import com.budget.model.authentication.LoginCredentials;
import com.budget.utilities.JwtUtil;
import com.budget.utilities.StringUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StreamUtils;

import jakarta.servlet.ReadListener;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class JsonAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private static final Logger logger = LoggerFactory.getLogger(JsonAuthenticationFilter.class);
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MINUTES = 15;
    private static final String DateTimeFormatter_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtUtil jwtUtil;
    private PersonDao personDao;
    private final Map<String, Integer> loginAttempts = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lockoutTimes = new ConcurrentHashMap<>();

    public JsonAuthenticationFilter(AuthenticationManager authenticationManager,
                                   JwtUtil jwtUtil) {
        super.setAuthenticationManager(authenticationManager);
        this.jwtUtil = jwtUtil;
        setFilterProcessesUrl("/login/auth");
    }

    @Autowired
    public void setPersonDao(PersonDao personDao) {
        this.personDao = personDao;
    }

    // Store the current request data for use in other methods
    private final ThreadLocal<Map<String, String>> currentRequestData = new ThreadLocal<>();

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        
        validateContentType(request);
        
        LoginCredentials credentials = extractLoginCredentials(request);
        
        validateCredentials(credentials);
        
        checkAccountLockout(credentials.getUsername());
        
        return performAuthentication(request, credentials);
    }

    private void validateContentType(HttpServletRequest request) throws AuthenticationException {
        String contentType = request.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).contains("application/json")) {
            logger.warn("Invalid content type for login: {}", contentType);
            throw new AuthenticationException("Content-Type must be application/json") {};
        }
    }

    private LoginCredentials extractLoginCredentials(HttpServletRequest request) throws AuthenticationException {
        try {
            byte[] requestBody = StreamUtils.copyToByteArray(request.getInputStream());
            
            Map<String, String> requestMap = objectMapper.readValue(
                requestBody,
                new TypeReference<Map<String, String>>() {}
            );
            
            // Store request data for other methods to use
            currentRequestData.set(requestMap);
            
            String username = requestMap.get("username");
            String password = requestMap.get("password");
            
            return new LoginCredentials(username, password, requestBody);
            
        } catch (IOException e) {
            logger.error("Failed to parse login request: {}", e.getMessage());
            throw new AuthenticationException("Invalid JSON format") {};
        }
    }

    private void validateCredentials(LoginCredentials credentials) throws AuthenticationException {
        if (StringUtil.checkEmptyString(credentials.getUsername()) || 
            StringUtil.checkEmptyString(credentials.getPassword())) {
            logger.warn("Login failed: Username or password missing or empty");
            throw new AuthenticationException("Username and password are required") {};
        }
    }

    private void checkAccountLockout(String username) throws AuthenticationException {
        cleanExpiredLockouts();
        
        logLockoutStatus(username);
        
        if (isAccountLocked(username)) {
            handleAccountLocked(username);
        }
    }

    private void logLockoutStatus(String username) {
        String lockedUntil = lockoutTimes.get(username) != null ? 
            lockoutTimes.get(username).plusMinutes(LOCKOUT_DURATION_MINUTES).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : 
            "Not locked";
            
        logger.debug("Checking lockout for user: {}. Attempts: {}, Locked until: {}",
            username, loginAttempts.getOrDefault(username, 0), lockedUntil);
    }

    private void handleAccountLocked(String username) throws AuthenticationException {
        LocalDateTime unlockTime = lockoutTimes.get(username).plusMinutes(LOCKOUT_DURATION_MINUTES);
        logger.warn("Account locked for user: {}. Will be unlocked at: {}", username, unlockTime);
        
        String errorMessage = String.format(
            "Account temporarily locked due to %d failed attempts. Try again after %s", 
            MAX_LOGIN_ATTEMPTS, 
            unlockTime.format(DateTimeFormatter.ofPattern(DateTimeFormatter_PATTERN))
        );
        
        throw new AuthenticationException(errorMessage) {};
    }

    private Authentication performAuthentication(HttpServletRequest request, LoginCredentials credentials) 
            throws AuthenticationException {
        
        String sanitizedUsername = credentials.getUsername().trim().replaceAll("[^a-zA-Z0-9@._-]", "");
        
        logger.info("Attempting authentication for user: {} (Attempt #{} out of {})", 
            sanitizedUsername, loginAttempts.getOrDefault(sanitizedUsername, 0) + 1, MAX_LOGIN_ATTEMPTS);

        HttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request, credentials.getRequestBody());
        
        UsernamePasswordAuthenticationToken authRequest =
                new UsernamePasswordAuthenticationToken(sanitizedUsername, credentials.getPassword());
        setDetails(wrappedRequest, authRequest);
        
        return this.getAuthenticationManager().authenticate(authRequest);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                           FilterChain chain, Authentication authResult) throws IOException {
        UserDetails userDetails = (UserDetails) authResult.getPrincipal();
        String username = userDetails.getUsername();
        String token = jwtUtil.generateToken(userDetails);

        // Reset login attempts on successful authentication
        int previousAttempts = loginAttempts.getOrDefault(username, 0);
        logger.info("Successful authentication for user: {}. Resetting {} previous failed attempts.", 
            username, previousAttempts);
        loginAttempts.remove(username);
        lockoutTimes.remove(username);
        
        currentRequestData.remove();

        if (personDao != null) {
            try {
                Person person = personDao.findByUsername(username);
                if (person != null) {
                    person.setLastLogin(LocalDateTime.now());
                    personDao.save(person);
                    logger.info("Updated last login time for user: {}", username);
                } else {
                    logger.warn("User not found in database for username: {}", username);
                }
            } catch (Exception e) {
                logger.error("Failed to update last login time for user: {}", username, e);
            }
        } else {
            logger.warn("PersonDao is not initialized for last login update");
        }

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("username", username);
        responseData.put("token", token);
        responseData.put("roles", userDetails.getAuthorities().stream()
                .map(Object::toString)
                .collect(Collectors.toList()));

        Map<String, Object> apiResponse = new HashMap<>();
        apiResponse.put("success", true);
        apiResponse.put("message", "Login successful");
        apiResponse.put("timestamp", LocalDateTime.now().toString());
        apiResponse.put("data", responseData);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));

        logger.info("Successful authentication completed for user: {}", username);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                             AuthenticationException failed) throws IOException {
        String username = extractUsernameFromRequest();
        int currentAttempts = updateLoginAttempts(username);
        int remainingAttempts = Math.max(0, MAX_LOGIN_ATTEMPTS - currentAttempts);
        
        currentRequestData.remove();
        
        Map<String, Object> apiResponse = new HashMap<>();
        apiResponse.put("success", false);
        apiResponse.put("error", failed.getMessage());
        apiResponse.put("timestamp", LocalDateTime.now().toString());
        apiResponse.put("remainingAttempts", remainingAttempts);
        apiResponse.put("maxAttempts", MAX_LOGIN_ATTEMPTS);
        apiResponse.put("currentAttempts", currentAttempts);

        if (currentAttempts >= MAX_LOGIN_ATTEMPTS) {
            LocalDateTime unlockTime = lockoutTimes.get(username).plusMinutes(LOCKOUT_DURATION_MINUTES);
            apiResponse.put("accountLocked", true);
            apiResponse.put("unlockTime", unlockTime.format(DateTimeFormatter.ofPattern(DateTimeFormatter_PATTERN)));
            apiResponse.put("lockoutDurationMinutes", LOCKOUT_DURATION_MINUTES);
        } else {
            apiResponse.put("accountLocked", false);
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));

        logger.warn("Authentication failed for user '{}': {}. Current attempts: {}/{}, Remaining: {}", 
            username, failed.getMessage(), currentAttempts, MAX_LOGIN_ATTEMPTS, remainingAttempts);
        
        if (currentAttempts >= MAX_LOGIN_ATTEMPTS) {
            logger.warn("Account LOCKED for user '{}' after {} failed attempts. Locked until: {}", 
                username, MAX_LOGIN_ATTEMPTS, 
                lockoutTimes.get(username).plusMinutes(LOCKOUT_DURATION_MINUTES).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
    }

    private boolean isAccountLocked(String username) {
        LocalDateTime lockoutTime = lockoutTimes.get(username);
        if (lockoutTime != null) {
            LocalDateTime unlockTime = lockoutTime.plusMinutes(LOCKOUT_DURATION_MINUTES);
            if (unlockTime.isAfter(LocalDateTime.now())) {
                logger.debug("Account is currently locked for user: {} until {}", username, unlockTime);
                return true;
            } else {
                logger.info("Lockout expired for user: {}. Removing lockout and resetting attempts.", username);
                loginAttempts.remove(username);
                lockoutTimes.remove(username);
                return false;
            }
        }
        return false;
    }

    private int updateLoginAttempts(String username) {
        if (username != null && !username.isEmpty() && !"unknown".equals(username)) {
            int attempts = loginAttempts.getOrDefault(username, 0) + 1;
            loginAttempts.put(username, attempts);
            
            logger.debug("Updated login attempts for user: '{}'. Current attempts: {}/{}", 
                username, attempts, MAX_LOGIN_ATTEMPTS);
            
            if (attempts >= MAX_LOGIN_ATTEMPTS) {
                LocalDateTime lockoutTime = LocalDateTime.now();
                lockoutTimes.put(username, lockoutTime);
                LocalDateTime unlockTime = lockoutTime.plusMinutes(LOCKOUT_DURATION_MINUTES);
                logger.warn("Account LOCKED for user '{}' after {} failed attempts. Will unlock at: {}", 
                    username, MAX_LOGIN_ATTEMPTS, unlockTime.format(DateTimeFormatter.ofPattern(DateTimeFormatter_PATTERN)));
            }
            
            return attempts;
        } else {
            logger.debug("Skipping attempt update for invalid username: '{}'", username);
            return 0;
        }
    }

    private void cleanExpiredLockouts() {
        LocalDateTime now = LocalDateTime.now();
        lockoutTimes.entrySet().removeIf(entry -> {
            LocalDateTime unlockTime = entry.getValue().plusMinutes(LOCKOUT_DURATION_MINUTES);
            boolean expired = unlockTime.isBefore(now);
            if (expired) {
                String username = entry.getKey();
                logger.info("Cleaning expired lockout for user: '{}'. Was locked until: {}", 
                    username, unlockTime.format(DateTimeFormatter.ofPattern(DateTimeFormatter_PATTERN)));
                loginAttempts.remove(username);
            }
            return expired;
        });
    }

    private String extractUsernameFromRequest() {
        try {
            Map<String, String> requestMap = currentRequestData.get();
            if (requestMap != null) {
                String username = requestMap.get("username");
                return username != null ? username.trim().replaceAll("[^a-zA-Z0-9@._-]", "") : "unknown";
            }
            return "unknown";
        } catch (Exception e) {
            logger.warn("Failed to extract username from cached request data: {}", e.getMessage());
            return "unknown";
        }
    }

    public Map<String, Object> getLoginAttemptsStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("totalLockedAccounts", lockoutTimes.size());
        status.put("maxAttempts", MAX_LOGIN_ATTEMPTS);
        status.put("lockoutDurationMinutes", LOCKOUT_DURATION_MINUTES);
        
        Map<String, Object> accountStatus = new HashMap<>();
        Map<String, Object> userStatus = new HashMap<>();
        for (Map.Entry<String, Integer> entry : loginAttempts.entrySet()) {
            String username = entry.getKey();
            userStatus.put("attempts", entry.getValue());
            userStatus.put("isLocked", isAccountLocked(username));
            if (lockoutTimes.containsKey(username)) {
                userStatus.put("unlockTime", lockoutTimes.get(username)
                    .plusMinutes(LOCKOUT_DURATION_MINUTES)
                    .format(DateTimeFormatter.ofPattern(DateTimeFormatter_PATTERN)));
            }
            accountStatus.put(username, userStatus);
        }
        status.put("accounts", accountStatus);
        
        return status;
    }

    private static class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
        private final byte[] cachedBody;

        public CachedBodyHttpServletRequest(HttpServletRequest request, byte[] cachedBody) {
            super(request);
            this.cachedBody = cachedBody;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return new CachedBodyServletInputStream(cachedBody);
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(getInputStream()));
        }
    }

    private static class CachedBodyServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream bais;

        public CachedBodyServletInputStream(byte[] cachedBody) {
            this.bais = new ByteArrayInputStream(cachedBody);
        }

        @Override
        public int read() throws IOException {
            return bais.read();
        }

        @Override
        public boolean isFinished() {
            return bais.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException("ReadListener not supported");
        }
    }
}