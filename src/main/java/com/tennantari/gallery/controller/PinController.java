package com.tennantari.gallery.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PinController {

    @Value("${gallery.access.pin}")
    private String accessPin;

    @GetMapping("/pin")
    public String pinForm() {
        return "pin";
    }

    @PostMapping("/pin")
    public String submitPin(@RequestParam String pin, HttpServletRequest request, RedirectAttributes ra) {
        if (accessPin == null || accessPin.isEmpty() || accessPin.equals(pin)) {
            HttpSession session = request.getSession(true);
            session.setAttribute("pinVerified", true);
            String returnUrl = (String) session.getAttribute("pinReturnUrl");
            session.removeAttribute("pinReturnUrl");
            return "redirect:" + (returnUrl != null ? returnUrl : "/");
        }
        ra.addFlashAttribute("error", "Incorrect PIN");
        return "redirect:/pin";
    }
}
