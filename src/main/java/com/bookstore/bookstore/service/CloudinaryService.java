package com.bookstore.bookstore.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.bookstore.bookstore.exception.CloudinaryException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private static final String FOLDER = "bookstore/covers";

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public String uploadCover(MultipartFile file, String publicId) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", FOLDER,
                            "public_id", publicId,
                            "overwrite", true,
                            "resource_type", "image"
                    )
            );
            return (String) result.get("secure_url");
        } catch (IOException e) {
            throw new CloudinaryException("Failed to upload cover image to Cloudinary", e);
        }
    }

    public void deleteCover(String publicId) {
        try {
            cloudinary.uploader().destroy(FOLDER + "/" + publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            throw new CloudinaryException("Failed to delete cover image from Cloudinary", e);
        }
    }
}
