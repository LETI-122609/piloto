package org.example.piloto;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class InteractionTests {
    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    public void setUp() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*", "--no-sandbox", "--disable-dev-shm-usage");
        // options.addArguments("--headless=new"); // uncomment for headless runs
        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void checkboxesToggle() {
        driver.get("https://the-internet.herokuapp.com/checkboxes");

        // wait for the checkbox container
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#checkboxes")));

        List<WebElement> boxes = driver.findElements(By.cssSelector("#checkboxes input[type='checkbox']"));
        assertTrue(boxes.size() >= 2, "Expected at least two checkboxes on the page");

        WebElement first = boxes.get(0);
        WebElement second = boxes.get(1);

        // on the demo site, typically first is unchecked and second is checked
        boolean firstInitial = first.isSelected();
        boolean secondInitial = second.isSelected();

        // toggle both
        first.click();
        second.click();

        // after toggle, the states should be inverted compared to initial
        assertEquals(!firstInitial, first.isSelected(), "First checkbox should have been toggled");
        assertEquals(!secondInitial, second.isSelected(), "Second checkbox should have been toggled");
    }

    @Test
    public void dropdownSelect() {
        driver.get("https://the-internet.herokuapp.com/dropdown");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("dropdown")));

        WebElement dropdownEl = driver.findElement(By.id("dropdown"));
        Select dropdown = new Select(dropdownEl);

        // verify options exist
        assertTrue(dropdown.getOptions().size() >= 2, "Expected at least two options in the dropdown");

        // select by visible text and assert
        dropdown.selectByVisibleText("Option 2");
        assertEquals("2", dropdown.getFirstSelectedOption().getAttribute("value"), "Option 2 should be selected");

        // select by value and assert
        dropdown.selectByValue("1");
        assertEquals("1", dropdown.getFirstSelectedOption().getAttribute("value"), "Option 1 should be selected");
    }

    @Test
    public void fileUpload() throws Exception {
        // create a small temporary file to upload
        Path tempFile = Files.createTempFile("upload-test-", ".txt");
        Files.writeString(tempFile, "Teste de upload - conteúdo\n");
        tempFile.toFile().deleteOnExit();

        driver.get("https://the-internet.herokuapp.com/upload");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("file-upload")));

        WebElement uploadInput = driver.findElement(By.id("file-upload"));
        // send absolute path to the file input
        uploadInput.sendKeys(tempFile.toAbsolutePath().toString());

        driver.findElement(By.id("file-submit")).click();

        // after submit, the page shows the uploaded file name in #uploaded-files
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("uploaded-files")));
        String uploadedName = driver.findElement(By.id("uploaded-files")).getText();

        assertEquals(tempFile.getFileName().toString(), uploadedName, "Uploaded filename should be displayed");
    }
}
