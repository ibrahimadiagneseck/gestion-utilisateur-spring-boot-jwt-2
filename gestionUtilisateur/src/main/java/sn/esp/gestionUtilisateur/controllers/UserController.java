package sn.esp.gestionUtilisateur.controllers;

import static org.springframework.http.HttpStatus.OK;

import java.io.IOException;
import java.util.List;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static sn.esp.gestionUtilisateur.constant.SecurityConstant.*;

import sn.esp.gestionUtilisateur.entities.HttpResponse;
import sn.esp.gestionUtilisateur.entities.User;
import sn.esp.gestionUtilisateur.entities.UserPrincipal;
import sn.esp.gestionUtilisateur.exception.ExceptionHandling;
import sn.esp.gestionUtilisateur.exception.entities.EmailExistException;
import sn.esp.gestionUtilisateur.exception.entities.UserNotFoundException;
import sn.esp.gestionUtilisateur.exception.entities.UsernameExistException;
import sn.esp.gestionUtilisateur.services.UserService;
import sn.esp.gestionUtilisateur.utility.JWTTokenProvider;

import javax.mail.MessagingException;


@RestController
@RequestMapping(path = { "/", "/user"})
public class UserController extends ExceptionHandling {
	
	public static final String EMAIL_SENT = "An email with a new password was sent to: ";
    public static final String USER_DELETED_SUCCESSFULLY = "User deleted successfully";
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JWTTokenProvider jwtTokenProvider;

//    public UserController(AuthenticationManager authenticationManager, UserService userService, JWTTokenProvider jwtTokenProvider) {
//        this.authenticationManager = authenticationManager;
//        this.userService = userService;
//        this.jwtTokenProvider = jwtTokenProvider;
//    }

    @PostMapping("/login")
    public HttpHeaders login(@RequestBody User user) {
    	
    	
        authenticate(user.getUsername(), user.getPassword()); // verrifications
        
        User loginUser = userService.findUserByUsername(user.getUsername());
        UserPrincipal userPrincipal = new UserPrincipal(loginUser);
        
        HttpHeaders jwtHeader = getJwtHeader(userPrincipal);
        
        return jwtHeader;
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody User user) throws UserNotFoundException, UsernameExistException, EmailExistException, MessagingException {
        User newUser = userService.register(user.getFirstName(), user.getLastName(), user.getUsername(), user.getEmail());
        return new ResponseEntity<>(newUser, OK);
    }

    @PostMapping("/add")
    public ResponseEntity<User> addNewUser(@RequestParam("firstName") String firstName,
                                           @RequestParam("lastName") String lastName,
                                           @RequestParam("username") String username,
                                           @RequestParam("email") String email,
                                           @RequestParam("role") String role,
                                           @RequestParam("isActive") String isActive,
                                           @RequestParam("isNonLocked") String isNonLocked,
                                           @RequestParam(value = "profileImage", required = false) MultipartFile profileImage) throws UserNotFoundException, UsernameExistException, EmailExistException, IOException{//, NotAnImageFileException {
        User newUser = userService.addNewUser(firstName, lastName, username,email, role, Boolean.parseBoolean(isNonLocked), Boolean.parseBoolean(isActive), profileImage);
        return new ResponseEntity<>(newUser, OK);
    }

    @PostMapping("/update")
    public ResponseEntity<User> update(@RequestParam("currentUsername") String currentUsername,
                                       @RequestParam("firstName") String firstName,
                                       @RequestParam("lastName") String lastName,
                                       @RequestParam("username") String username,
                                       @RequestParam("email") String email,
                                       @RequestParam("role") String role,
                                       @RequestParam("isActive") String isActive,
                                       @RequestParam("isNonLocked") String isNonLocked,
                                       @RequestParam(value = "profileImage", required = false) MultipartFile profileImage) throws UserNotFoundException, UsernameExistException, EmailExistException, IOException{//, NotAnImageFileException {
        User updatedUser = userService.updateUser(currentUsername, firstName, lastName, username,email, role, Boolean.parseBoolean(isNonLocked), Boolean.parseBoolean(isActive), profileImage);
        return new ResponseEntity<>(updatedUser, OK);
    }
    
    @GetMapping("/find/{username}")
    public ResponseEntity<User> getUser(@PathVariable("username") String username) {
        User user = userService.findUserByUsername(username);
        return new ResponseEntity<>(user, OK);
    }

    @GetMapping("/list")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getUsers();
        return new ResponseEntity<>(users, OK);
    }

//    @GetMapping("/resetpassword/{email}")
//    public ResponseEntity<HttpResponse> resetPassword(@PathVariable("email") String email) throws MessagingException, EmailNotFoundException {
//        userService.resetPassword(email);
//        return response(OK, EMAIL_SENT + email);
//    }

    @DeleteMapping("/delete/{username}")
    @PreAuthorize("hasAnyAuthority('user:delete')")
    public ResponseEntity<HttpResponse> deleteUser(@PathVariable("username") String username) throws IOException {
        userService.deleteUser(username);
        return response(OK, USER_DELETED_SUCCESSFULLY);
    }

//    @PostMapping("/updateProfileImage")
//    public ResponseEntity<User> updateProfileImage(@RequestParam("username") String username, @RequestParam(value = "profileImage") MultipartFile profileImage) throws UserNotFoundException, UsernameExistException, EmailExistException, IOException, NotAnImageFileException {
//        User user = userService.updateProfileImage(username, profileImage);
//        return new ResponseEntity<>(user, OK);
//    }

//    @GetMapping(path = "/image/{username}/{fileName}", produces = IMAGE_JPEG_VALUE)
//    public byte[] getProfileImage(@PathVariable("username") String username, @PathVariable("fileName") String fileName) throws IOException {
//        return Files.readAllBytes(Paths.get(USER_FOLDER + username + FORWARD_SLASH + fileName));
//    }



    private ResponseEntity<HttpResponse> response(HttpStatus httpStatus, String message) {
        return new ResponseEntity<>(new HttpResponse(httpStatus.value(), httpStatus, httpStatus.getReasonPhrase().toUpperCase(),
                message), httpStatus);
    }
    
    private HttpHeaders getJwtHeader(UserPrincipal user) {
        HttpHeaders headers = new HttpHeaders();
        String jwtToken = jwtTokenProvider.generateJwtToken(user);
        
        if (jwtToken != null) {
            headers.add(JWT_TOKEN_HEADER, jwtToken);
            System.out.println("JWT token added to headers: " + jwtToken);
            // System.out.println("headers" + headers);
        } else {
            System.out.println("Failed to generate JWT token for user " + user.getUsername());
        }
        
        return headers;
    }


//    private HttpHeaders getJwtHeader(UserPrincipal user) {
//    	
//        HttpHeaders headers = new HttpHeaders();
//        
//        headers.add(JWT_TOKEN_HEADER, jwtTokenProvider.generateJwtToken(user));
//        
//        return headers;
//    }

    private void authenticate(String username, String password) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
    }

}
