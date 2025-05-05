package com.example.Finanzmanager.controller;

import com.example.Finanzmanager.model.KryptoEintrag;
import com.example.Finanzmanager.repository.LehrerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lehrer")
public class KryptoController {

    private final LehrerRepository lehrerRepository;

    @Autowired
    public KryptoController(LehrerRepository lehrerRepository) {
        this.lehrerRepository = lehrerRepository;
    }

    @GetMapping
    public List<KryptoEintrag> getAllLehrer() {
        return lehrerRepository.findAll();
    }

    @PostMapping
    public KryptoEintrag addLehrer(@RequestBody KryptoEintrag lehrer) {
        return lehrerRepository.save(lehrer);
    }

    @GetMapping("/{id}")
    public KryptoEintrag getLehrerById(@PathVariable Long id) {
        return lehrerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lehrer nicht gefunden: " + id));
    }
}