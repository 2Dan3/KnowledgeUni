package com.ftn.research.knowledgeuniverse.controller;

import com.ftn.research.knowledgeuniverse.model.dto.SearchQueryDTO;
import com.ftn.research.knowledgeuniverse.model.index.BookIndex;
import com.ftn.research.knowledgeuniverse.service.SearchService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping(value = "")
@RequiredArgsConstructor
public class PageIndexController {

    @Autowired
    private ServletContext servletContext;

    private String baseURL;

    @PostConstruct
    public void init() { baseURL = servletContext.getContextPath() + "/";}


    @GetMapping(value = "")
    public ModelAndView getIndexPage() {
        return new ModelAndView("index");
    }
}