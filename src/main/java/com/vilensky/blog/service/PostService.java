package com.vilensky.blog.service;

import com.vilensky.blog.dto.PostDTO;
import com.vilensky.blog.entity.ImageModel;
import com.vilensky.blog.entity.Post;
import com.vilensky.blog.entity.User;
import com.vilensky.blog.exceptions.PostNotFoundException;
import com.vilensky.blog.repository.ImageRepository;
import com.vilensky.blog.repository.PostRepository;
import com.vilensky.blog.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Service
public class PostService {
    public static final Logger LOGGER = LoggerFactory.getLogger(PostService.class);

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ImageRepository imageRepository;

    @Autowired
    public PostService(PostRepository postRepository, UserRepository userRepository, ImageRepository imageRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.imageRepository = imageRepository;
    }

    public Post createPost(PostDTO postDTO, Principal principal){
        User user = getUserByPrincipal(principal);
        Post post = new Post();
        post.setUser(user);
        post.setCaption(postDTO.getCaption());
        post.setLocation(postDTO.getLocation());
        post.setTitle(postDTO.getTitle());
        post.setLikes(0);

        LOGGER.info("Saving post for user: {}", user.getEmail());
        return postRepository.save(post);
    }

    public List<Post> getAllPosts(){
        return postRepository.findAllByOrderByCreatedDateDesc();
    }

    public Post getPostById(Long postId, Principal principal){
        User user = getUserByPrincipal(principal);
        return postRepository.findPostByIdAndUser(postId, user)
                .orElseThrow(() -> new PostNotFoundException("Post cannot be found for username: " + user.getEmail()));
    }

    public List<Post> getAllPostsForUser(Principal principal){
        User user = getUserByPrincipal(principal);
        return postRepository.findAllByUserOrderByCreatedDateDesc(user);
    }

    public Post likePost(Long postId, String username){
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post cannot be found"));

        //check if %username already liked this post
        Optional<String> userLiked = post.getLikedUsers()
                .stream()
                .filter(u -> u.equals(username))
                .findAny();

        //if true, removing like from this user
        if(userLiked.isPresent()){
            post.setLikes(post.getLikes() - 1);
            post.getLikedUsers().remove(username);
        }else{
            post.setLikes(post.getLikes() + 1);
            post.getLikedUsers().add(username);
        }
        return postRepository.save(post);
    }

    public void deletePost(Long postId, Principal principal){
        Post post = getPostById(postId, principal);
        Optional<ImageModel> imageModel = imageRepository.findByPostId(post.getId());
        postRepository.delete(post);
        imageModel.ifPresent(imageRepository::delete);
    }

    private User getUserByPrincipal(Principal principal){
        String username = principal.getName();
        return userRepository.findUserByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Username "+ username + " not found"));
    }

}
