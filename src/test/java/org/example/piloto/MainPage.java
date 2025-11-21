package org.example.piloto;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;

// page_url = https://www.jetbrains.com/
public class MainPage {
    private final WebDriver driver;

    // Expor localizadores para uso com WebDriverWait nos testes
    public final By seeDeveloperToolsBy = By.xpath("//*[@data-test-marker='Developer Tools']");
    public final By findYourToolsBy = By.xpath("//*[@data-test='suggestion-action']");
    public final By toolsMenuBy = By.xpath("//div[@data-test='main-menu-item' and @data-test-marker = 'Developer Tools']");
    public final By searchButtonBy = By.cssSelector("[data-test='site-header-search-action']");

    public MainPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // Métodos utilitários simples
    public WebElement getSearchButton() {
        return driver.findElement(searchButtonBy);
    }

    public WebElement getToolsMenu() {
        return driver.findElement(toolsMenuBy);
    }

    public WebElement getSeeDeveloperToolsButton() {
        return driver.findElement(seeDeveloperToolsBy);
    }

    public WebElement getFindYourToolsButton() {
        return driver.findElement(findYourToolsBy);
    }
}
