package com.abhishek.authify.service;

import com.abhishek.authify.entity.UserEntity;
import com.abhishek.authify.io.ProfileRequest;
import com.abhishek.authify.io.ProfileResponse;
import com.abhishek.authify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Override
    public ProfileResponse createProfile(ProfileRequest request) {
        UserEntity newProfile= convertToUserEntity(request);
        if (!userRepository.existsByEmail(request.getEmail())) {
            newProfile=userRepository.save(newProfile);
            return convertToProfileResponse(newProfile);
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
    }

    @Override
    public ProfileResponse getProfile(String email) {
        UserEntity existingUser  = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Username not found"));

        return convertToProfileResponse(existingUser);
    }

    @Override
    public void sendResetOtp(String email) {
        UserEntity existingEntity=userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("Username not found"));

        String otp= String.valueOf(ThreadLocalRandom.current().nextInt(10000,99999));

        long expiryTime = System.currentTimeMillis() + (15*60*1000);

        existingEntity.setResetOtp(otp);
        existingEntity.setResetOtpExpireAt(expiryTime);

        userRepository.save(existingEntity);

        try {
            emailService.sendResetOtpEmail(existingEntity.getEmail(), otp);
        }catch (Exception e){
            throw new RuntimeException("Unable to send mail");
        }

    }

    @Override
    public void resetPassword(String email, String otp, String newpassword) {
        UserEntity existingUser=userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("Username not found"));

        if (existingUser.getResetOtp() == null || !existingUser.getResetOtp().equals(otp)) {
            throw new RuntimeException("Invalid reset otp");
        }

        if (existingUser.getResetOtpExpireAt() < System.currentTimeMillis()) {
            throw new RuntimeException("Otp expired");
        }

        existingUser.setPassword(passwordEncoder.encode(newpassword));
        existingUser.setResetOtp(null);
        existingUser.setResetOtpExpireAt(0L);

        userRepository.save(existingUser);
    }

    @Override
    public void sendOtp(String email) {
        UserEntity existingUser=userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("Username not found"));
        if (existingUser.getIsAccountVerified() != null && !existingUser.getIsAccountVerified()) {
            return;
        }

        String otp= String.valueOf(ThreadLocalRandom.current().nextInt(10000,99999));

        long expiryTime = System.currentTimeMillis() + (24*60*60*1000);

        existingUser.setVerifyOtp(otp);
        existingUser.setVerfyOtpExpireAt(expiryTime);

        userRepository.save(existingUser);

        try {
            emailService.sendOtpEmail(existingUser.getEmail(), otp);
        }catch (Exception e){
            throw new RuntimeException("Unable to send mail");
        }

    }

    @Override
    public void verifyOtp(String email, String otp) {
        UserEntity existingUser=userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("Username not found"));
        if (existingUser.getVerifyOtp() == null || !existingUser.getVerifyOtp().equals(otp)) {
            throw new RuntimeException("Invalid verfy otp");
        }
        if (existingUser.getVerfyOtpExpireAt() < System.currentTimeMillis()) {
            throw new RuntimeException("Otp expired");
        }
        existingUser.setIsAccountVerified(true);
        existingUser.setVerifyOtp(null);
        existingUser.setVerfyOtpExpireAt(0L);

        userRepository.save(existingUser);
    }


    private ProfileResponse convertToProfileResponse(UserEntity newProfile) {
        return ProfileResponse.builder()
                .name(newProfile.getName())
                .email(newProfile.getEmail())
                .userId(newProfile.getUserId())
                .isAccountVerified(newProfile.getIsAccountVerified())
                .build();
    }

    private UserEntity convertToUserEntity(ProfileRequest request) {
        return UserEntity.builder()
                .email(request.getEmail())
                .userId(UUID.randomUUID().toString())
                .name(request.getName())
                .password(passwordEncoder.encode(request.getPassword()))
                .isAccountVerified(false)
                .resetOtpExpireAt(0L)
                .verifyOtp(null)
                .verfyOtpExpireAt(0L)
                .resetOtp(null)
                .build();
    }
}


