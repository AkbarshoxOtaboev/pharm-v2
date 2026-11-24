package uz.uwon.pharm.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping({
            "/",
            "/login",
            "/admin",
            "/admin/**",
            "/operator",
            "/operator/**",
            "/courier",
            "/courier/**",
            "/viewer",
            "/viewer/**"
    })
    public String index() {
        return "forward:/index.html";
    }
}
