package org.example.piloto;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

// page_url = https://www.jetbrains.com/
public class MainPage {
    private final WebDriver driver;

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
}