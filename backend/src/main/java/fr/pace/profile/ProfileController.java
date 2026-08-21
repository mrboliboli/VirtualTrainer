package fr.pace.profile;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profil")
public class ProfileController {

    private final AthleteProfileService service;

    public ProfileController(AthleteProfileService service) {
        this.service = service;
    }

    @GetMapping
    public ProfileResponse get() {
        return ProfileResponse.from(service.get());
    }

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    public ProfileResponse save(@Valid @RequestBody ProfileRequest request) {
        return ProfileResponse.from(service.save(request.toData()));
    }
}
