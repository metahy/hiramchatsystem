package com.hiramchatsystem.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class ChatController {
    @RequestMapping("/chat")
    public String  getUser() {
        return "/index";
    }
}
