package com.abhishek.authify.service;

import com.abhishek.authify.io.ProfileRequest;
import com.abhishek.authify.io.ProfileResponse;

public interface ProfileService {

    ProfileResponse createProfile(ProfileRequest request);
}
