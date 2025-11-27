package org.example.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;

public class DynamicLoadingPage {

    private final WebDriver driver;

    // URL da página
    public static final String PAGE_URL = "https://the-internet.herokuapp.com/dynamic_loading/2";

    // Localizadores expostos para o teste usar WebDriverWait
    public final By startButtonBy = By.cssSelector("#start button");
    public final By loadingBy = By.id("loading");
    public final By finishTextBy = By.id("finish");

    public DynamicLoadingPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // Navegação
    public void abrirPagina() {
        driver.get(PAGE_URL);
    }
}
