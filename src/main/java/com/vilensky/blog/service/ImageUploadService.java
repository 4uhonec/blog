package com.vilensky.blog.service;

import com.vilensky.blog.entity.ImageModel;
import com.vilensky.blog.entity.Post;
import com.vilensky.blog.entity.User;
import com.vilensky.blog.exceptions.ImageNotFoundException;
import com.vilensky.blog.repository.ImageRepository;
import com.vilensky.blog.repository.PostRepository;
import com.vilensky.blog.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.Principal;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

@Service
public class ImageUploadService {
    public static final Logger LOGGER = LoggerFactory.getLogger(ImageUploadService.class);

    private ImageRepository imageRepository;
    private UserRepository userRepository;
    private PostRepository postRepository;

    @Autowired
    public ImageUploadService(ImageRepository imageRepository, UserRepository userRepository, PostRepository postRepository) {
        this.imageRepository = imageRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
    }

    public ImageModel uploadImageToUser(MultipartFile file, Principal principal) throws IOException{
        User user = getUserByPrincipal(principal);
        LOGGER.info("Uploading image profile to user {}", user.getUsername());

        ImageModel userProfileImage = imageRepository.findByUserId(user.getId()).orElse(null);
        if(!ObjectUtils.isEmpty(userProfileImage)){
            imageRepository.delete(userProfileImage);
        }

        ImageModel imageModel = new ImageModel();
        imageModel.setUserId(user.getId());
        imageModel.setImageBytes(compressBytes(file.getBytes()));
        imageModel.setName(file.getOriginalFilename());
        return imageRepository.save(imageModel);
    }

    public ImageModel uploadImageToPost(MultipartFile file, Principal principal, Long postId) throws IOException{
        User user = getUserByPrincipal(principal);
        Post post = user.getPosts()
                .stream()
                .filter(p -> p.getId().equals(postId))
                .collect(toSinglePostCollector());

        ImageModel imageModel = new ImageModel();
        imageModel.setPostId(post.getId());
        imageModel.setUserId(user.getId());
        imageModel.setImageBytes(compressBytes(file.getBytes()));
        imageModel.setName(file.getOriginalFilename());

        LOGGER.info("Uploading image to post {}", post.getId());

        return imageRepository.save(imageModel);
    }

    public ImageModel getImageToUser(Principal principal){
        User user = getUserByPrincipal(principal);
        ImageModel imageModel = imageRepository.findByUserId(user.getId()).orElse(null);
        if(!ObjectUtils.isEmpty(imageModel)){
            imageModel.setImageBytes(decompressBytes(imageModel.getImageBytes()));
        }

        return imageModel;
    }

    public ImageModel getImageToPost(Long postId){
        ImageModel imageModel = imageRepository.findByPostId(postId)
                .orElseThrow(() -> new ImageNotFoundException("Cannot find image to post " + postId));
        if(!ObjectUtils.isEmpty(imageModel)){
            imageModel.setImageBytes(decompressBytes(imageModel.getImageBytes()));
        }

        return imageModel;
    }

    private byte[] compressBytes(byte[] data){
        Deflater deflater = new Deflater();
        deflater.setInput(data);
        deflater.finish();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        byte[] buffer = new byte[1024];
        while(!deflater.finished()){
            int count = deflater.deflate(buffer);
            outputStream.write(buffer,0, count);
        }
        try{
            outputStream.close();
        }catch (IOException e){
            LOGGER.error("Cannot compress bytes");
        }
        System.out.println("Compressed image byte size: " + outputStream.toByteArray().length);
        return outputStream.toByteArray();
    }

    private static byte[] decompressBytes(byte[] data){
        Inflater inflater = new Inflater();
        inflater.setInput(data);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        byte[] buffer = new byte[1024];
        try{
            while(!inflater.finished()){
                int count = inflater.inflate(buffer);
                outputStream.write(buffer, 0, count);
            }
            outputStream.close();
        }catch(IOException | DataFormatException e){
            LOGGER.error("Cannot decompress bytes");
        }
        return outputStream.toByteArray();
    }

    private User getUserByPrincipal(Principal principal){
        String username = principal.getName();
        return userRepository.findUserByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Username "+ username + " not found"));
    }

    private <T> Collector<T, ?, T> toSinglePostCollector(){
        return Collectors.collectingAndThen(
                Collectors.toList(),
                list -> {
                    if(list.size() != 1){
                        throw new IllegalStateException();
                    }
                    return list.get(0);
                }
        );
    }
}
