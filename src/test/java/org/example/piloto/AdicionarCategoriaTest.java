package org.example.piloto;

import AdicionarCategoria.PaginaBookstoreAdmin;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.SelenideElement;
import org.junit.jupiter.api.*;

import java.time.Duration;

import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Selenide.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AdicionarCategoriaTest {

    @BeforeEach
    public void setUp() {
        Configuration.browser = "chrome";
        Configuration.browserSize = "100%x100%";
        Configuration.browserPosition = "0x0";
        Configuration.timeout = 10000; // Timeout maior para Vaadin
    }

    @Test
    public void adicionarCategoriaComSucesso() {
        PaginaBookstoreAdmin pagina = page(PaginaBookstoreAdmin.class);

        pagina.abrirPagina();
        pagina.loginComoAdmin();
        pagina.abrirAdmin();

        String categoria = "Categoria";
        pagina.adicionarCategoria(categoria);


    }


    @AfterEach
    public void tearDown() {
        closeWebDriver();
    }
}
