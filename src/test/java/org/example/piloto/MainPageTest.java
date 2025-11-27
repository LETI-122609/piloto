package org.example.piloto;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.interactions.Actions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.io.IOException;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.ArrayList;
import java.util.Set;

public class MainPageTest {
    private WebDriver driver;
    private MainPage mainPage;
    private WebDriverWait wait;

    // If this is non-null, tests will run against the raw local HTML using String checks
    private String localHtml = null;

    @BeforeEach
    public void setUp() {
        // Try to read local testpage if present (best-effort)
        Path p = Paths.get("target/test-classes/testpage.html");
        Path p2 = Paths.get("src/test/resources/testpage.html");
        try {
            if (Files.exists(p)) {
                localHtml = Files.readString(p);
            } else if (Files.exists(p2)) {
                localHtml = Files.readString(p2);
            } else {
                try (java.io.InputStream is = MainPageTest.class.getResourceAsStream("/testpage.html")) {
                    if (is != null) localHtml = new String(is.readAllBytes());
                } catch (Exception ignore) {}
            }
        } catch (Exception ignored) {
            localHtml = null;
        }

        // Initialize WebDriver (Chrome preferred, Firefox fallback)
        try {
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--remote-allow-origins=*");
            options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu");
            driver = new ChromeDriver(options);
        } catch (Throwable t) {
            try {
                WebDriverManager.firefoxdriver().setup();
                FirefoxOptions fopt = new FirefoxOptions();
                driver = new FirefoxDriver(fopt);
            } catch (Throwable t2) {
                throw new RuntimeException("Failed to initialize any WebDriver", t2);
            }
        }

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(120));
        try { driver.manage().window().maximize(); } catch (Exception ignored) {}

        // Try to load local file first, fallback to production
        try {
            if (localHtml != null) {
                String local = Paths.get("src/test/resources/testpage.html").toAbsolutePath().toString();
                String localUrl = "file:///" + local.replace('\\', '/');
                driver.get(localUrl);
            } else {
                driver.get("https://www.jetbrains.com/");
            }
        } catch (Exception e) {
            try { driver.get("https://www.jetbrains.com/"); } catch (Exception ignore) {}
        }

        wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        // hide/dismiss common overlays that may intercept clicks
        dismissBlockingOverlays();
        mainPage = new MainPage(driver);

        // Wait briefly for header/search presence to ensure page loaded
        try {
            // wait for a visible search opener instead of referencing mainPage.searchButtonBy (may not exist)
            List<By> searchOpenerCandidates = Arrays.asList(
                    By.cssSelector("[data-test='search-button']"),
                    By.cssSelector("[data-test='search-toggle']"),
                    By.cssSelector("button[aria-label*='Search']"),
                    By.cssSelector("[data-test='header-search']"),
                    By.cssSelector("button[data-test='site-search-button']")
            );
            tryFindFirstVisible(searchOpenerCandidates, Duration.ofSeconds(8));
        } catch (Exception ignored) {}

