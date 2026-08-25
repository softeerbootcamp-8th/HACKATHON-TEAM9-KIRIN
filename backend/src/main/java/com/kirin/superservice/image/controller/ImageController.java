package com.kirin.superservice.image.controller;

import com.kirin.superservice.image.dto.response.ImageUploadResponse;
import com.kirin.superservice.image.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @PostMapping
    public ResponseEntity<ImageUploadResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        String imageUrl = imageService.storeImage(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ImageUploadResponse(imageUrl));
    }
}
