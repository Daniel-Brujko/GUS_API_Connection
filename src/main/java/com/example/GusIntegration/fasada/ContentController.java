package com.example.GusIntegration.fasada;


import com.example.GusIntegration.Entity.RequestDTO;
import com.example.GusIntegration.Services.GusService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1/gus")
@RequiredArgsConstructor
@RestController
public class ContentController {

    private final GusService gusService;

    @PostMapping("/getData")
    public ResponseEntity<?> getData(@RequestBody RequestDTO httprequest, HttpServletResponse httpResponse) {
        return gusService.getAlldata(httprequest.type, httprequest.registryNumber);
    }

}