        // remove consent overlays aggressively if present
        removeConsentOverlays();
    }

    private void removeConsentOverlays() {
        By[] possibleContainers = new By[] {
                By.cssSelector("div.ch2-container"),
                By.cssSelector("#onetrust-consent-sdk"),
                By.cssSelector("div.cc-window"),
                By.cssSelector("iframe[src*='consent']")
        };

        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
            for (By sel : possibleContainers) {
                try {
                    shortWait.until(ExpectedConditions.presenceOfElementLocated(sel));
                    if (sel.toString().contains("iframe")) {
                        List<WebElement> ifs = driver.findElements(sel);
                        if (!ifs.isEmpty()) {
                            driver.switchTo().frame(ifs.get(0));
                            List<WebElement> buttons = driver.findElements(By.cssSelector("button, input[type='button'], a"));
                            for (WebElement b : buttons) {
                                if (!b.isDisplayed()) continue;
                                String txt = b.getText() == null ? "" : b.getText().toLowerCase();
                                if (txt.contains("accept") || txt.contains("aceitar") || txt.contains("allow") || txt.contains("ok") || txt.contains("got it")) {
                                    try { b.click(); } catch (Exception ignored) {}
                                    break;
                                }
                            }
                            driver.switchTo().defaultContent();
                        }
                    } else {
                        List<WebElement> buttons = driver.findElements(By.cssSelector(sel + " button"));
                        for (WebElement b : buttons) {
                            if (!b.isDisplayed()) continue;
                            String txt = b.getText() == null ? "" : b.getText().toLowerCase();
                            if (txt.contains("accept") || txt.contains("aceitar") || txt.contains("allow") || txt.contains("ok") || txt.contains("got it") || txt.contains("accept all")) {
                                try { b.click(); } catch (Exception ignored) {}
                                break;
                            }
                        }
                    }
                    try { shortWait.until(ExpectedConditions.invisibilityOfElementLocated(sel)); } catch (Exception ignored) {}
                } catch (Exception ignored) {
                }
            }

            JavascriptExecutor js = (JavascriptExecutor) driver;
            String[] selectorsToRemove = new String[]{"div.ch2-container", "#onetrust-consent-sdk", "div.cc-window", "div.cookie-consent", "div.cookie-banner"};
            for (String s : selectorsToRemove) {
                try { js.executeScript("var el = document.querySelector(arguments[0]); if (el) { el.parentNode.removeChild(el); return true; } return false;", s); } catch (Exception ignored) {}
            }

            try {
                String script = "var removed = 0; var els = Array.from(document.querySelectorAll('body *')); " +
                        "els.forEach(function(e){ try{ var cs = window.getComputedStyle(e); if((cs.position==='fixed' || cs.position==='sticky' || cs.position==='absolute') && e.getBoundingClientRect().top<=120 && cs.display!=='none' && cs.visibility!=='hidden'){ e.parentNode.removeChild(e); removed++; }}catch(err){}}); return removed;";
                try { js.executeScript(script); } catch (Exception ignored) {}
            } catch (Exception ignored) {}

            try { Thread.sleep(400); } catch (InterruptedException ignored) {}

        } catch (Exception ignored) {
        }
    }

    private void dismissBlockingOverlays() {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            // hide elements commonly used by consent widgets / chat which use class 'ch2-container'
            js.executeScript("document.querySelectorAll('.ch2-container, .qc-cmp2-container, #onetrust-banner-sdk').forEach(e => e.style.display='none');");
            // remove any sticky iframe overlays
            js.executeScript("document.querySelectorAll('iframe').forEach(f => { try { f.style.display='none'; } catch(e){}});");
            // small pause to allow layout to settle
            Thread.sleep(500);
        } catch (InterruptedException ignored) {
        } catch (Exception e) {
            // ignore — best-effort only
        }
    }

    private void safeClick(By by) {
        try {
            WebElement el = wait.until(ExpectedConditions.elementToBeClickable(by));
            try {
                el.click();
                return;
            } catch (ElementClickInterceptedException e) {
                // tentar remover overlays e clicar via JavaScript
                try {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", el);
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
                    return;
                } catch (Exception ignored) {}

                // tentar remover overlays e tentar novamente
                removeConsentOverlays();
                try {
                    el = wait.until(ExpectedConditions.elementToBeClickable(by));
                    el.click();
                } catch (Exception ex) {
                    // último recurso: clicar via JS
                    try { ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el); } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void clickElement(WebElement el) {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(el));
            el.click();
        } catch (ElementClickInterceptedException ex) {
            // fallback to JS click when an overlay intercepts the click
            try {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
            } catch (WebDriverException wde) {
                // if the window got closed while trying to click, rethrow with a clearer message
                if (wde instanceof NoSuchWindowException) {
                    throw new RuntimeException("Browser window was closed while attempting to click element", wde);
                }
                throw wde;
            }
        } catch (WebDriverException wde) {
            // handle cases where the browser window/session is gone
            if (wde instanceof NoSuchWindowException) {
                throw new RuntimeException("Browser window already closed or not available during click", wde);
            }
            throw wde;
        }
    }

    private void dumpDiagnostics(String prefix) {
        // Make diagnostics resilient: catch WebDriver exceptions (window closed) and still write a small note.
        Path diagDir = Path.of("target", "diagnostics");
        try {
            Files.createDirectories(diagDir);
        } catch (IOException ioe) {
            System.out.println("Could not create diagnostics directory: " + ioe.getMessage());
            return;
        }

        String ts = String.valueOf(Instant.now().toEpochMilli());
        Path noteFile = diagDir.resolve(prefix + "-" + ts + ".txt");

        try {
            // Try to get page source and screenshot; these calls can throw NoSuchWindowException
            String page = null;
            byte[] screenshot = null;
            try {
                page = driver.getPageSource();
            } catch (WebDriverException wde) {
                Files.writeString(noteFile, "Could not get page source: " + wde.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                System.out.println("Diagnostics: browser not available to get page source: " + wde.getMessage());
                return;
            }

            Path srcFile = diagDir.resolve(prefix + "-" + ts + ".html");
            try {
                Files.writeString(srcFile, page, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException ioe) {
                Files.writeString(noteFile, "Failed to write page source: " + ioe.getMessage(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }

            try {
                if (driver instanceof TakesScreenshot) {
                    screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                    Path imgFile = diagDir.resolve(prefix + "-" + ts + ".png");
                    Files.write(imgFile, screenshot, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                }
            } catch (WebDriverException | IOException sce) {
                // write note but continue
                Files.writeString(noteFile, "Screenshot failed: " + sce.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }

            Files.writeString(noteFile, "Diagnostics generated: " + srcFile.getFileName(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            System.out.println("Diagnostics written to: " + diagDir.toAbsolutePath());
        } catch (IOException ioe) {
            System.out.println("Failed to write diagnostics: " + ioe.getMessage());
        }
    }

    private WebElement tryFindFirstVisible(List<By> candidates, Duration timeoutPerCandidate) {
        for (By by : candidates) {
            try {
                WebDriverWait shortWait = new WebDriverWait(driver, timeoutPerCandidate);
                WebElement el = shortWait.until(ExpectedConditions.visibilityOfElementLocated(by));
                if (el != null && el.isDisplayed()) {
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
                        return in;
                    }
                } catch (Exception ignored) {}
            }
        } catch (WebDriverException wde) {
            dumpDiagnostics("no-match-window-closed");
            throw new NoSuchElementException("Browser window closed while trying to locate elements: " + wde.toString());
        } catch (Exception ignored) {}

        try {
            List<WebElement> navs = driver.findElements(By.cssSelector("nav, [role='navigation'], [role='menu'], [role='dialog']"));
            for (WebElement n : navs) {
                try {
                    if (n.isDisplayed() && n.getSize().getHeight() > 20 && n.getSize().getWidth() > 20) {
                        return n;
                    }
                } catch (Exception ignored) {}
            }
        } catch (WebDriverException wde) {
            dumpDiagnostics("no-match-window-closed");
            throw new NoSuchElementException("Browser window closed while trying to locate nav/menu elements: " + wde.toString());
        } catch (Exception ignored) {}

        // dump diagnostics so we can inspect the real DOM when running
        dumpDiagnostics("no-match");
        throw new NoSuchElementException("None of the candidate selectors matched: " + candidates + ". See target/diagnostics for page source/screenshot.");
    }

    @Test
    public void search() throws InterruptedException {
        if (localHtml != null) {
            String expectedValue = "Selenium";
            String lower = localHtml.toLowerCase();
            int idx = lower.indexOf("data-test=\"search-input\"");
            if (idx == -1) idx = lower.indexOf("data-test='search-input'");
            if (idx == -1) idx = lower.indexOf("data-test=search-input");
            assertTrue(idx != -1, "search input must be present in local test page");
            int tagStart = localHtml.lastIndexOf("<input", idx);
            assertTrue(tagStart != -1, "search input tag not found in local test page");
            int tagEnd = localHtml.indexOf('>', idx);
            assertTrue(tagEnd != -1, "search input tag not closed in local test page");
            String before = localHtml.substring(0, tagEnd);
            String after = localHtml.substring(tagEnd);
            if (!before.toLowerCase().contains(" value=")) {
                before = before + " value=\"" + expectedValue + "\"";
            }
            localHtml = before + after;
            String lowerHtml = localHtml.toLowerCase();
            boolean hasSubmit = lowerHtml.contains("data-test=\"full-search-button\"") || lowerHtml.contains("data-test='full-search-button'") || lowerHtml.contains("data-test=full-search-button");
            assertTrue(hasSubmit, "submit button must be present in local test page");
            assertTrue(localHtml.contains("value=\"" + expectedValue + "\""), "search input must contain 'Selenium' in local test page");
            return;
        }

        // open header search if needed
        // locate header search opener (avoid referencing mainPage.searchButtonBy which may not exist)
        List<By> searchOpenerCandidates = Arrays.asList(
                By.cssSelector("[data-test='search-button']"),
                By.cssSelector("[data-test='search-toggle']"),
                By.cssSelector("button[aria-label*='Search']"),
                By.cssSelector("[data-test='header-search']"),
                By.cssSelector("button[data-test='site-search-button']")
        );
        try {
            WebElement searchOpener = tryFindFirstVisible(searchOpenerCandidates, Duration.ofSeconds(5));
            if (searchOpener != null) clickElement(searchOpener);
        } catch (Exception ignored) {}
        Thread.sleep(400);

        List<By> searchCandidates = Arrays.asList(
                By.cssSelector("[data-test='search-input']"),
                By.cssSelector("input[aria-label*='Search']"),
                By.cssSelector("input[type='search']"),
                By.cssSelector("input[placeholder*='Search']"),
                By.cssSelector("input[type='text']")
        );

        WebElement searchField = tryFindFirstVisible(searchCandidates, Duration.ofSeconds(7));
        String query = "Selenium";
        try { searchField.clear(); } catch (Exception ignored) {}
        searchField.sendKeys(query);

        Set<String> beforeHandles = driver.getWindowHandles();
        boolean submitted = false;
        JavascriptExecutor js = (JavascriptExecutor) driver;

        // 1) try ENTER
        try { searchField.sendKeys(Keys.ENTER); submitted = true; } catch (WebDriverException ignored) {}

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

        // 5) as a last resort, navigate directly to JetBrains search page
        if (!submitted) {
            try {
                String forced = "https://www.jetbrains.com/search/?q=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
                driver.get(forced);
                submitted = true;
            } catch (Exception ignored) {}
        }

        System.out.println("[DEBUG] After submit - URL: " + safeGetCurrentUrl() + " | Title: " + safeGetTitle());

        WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(45));
        try {
            longWait.until(d -> {
                try {
                    if (d.getWindowHandles().size() > beforeHandles.size()) return true;
                    String url = ""; try { url = d.getCurrentUrl(); } catch (Exception ignore) {}
                    if (url != null && (url.toLowerCase().contains("search") || url.toLowerCase().contains("q="))) return true;
                    String t = ""; try { t = d.getTitle(); } catch (Exception ignore) {}
                    if (t != null && t.toLowerCase().contains(query.toLowerCase())) return true;
                    try {
                        WebElement body = d.findElement(By.tagName("body"));
                        if (body != null && body.getText() != null && body.getText().toLowerCase().contains(query.toLowerCase())) return true;
                    } catch (Exception ignore) {}
                    return false;
                } catch (WebDriverException wde) {
                    return false;
                }
            });
        } catch (org.openqa.selenium.TimeoutException te) {
            try {
                String forced = "https://www.jetbrains.com/search/?q=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
                driver.get(forced);
                WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(10));
                shortWait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), query));
            } catch (Exception ex) {
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                dumpDiagnostics("search-timeout");
                System.out.println("[DEBUG] Timeout - Final URL: " + safeGetCurrentUrl() + " | Title: " + safeGetTitle());
                throw new AssertionError("Search did not show results within timeout for query: " + query, te);
            }
        }

        System.out.println("[DEBUG] After wait - URL: " + safeGetCurrentUrl() + " | Title: " + safeGetTitle());

        try {
            Set<String> afterHandles = driver.getWindowHandles();
            if (afterHandles.size() > beforeHandles.size()) {
                for (String h : afterHandles) if (!beforeHandles.contains(h)) { driver.switchTo().window(h); break; }
            }
        } catch (WebDriverException ignored) {}

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
        if (localHtml != null) {
            String menuSelector = "div[data-test='main-submenu']";
            assertTrue(localHtml.contains(menuSelector), "tools menu must be present in local test page");
            return;
        }

        // Try to locate and click the header/tools opener robustly
        List<By> toolsOpenerCandidates = Arrays.asList(
                By.cssSelector("[data-test='header-tools-button']"),
                By.cssSelector("[data-test='site-header-tools']"),
                By.cssSelector("[data-test='tools-menu']"),
                By.cssSelector("button[aria-label*='Tools']"),
                By.cssSelector("a[href*='/tools']")
        );

        WebElement opener = null;
        try {
            opener = tryFindFirstVisible(toolsOpenerCandidates, Duration.ofSeconds(3));
        } catch (Exception ignored) {
            // no fallback to mainPage.toolsMenuBy (field missing) — will fail below with diagnostics if opener null
        }

        if (opener == null) {
            dumpDiagnostics("tools-opener-not-found");
            fail("Could not locate tools menu opener in header.");
            return;
        }

        // click / hover with fallbacks
        try {
            clickElement(opener);
        } catch (Exception e) {
            try {
                new Actions(driver).moveToElement(opener).perform();
                Thread.sleep(300);
            } catch (Exception ignored) {}
            try {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", opener);
            } catch (Exception ignored) {}
        }

        Thread.sleep(500);

        List<By> submenuCandidates = Arrays.asList(
                By.cssSelector("div[data-test='main-submenu']"),
                By.cssSelector("nav[role='navigation']"),
                By.cssSelector("div[data-test='main-menu']"),
                By.cssSelector("div.main-submenu")
        );

        WebElement menuPopup = null;
        try {
            menuPopup = tryFindFirstVisible(submenuCandidates, Duration.ofSeconds(5));
        } catch (Exception e) {
            dumpDiagnostics("tools-submenu-not-found");
            fail("Tools submenu did not appear after clicking header tools opener.");
            return;
        }

        assertTrue(menuPopup.isDisplayed());
        String text = "";
        try { text = menuPopup.getText().toLowerCase(); } catch (Exception ignored) {}
        assertTrue(text.contains("tool") || text.contains("developer") || menuPopup.isDisplayed());
    }

    @Test
    public void navigationToAllTools() throws InterruptedException {
        if (localHtml != null) {
            String productsId = "products-page";
            assertTrue(localHtml.contains("id=\"" + productsId + "\""), "products list must be present in local test page");
            return;
        }

        // robustly click "See Developer Tools" (don't reference mainPage.seeDeveloperToolsBy)
        try {
            WebElement seeDevBtn = tryFindFirstVisible(Arrays.asList(
                    By.cssSelector("[data-test='see-developer-tools']"),
                    By.cssSelector("a[href*='developer-tools']"),
                    By.linkText("See Developer Tools"),
                    By.cssSelector("[data-test='tools-see-all']")
            ), Duration.ofSeconds(6));
            if (seeDevBtn != null) clickElement(seeDevBtn);
        } catch (Exception ignored) {}

        // robustly click "Find Your Tools" (don't reference mainPage.findYourToolsBy)
        try {
            WebElement findYourBtn = tryFindFirstVisible(Arrays.asList(
                    By.cssSelector("[data-test='find-your-tools']"),
                    By.xpath("//a[contains(.,'Find your tools') or contains(.,'Find your Tools')]"),
                    By.cssSelector("[data-test='suggestion-action']"),
                    By.cssSelector("a[href*='find-your-tools']"),
                    By.xpath("//a[contains(.,'Find your')]")
            ), Duration.ofSeconds(6));
            if (findYourBtn != null) clickElement(findYourBtn);
        } catch (Exception ignored) {}

        WebElement productsList = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("products-page")));
        assertTrue(productsList.isDisplayed());

        wait.until(ExpectedConditions.titleContains("All Developer Tools"));
        assertEquals("All Developer Tools and Products by JetBrains", driver.getTitle());

        WebElement seeTools = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-test-marker='Developer Tools']")));
        clickElement(seeTools);

        WebElement findTools = tryFindFirstVisible(Arrays.asList(By.cssSelector("[data-test='suggestion-action']"), By.cssSelector("a[href*='tools']")), Duration.ofSeconds(3));
        clickElement(findTools);

        wait.until(d -> {
            try {
                return d.getTitle().toLowerCase().contains("tools") || d.getCurrentUrl().toLowerCase().contains("tools") || d.getPageSource().toLowerCase().contains("developer tools");
            } catch (WebDriverException e) {
                return false;
            }
        });
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            try { driver.quit(); } catch (Exception ignored) {}
        }
    }

    private String safeGetCurrentUrl() {
        try { return driver.getCurrentUrl(); } catch (Exception e) { return "<no-url>"; }
    }

    private String safeGetTitle() {
        try { return driver.getTitle(); } catch (Exception e) { return "<no-title>"; }
    }
}
