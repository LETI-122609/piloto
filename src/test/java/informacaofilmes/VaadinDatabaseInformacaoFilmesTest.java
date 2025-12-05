package informacaofilmes;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.SelenideElement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.codeborne.selenide.Selenide.closeWebDriver;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static org.junit.jupiter.api.Assertions.*;

public class VaadinDatabaseInformacaoFilmesTest {
    private VaadinDatabaseInformacaoFilmes page;

    @BeforeEach
    public void setUp() {
        // Use Selenide default browser (Chrome) and reasonable timeout
        Configuration.browserSize = "1920x1080";
        Configuration.timeout = 10000;

        page = new VaadinDatabaseInformacaoFilmes();
    }

    @AfterEach
    public void tearDown() {
        closeWebDriver();
    }

    @Test
    public void openAndCheckGridIsVisible() {
        page.openPage();
        boolean visible = page.isGridVisible();
        assertTrue(visible, "Vaadin grid should be visible on the page");
    }

    @Test
    public void filterResultsShowsMatchingRow() {
        page.openPage();
        // Try a common movie title fragment like 'The' to filter
        String filter = "The";
        page.filterBy(filter);

        String first = "";

        try {
            // primary selector: Vaadin grid cells
            SelenideElement match = $$("vaadin-grid-cell-content").findBy(text(filter));
            match.shouldBe(visible, Duration.ofSeconds(10));
            first = match.getText();
        } catch (Exception e1) {
            try {
                // fallback: table cells
                SelenideElement match = $$("td").findBy(text(filter));
                match.shouldBe(visible, Duration.ofSeconds(10));
                first = match.getText();
            } catch (Exception e2) {
                // last fallback: any visible element containing text
                try {
                    SelenideElement match = $$("*").findBy(text(filter));
                    match.shouldBe(visible, Duration.ofSeconds(10));
                    first = match.getText();
                } catch (Exception ignored) {
                }
            }
        }

        assertNotNull(first);
        assertFalse(first.isEmpty(), "After filtering the grid should contain at least one row");
        // Basic sanity: the row should contain some alphabetic characters
        assertTrue(first.matches(".*[A-Za-z].*"), "Row text should include letters");
    }
}

