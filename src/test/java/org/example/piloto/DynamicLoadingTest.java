package org.example.piloto;

import org.example.pages.DynamicLoadingPage;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.*;

import java.time.Duration;

public class DynamicLoadingTest {

    private WebDriver driver;
    private WebDriverWait wait;
    private DynamicLoadingPage page;

    @BeforeEach
    public void setUp() {
        driver = new ChromeDriver();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        page = new DynamicLoadingPage(driver);
        page.abrirPagina();
    }

    @AfterEach
    public void tearDown() {
        driver.quit();
    }

    @Test
    public void verificarTextoAposCarregamento() {
        // Clicar no botão Start
        driver.findElement(page.startButtonBy).click();

        // Esperar que o "loading" desapareça
        wait.until(ExpectedConditions.invisibilityOfElementLocated(page.loadingBy));

        // Esperar que o texto final fique visível
        WebElement textoFinal = wait.until(
                ExpectedConditions.visibilityOfElementLocated(page.finishTextBy)
        );

        // Validar texto
        Assertions.assertEquals("Hello World!", textoFinal.getText());
    }
}
