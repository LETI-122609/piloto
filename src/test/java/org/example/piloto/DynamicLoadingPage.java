package org.example.piloto;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class DynamicLoadingPage {

    private WebDriver driver;

    public By startButtonBy = By.cssSelector("#start button");
    public By loadingBy = By.id("loading");
    public By finishTextBy = By.cssSelector("#finish h4");

    public DynamicLoadingPage(WebDriver driver) {
        this.driver = driver;
    }

    public void abrirPagina() {
        driver.get("https://the-internet.herokuapp.com/dynamic_loading/2");
    }
}
