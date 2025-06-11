package com.polatholding.procurementsystem.controller;

import com.polatholding.procurementsystem.dto.PurchaseRequestDetailDto;
import com.polatholding.procurementsystem.dto.PurchaseRequestFormDto;
import com.polatholding.procurementsystem.service.PurchaseRequestService;
import com.polatholding.procurementsystem.service.RequestHistoryService;
import com.polatholding.procurementsystem.service.FileService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import java.nio.file.Path;
import java.util.List;

import java.security.Principal;

@Controller
@RequestMapping("/requests")
public class PurchaseRequestController {

    private final PurchaseRequestService purchaseRequestService;
    private final RequestHistoryService requestHistoryService;
    private final FileService fileService;

    public PurchaseRequestController(PurchaseRequestService purchaseRequestService,
                                     RequestHistoryService requestHistoryService,
                                     FileService fileService) {
        this.purchaseRequestService = purchaseRequestService;
        this.requestHistoryService = requestHistoryService;
        this.fileService = fileService;
    }

    @GetMapping("/new")
    @PreAuthorize("!hasRole('Auditor')")
    public String showNewRequestForm(Model model, Principal principal) {
        model.addAttribute("formData", purchaseRequestService.getNewRequestFormData(principal.getName()));
        model.addAttribute("requestForm", new PurchaseRequestFormDto());
        // For the form title and action URL
        model.addAttribute("isEditMode", false);
        return "request-form";
    }

    @PostMapping("/save")
    @PreAuthorize("!hasRole('Auditor')")
    public String saveNewRequest(@ModelAttribute("requestForm") PurchaseRequestFormDto formDto,
                                 @RequestParam(value = "files", required = false) List<MultipartFile> files,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        try {
            purchaseRequestService.saveNewRequest(formDto, principal.getName(), files);
            redirectAttributes.addFlashAttribute("successMessage", "Purchase Request created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating request: " + e.getMessage());
            return "redirect:/requests/new";
        }
        return "redirect:/dashboard";
    }

    /**
     * NEW: Displays the request form in edit mode, pre-filled with data.
     */
    @GetMapping("/{id}/edit")
    @PreAuthorize("!hasRole('Auditor')")
    public String showEditRequestForm(@PathVariable("id") Integer id, Model model, Principal principal) {
        // Security check: Ensure the user is the creator of the request
        PurchaseRequestDetailDto requestDetails = purchaseRequestService.getRequestDetailsById(id);
        if (!requestDetails.getCreatorFullName().equals(purchaseRequestService.getUserFullName(principal.getName()))) {
            throw new AccessDeniedException("You are not authorized to edit this request.");
        }

        model.addAttribute("formData", purchaseRequestService.getNewRequestFormData(principal.getName()));
        model.addAttribute("requestForm", purchaseRequestService.getRequestFormById(id));
        model.addAttribute("isEditMode", true);
        model.addAttribute("requestId", id);
        return "request-form";
    }

    /**
     * NEW: Processes the form submission for an existing request.
     */
    @PostMapping("/{id}/update")
    @PreAuthorize("!hasRole('Auditor')")
    public String updateRequest(@PathVariable("id") Integer id,
                                @ModelAttribute("requestForm") PurchaseRequestFormDto formDto,
                                @RequestParam(value = "files", required = false) List<MultipartFile> files,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        try {
            purchaseRequestService.updateRequest(id, formDto, principal.getName(), files);
            redirectAttributes.addFlashAttribute("successMessage", "Request #" + id + " has been updated and resubmitted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating request: " + e.getMessage());
            return "redirect:/requests/" + id + "/edit";
        }
        return "redirect:/dashboard";
    }

    @GetMapping("/{id}")
    public String viewRequestDetails(@PathVariable("id") Integer id, Model model) {
        PurchaseRequestDetailDto requestDetails = purchaseRequestService.getRequestDetailsById(id);
        model.addAttribute("request", requestDetails);
        return "request-details";
    }


    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable("fileId") Integer fileId) throws Exception {
        Resource resource = fileService.loadAsResource(fileId);
        String filename = Path.of(resource.getFilename()).getFileName().toString();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .body(resource);
    }
}