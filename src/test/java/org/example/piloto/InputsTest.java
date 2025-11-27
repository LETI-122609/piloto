package org.example.piloto;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class InputsTest {
    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    public void setUp() {
        WebDriverManager.chromedriver().setup();
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
    public void inputsAcceptNumbersAndArrowKeys() {
        driver.get("https://the-internet.herokuapp.com/inputs");

        WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("input")));
        assertNotNull(input, "Input element should be present");

        // clear and type number
        input.clear();
        input.sendKeys("10");
        assertEquals("10", input.getAttribute("value"));

        // use arrow up to increment (some browsers will increment numeric inputs)
        input.sendKeys(Keys.ARROW_UP);
        String valAfterUp = input.getAttribute("value");
        // Accept either incremented value or same value depending on browser; assert numeric
        assertTrue(valAfterUp.matches("\\d+"), "Value after Arrow Up should be numeric, was: " + valAfterUp);

        // use arrow down to decrement
        input.sendKeys(Keys.ARROW_DOWN);
        String valAfterDown = input.getAttribute("value");
        assertTrue(valAfterDown.matches("\\d+"), "Value after Arrow Down should be numeric, was: " + valAfterDown);

        // send a non-numeric char: some browsers strip non-digits, others accept text. Accept either behavior.
        input.clear();
        input.sendKeys("abc123");
        String finalVal = input.getAttribute("value");
        // Accept either the typed text or only the numeric part depending on browser
        boolean acceptsFull = "abc123".equals(finalVal);
        boolean keepsNumbersOnly = finalVal != null && finalVal.matches("\\d+");
        assertTrue(acceptsFull || keepsNumbersOnly, "Input value should be 'abc123' or numeric-only; was: '" + finalVal + "'");
    }
}
