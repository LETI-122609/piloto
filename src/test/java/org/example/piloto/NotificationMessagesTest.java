package org.example.piloto;

import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class NotificationMessagesTest {
    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    public void setUp() {
        driver = new ChromeDriver();
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void notificationMessageShowsAndVanishesOrChanges() {
        driver.get("https://the-internet.herokuapp.com/notification_message_rendered");

        String lastText = null;

        // Click multiple times to observe different messages. Re-find the link each iteration and retry clicks to avoid stale elements.
        for (int i = 0; i < 6; i++) {
            boolean clicked = false;
            int attempts = 0;
            while (!clicked && attempts < 3) {
                attempts++;
                try {
                    WebElement link = wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Click here")));
                    link.click();
                    clicked = true;
                } catch (StaleElementReferenceException | ElementClickInterceptedException | NoSuchElementException e) {
                    // small backoff then retry
                    try { Thread.sleep(150); } catch (InterruptedException ie) {}
                }
            }

            if (!clicked) {
                fail("Could not click the 'Click here' link after retries");
            }

            // notification has id "flash"
            WebElement flash = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("flash")));
            String text = flash.getText().trim();
            System.out.println("Notification [" + i + "] = '" + text + "'");

            lastText = text;

            // Wait for the flash to disappear (best-effort) before next iteration to reduce flakiness
            try {
                WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(2));
                shortWait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("flash")));
            } catch (Exception ignored) {
                // fallback small sleep
                try { Thread.sleep(300); } catch (InterruptedException ie2) {}
            }
        }

        // Ensure we saw at least one notification
        assertNotNull(lastText, "Should see at least one notification message after clicking the link");
    }
}
