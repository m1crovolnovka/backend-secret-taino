package com.taino.image;

import com.taino.security.SecurityUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/me/images")
public class MeImageController {

    private final ImageUploadService imageUploadService;

    public MeImageController(ImageUploadService imageUploadService) {
        this.imageUploadService = imageUploadService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImageUploadService.ImageResponse upload(@RequestPart("file") MultipartFile file) {
        return imageUploadService.uploadForUser(SecurityUtils.currentUser().userId(), file);
    }
}
