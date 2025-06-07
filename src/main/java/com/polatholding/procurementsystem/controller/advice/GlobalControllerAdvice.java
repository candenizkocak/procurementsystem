package com.polatholding.procurementsystem.controller.advice;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    /**
     * This method adds the current request's URI to the model for every request.
     * This allows us to access it in Thymeleaf templates for things like
     * determining the active navigation link.
     *
     * @param request The HttpServletRequest object, automatically provided by Spring.
     * @return The URI string of the current request.
     */
    @ModelAttribute("currentUrl")
    public String getCurrentUrl(HttpServletRequest request) {
        return request.getRequestURI();
    }
}