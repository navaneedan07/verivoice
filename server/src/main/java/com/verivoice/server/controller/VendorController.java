package com.verivoice.server.controller;

import com.verivoice.server.entity.Vendor;
import com.verivoice.server.repository.VendorRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class VendorController {
    private final VendorRepository vendorRepository;

    public VendorController(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    @GetMapping("/vendors")
    public List<Vendor> listVendors() {
        return vendorRepository.findAll();
    }
}

