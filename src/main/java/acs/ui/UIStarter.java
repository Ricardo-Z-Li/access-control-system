package acs.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class UIStarter implements CommandLineRunner {
    
    @Autowired
    private MainApp mainApp;
    
    @Override
    public void run(String... args) {
        mainApp.showUI();
    }
}