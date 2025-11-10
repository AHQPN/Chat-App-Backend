package org.example.chatapp.controller;

import lombok.RequiredArgsConstructor;
import org.example.chatapp.service.impl.MessageInteractionService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/msginteractions")
@RequiredArgsConstructor
public class MessageInteractionController {
    private final MessageInteractionService messageInteractionService;






}
