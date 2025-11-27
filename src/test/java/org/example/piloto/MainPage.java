package org.example.piloto;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

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
    public MainPage(WebDriver driver) {
        this.driver = driver;
    }

    // Try a couple of selectors / fallbacks to increase resilience when the site changes slightly.
    public WebElement getSeeDeveloperToolsButton() {
        try {
            return driver.findElement(By.cssSelector("[data-test-marker='Developer Tools']"));
        } catch (Exception e1) {
            try {
                return driver.findElement(By.xpath("//*[contains(.,'Developer Tools')]") );
            } catch (Exception e2) {
                throw new NoSuchElementException("seeDeveloperToolsButton not found using known selectors");
            }
        }
    }

    public WebElement getFindYourToolsButton() {
        try {
            return driver.findElement(By.cssSelector("[data-test='suggestion-action']"));
        } catch (Exception e1) {
            try {
                return driver.findElement(By.xpath("//*[contains(.,'Find your') or contains(.,'Find')]") );
            } catch (Exception e2) {
                throw new NoSuchElementException("findYourToolsButton not found using known selectors");
            }
        }
    }

    public WebElement getToolsMenu() {
        try {
            return driver.findElement(By.cssSelector("div[data-test='main-menu-item'][data-test-marker='Developer Tools']"));
        } catch (Exception e1) {
            try {
                return driver.findElement(By.xpath("//div[contains(@class,'main-menu') and contains(.,'Developer Tools')]") );
            } catch (Exception e2) {
                throw new NoSuchElementException("toolsMenu not found using known selectors");
            }
        }
    }

    public WebElement getSearchButton() {
        try {
            return driver.findElement(By.cssSelector("[data-test='site-header-search-action']"));
        } catch (Exception e1) {
            try {
                return driver.findElement(By.cssSelector("button[aria-label='Search']"));
            } catch (Exception e2) {
                throw new NoSuchElementException("searchButton not found using known selectors");
            }
        }
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
