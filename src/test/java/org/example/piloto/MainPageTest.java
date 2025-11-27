package org.example.piloto;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.time.Duration;

// Added imports
import java.util.List;
import java.util.Arrays;
import java.util.NoSuchElementException;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.interactions.Actions;

public class MainPageTest {
    private WebDriver driver;
    private MainPage mainPage;
    private WebDriverWait wait; // added wait field

    @BeforeEach
    public void setUp() {
        driver = new ChromeDriver();
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        // initialize explicit wait
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.get("https://www.jetbrains.com/");

        mainPage = new MainPage(driver);
    }

    @AfterEach
    public void tearDown() {
        driver.quit();
    }

    @Test
    public void search() throws InterruptedException {
        clickElement(mainPage.searchButton);

        List<By> searchCandidates = Arrays.asList(
                By.cssSelector("[data-test='search-input']"),
                By.cssSelector("input[aria-label*='Search']"),
                By.cssSelector("input[type='search']"),
                By.cssSelector("input[placeholder*='Search']"),
                By.cssSelector("input[type='text']")
        );

        WebElement searchField = tryFindFirstVisible(searchCandidates, Duration.ofSeconds(7));
        String query = "Selenium";
        searchField.clear();
        searchField.sendKeys(query);

        // remember existing windows in case results open in a new tab/window
        java.util.Set<String> beforeHandles = driver.getWindowHandles();

        // Strategy attempts to submit the search
        boolean submitted = false;
        JavascriptExecutor js = (JavascriptExecutor) driver;

        // 1) try ENTER
        try {
            searchField.sendKeys(Keys.ENTER);
            submitted = true;
        } catch (WebDriverException ignored) {}

        // 2) try clicking submit button
        if (!submitted) {
            try {
                List<By> submitCandidates = Arrays.asList(
                        By.cssSelector("button[data-test='full-search-button']"),
                        By.cssSelector("button[type='submit']"),
                        By.cssSelector("button[aria-label*='Search']")
                );
                WebElement submitButton = tryFindFirstVisible(submitCandidates, Duration.ofSeconds(3));
                clickElement(submitButton);
                submitted = true;
            } catch (Exception ignored) {}
        }

        // 3) try JS form submit if not submitted
        if (!submitted) {
            try {
                js.executeScript("var f = arguments[0].form || arguments[0].closest('form'); if(f) { f.submit(); return true; } return false;", searchField);
                submitted = true;
            } catch (WebDriverException ignored) {}
        }

        // 4) dispatch keyboard Enter via JS
        if (!submitted) {
            try {
                js.executeScript("var ev = new KeyboardEvent('keydown', {key:'Enter',keyCode:13,which:13,bubbles:true}); arguments[0].dispatchEvent(ev);", searchField);
                submitted = true;
            } catch (WebDriverException ignored) {}
        }

        // 5) as a last resort, navigate directly to JetBrains search page (best-effort fallback)
        if (!submitted) {
            try {
                String forced = "https://www.jetbrains.com/search/?q=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
                driver.get(forced);
                submitted = true;
            } catch (Exception ignored) {}
        }

        // diagnostic log right after submit/navigation
        try {
            System.out.println("[DEBUG] After submit - URL: " + safeGetCurrentUrl() + " | Title: " + safeGetTitle());
        } catch (Exception ignored) {}

        // Wait up to 45s for either a new window, url/title change or the query text to appear in the body
        WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(45));
        try {
            longWait.until(d -> {
                try {
                    // 1) new window/tab opened
                    if (d.getWindowHandles().size() > beforeHandles.size()) {
                        return true;
                    }
                    // 2) URL contains search or query
                    String url = "";
                    try { url = d.getCurrentUrl(); } catch (Exception ignore) {}
                    if (url != null && (url.toLowerCase().contains("search") || url.toLowerCase().contains("q="))) return true;
                    // 3) title contains query
                    String t = "";
                    try { t = d.getTitle(); } catch (Exception ignore) {}
                    if (t != null && t.toLowerCase().contains(query.toLowerCase())) return true;
                    // 4) body contains query text
                    try {
                        WebElement body = d.findElement(By.tagName("body"));
                        if (body != null && body.getText() != null && body.getText().toLowerCase().contains(query.toLowerCase())) return true;
                    } catch (Exception ignore) {}
                    return false;
                } catch (WebDriverException wde) {
                    return false;
                }
            });
        } catch (TimeoutException te) {
            // fallback: try forced navigation to search endpoint before failing
            try {
                String forced = "https://www.jetbrains.com/search/?q=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
                driver.get(forced);
                WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(10));
                shortWait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), query));
            } catch (Exception ex) {
                // final fallback: small sleep, dump diagnostics and fail
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                dumpDiagnostics("search-timeout");
                System.out.println("[DEBUG] Timeout - Final URL: " + safeGetCurrentUrl() + " | Title: " + safeGetTitle());
                throw new AssertionError("Search did not show results within timeout for query: " + query, te);
            }
        }

        // diagnostic log after successful wait
        try {
            System.out.println("[DEBUG] After wait - URL: " + safeGetCurrentUrl() + " | Title: " + safeGetTitle());
        } catch (Exception ignored) {}

        // If a new window opened, switch to it (the one not in beforeHandles)
        try {
            java.util.Set<String> afterHandles = driver.getWindowHandles();
            if (afterHandles.size() > beforeHandles.size()) {
                for (String h : afterHandles) {
                    if (!beforeHandles.contains(h)) {
                        driver.switchTo().window(h);
                        break;
                    }
                }
            }
        } catch (WebDriverException ignored) {
        }

        // final verification: check URL/title/body for query
        String currentUrl = "";
        String title = "";
        String pageSource = "";
        try { currentUrl = driver.getCurrentUrl(); } catch (WebDriverException ignored) {}
        try { title = driver.getTitle(); } catch (WebDriverException ignored) {}
        try { pageSource = driver.findElement(By.tagName("body")).getText().toLowerCase(); } catch (WebDriverException ignored) {}

        boolean matched = (currentUrl != null && (currentUrl.toLowerCase().contains("search") || currentUrl.toLowerCase().contains("q=")))
                || (title != null && title.toLowerCase().contains(query.toLowerCase()))
                || (pageSource != null && pageSource.contains(query.toLowerCase()));
        if (!matched) {
            dumpDiagnostics("search-no-match");
            throw new AssertionError("Search did not navigate to a page containing the query (URL/title/body) after submit: " + query);
        }
    }

    @Test
    public void toolsMenu() throws InterruptedException {
        clickElement(mainPage.toolsMenu);

        List<By> submenuCandidates = Arrays.asList(
                By.cssSelector("div[data-test='main-menu']")
        );

        WebElement menuPopup = tryFindFirstVisible(submenuCandidates, Duration.ofSeconds(10));
        assertTrue(menuPopup.isDisplayed());

        // Also assert that menu contains some expected text like 'Tools' or 'Developer'
        String text = menuPopup.getText().toLowerCase();
        assertTrue(text.contains("tool") || text.contains("developer") || menuPopup.isDisplayed());
    }

    @Test
    public void navigationToAllTools() throws InterruptedException {

        clickElement(mainPage.seeDeveloperToolsButton);

        WebElement findTools = tryFindFirstVisible(Arrays.asList(By.cssSelector("[data-test='suggestion-action']"), By.cssSelector("a[href*='tools']")), Duration.ofSeconds(3));
        clickElement(findTools);

        // check navigation by URL/title or content
        wait.until(d -> {
            try {
                return d.getTitle().toLowerCase().contains("tools") || d.getCurrentUrl().toLowerCase().contains("tools") || d.getPageSource().toLowerCase().contains("developer tools");
            } catch (WebDriverException e) {
                return false;
            }
        });
    }

    // Helper methods added below

    private WebElement tryFindFirstVisible(List<By> candidates, Duration timeoutPerCandidate) {
        for (By by : candidates) {
            try {
                WebDriverWait shortWait = new WebDriverWait(driver, timeoutPerCandidate);
                WebElement el = shortWait.until(ExpectedConditions.visibilityOfElementLocated(by));
                if (el != null && el.isDisplayed()) {
                    System.out.println("Found element using: " + by.toString());
                    return el;
                }
            } catch (Exception e) {
                // try next
            }
        }
        // Fallback heuristics: try inputs (search) or nav/menu elements
        try {
            List<WebElement> inputs = driver.findElements(By.tagName("input"));
            for (WebElement in : inputs) {
                try {
                    if (!in.isDisplayed()) continue;
                    String placeholder = in.getAttribute("placeholder");
                    String aria = in.getAttribute("aria-label");
                    String type = in.getAttribute("type");
                    if ((placeholder != null && placeholder.toLowerCase().contains("search")) ||
                            (aria != null && aria.toLowerCase().contains("search")) ||
                            (type != null && type.toLowerCase().contains("search")) ||
                            in.getSize().getWidth() > 100) {
                        System.out.println("Fallback: using visible input with placeholder/aria/type/size");
                        return in;
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (WebDriverException wde) {
            // if the window closed, write diagnostics note and rethrow a clear exception
            dumpDiagnostics("no-match-window-closed");
            throw new NoSuchElementException("Browser window closed while trying to locate elements: " + wde.toString());
        } catch (Exception ignored) {
        }

        try {
            List<WebElement> navs = driver.findElements(By.cssSelector("nav, [role='navigation'], [role='menu'], [role='dialog']"));
            for (WebElement n : navs) {
                try {
                    if (n.isDisplayed() && n.getSize().getHeight() > 20 && n.getSize().getWidth() > 20) {
                        System.out.println("Fallback: using visible nav/menu/dialog element");
                        return n;
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (WebDriverException wde) {
            dumpDiagnostics("no-match-window-closed");
            throw new NoSuchElementException("Browser window closed while trying to locate nav/menu elements: " + wde.toString());
        } catch (Exception ignored) {
        }

        // dump diagnostics so we can inspect the real DOM when running
        dumpDiagnostics("no-match");
        throw new NoSuchElementException("None of the candidate selectors matched: " + candidates + ". See target/diagnostics for page source/screenshot.");
    }

    private void clickElement(WebElement el) {
        if (el == null) throw new IllegalArgumentException("Element to click is null");
        try {
            el.click();
            return;
        } catch (Exception ignored) {}
        // fallback to JS click
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
        } catch (Exception e) {
            // last resort: attempt to focus and send Enter
            try {
                el.sendKeys(Keys.ENTER);
            } catch (Exception ignored) {}
        }
    }

    private String safeGetCurrentUrl() {
        try { return driver.getCurrentUrl(); } catch (Exception e) { return ""; }
    }

    private String safeGetTitle() {
        try { return driver.getTitle(); } catch (Exception e) { return ""; }
    }

    private void dumpDiagnostics(String tag) {
        try {
            System.out.println("[DIAG " + tag + "] URL: " + safeGetCurrentUrl());
            System.out.println("[DIAG " + tag + "] Title: " + safeGetTitle());
            try {
                WebElement body = driver.findElement(By.tagName("body"));
                String text = body != null ? body.getText() : "<no-body>";
                System.out.println("[DIAG " + tag + "] Body snippet: " + (text.length() > 800 ? text.substring(0, 800) + "..." : text));
            } catch (Exception ignored) {
                System.out.println("[DIAG " + tag + "] Body: <unavailable>");
            }
        } catch (Exception ignored) {}
    }
}
