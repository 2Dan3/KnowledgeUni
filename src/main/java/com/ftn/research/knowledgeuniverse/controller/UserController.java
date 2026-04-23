package com.ftn.research.knowledgeuniverse.controller;

import com.ftn.research.knowledgeuniverse.model.dto.*;
import com.ftn.research.knowledgeuniverse.model.entity.Genre;
import com.ftn.research.knowledgeuniverse.model.entity.User;
import com.ftn.research.knowledgeuniverse.service.FileService;
import com.ftn.research.knowledgeuniverse.service.GenreService;
import com.ftn.research.knowledgeuniverse.service.IndexingService;
import com.ftn.research.knowledgeuniverse.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.*;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private static final String USER_KEY = "logged_user";

//    private final IndexingService indexingService;
//    private final FileService fileService;
    private final GenreService genreService;
    private final UserService userService;

    @GetMapping("/{id}/genres")
    public ResponseEntity<List<GenreResponseDTO>> getFavoriteGenresForUser(@PathVariable(name = "id") Long userId) {

        Optional<User> foundUser = userService.findById(userId);
        if (foundUser.isEmpty())
            return ResponseEntity.notFound().build();

        List<Genre> favoriteGenresFound = genreService.findFavoriteGenresForUser(foundUser.get());

        List<GenreResponseDTO> genreDTOs = favoriteGenresFound.stream().map(GenreResponseDTO::new).toList();

        return ResponseEntity.ok().body(genreDTOs);
    }

    @GetMapping(value = "/register")
    public ModelAndView getRegistrationPage(){

        ModelAndView mov = new ModelAndView("register");
        return mov;
    }

    @PostMapping(consumes = "application/json", value = "/register")
    public ResponseEntity<UserResponseDTO> registerNewUser(@Valid @RequestBody UserCreationDTO newUser){

        if (userService.isEmailTaken(newUser.getEmail()))
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);

        User createdUser = userService.createUser(newUser);

        if(createdUser == null)
            return new ResponseEntity<>(null, HttpStatus.NOT_ACCEPTABLE);

        return new ResponseEntity<>(new UserResponseDTO(createdUser), HttpStatus.CREATED);
    }

    @GetMapping(value="/login")
    public ModelAndView login() {

        ModelAndView mov = new ModelAndView("login");
        return mov;
    }

    @PostMapping(value="/login")
    public ModelAndView postLogin(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session, HttpServletResponse response, Locale locale) throws IOException {
        try {
            Optional<User> user = userService.findByCredentials(email, password);
            if (user.isEmpty())
                throw new Exception(ResourceBundle.getBundle("messages.messages", locale).getString("errors.credentialsValid"));

            session.setAttribute(UserController.USER_KEY, user.get());

            return new ModelAndView("index");
//            response.sendRedirect(baseURL);
        } catch (Exception ex) {
            String poruka = ex.getMessage();
//            if ("" == poruka) {
            if ("".equals(poruka))
                poruka = ResourceBundle.getBundle("messages.messages", locale).getString("errors.loginSuccess");


            ModelAndView retval = new ModelAndView("login");
            retval.addObject("poruka", poruka);

            return retval;
        }
    }

    @GetMapping(value="/logout")
    public void logout(HttpSession session, HttpServletResponse response) {
        session.invalidate();

        response.sendRedirect(baseURL);
    }


//    @GetMapping("/{id}")
//    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
//    public ResponseEntity<UserDTO> getUser(@PathVariable Long id) {
//        User foundUser = this.userService.findById(id);
//
//        if (foundUser == null) {
//            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
//        }
//        return new ResponseEntity<UserDTO>(new UserDTO(foundUser), HttpStatus.OK);
//    }

}