package com.ing.datalib.or.yaml;

import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.mobile.MobileOR;
import com.ing.datalib.or.mobile.MobileORObject;
import com.ing.datalib.or.mobile.MobileORPage;
import com.ing.datalib.or.web.WebOR;
import com.ing.datalib.or.web.WebORObject;
import com.ing.datalib.or.web.WebORPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.annotations.AfterMethod;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for YAML Object Repository classes.
 */
public class YamlORTest {
    
    private ObjectMapper yamlMapper;
    private Path tempDir;
    
    @BeforeMethod
    public void setUp() throws IOException {
        YAMLFactory factory = new YAMLFactory();
        factory.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
        factory.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
        yamlMapper = new ObjectMapper(factory);
        
        tempDir = Files.createTempDirectory("yaml-or-test");
    }
    
    @AfterMethod
    public void tearDown() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }
    
    // ==================== YamlElementDefinition Tests ====================
    
    @Test
    public void testYamlElementDefinition_SerializeWithRoleAndText() throws IOException {
        YamlElementDefinition element = new YamlElementDefinition();
        element.setRole("button");
        element.setText("Submit");
        
        String yaml = yamlMapper.writeValueAsString(element);
        
        assertThat(yaml).contains("role: button");
        assertThat(yaml).contains("text: Submit");
        // Empty fields should not be serialized
        assertThat(yaml).doesNotContain("xpath:");
        assertThat(yaml).doesNotContain("css:");
    }
    
    @Test
    public void testYamlElementDefinition_DeserializeMinimal() throws IOException {
        String yaml = "role: checkbox\nlabel: Accept Terms";
        
        YamlElementDefinition element = yamlMapper.readValue(yaml, YamlElementDefinition.class);
        
        assertThat(element.getRole()).isEqualTo("checkbox");
        assertThat(element.getLabel()).isEqualTo("Accept Terms");
        assertThat(element.getXpath()).isNull();
    }
    
    @Test
    public void testYamlElementDefinition_FromWebORObject() {
        // Create WebORObject directly without using factory methods that need ObjectRepository
        WebORObject obj = new WebORObject();
        obj.setName("usernameInput");
        obj.getAttribute("Role").setValue("textbox");
        obj.getAttribute("Label").setValue("Username");
        obj.setFrame("mainFrame");
        
        YamlElementDefinition element = YamlElementDefinition.fromWebORObject(obj);
        
        assertThat(element.getRole()).isEqualTo("textbox");
        assertThat(element.getLabel()).isEqualTo("Username");
        assertThat(element.getFrame()).isEqualTo("mainFrame");
    }
    
    @Test
    public void testYamlElementDefinition_ToWebORObject() {
        YamlElementDefinition element = new YamlElementDefinition();
        element.setRole("link");
        element.setText("Click here");
        element.setXpath("//a[@id='clickHere']");
        
        // Create objects directly without using factory methods
        WebORObject obj = element.toWebORObject("clickLink", null);
        
        assertThat(obj.getName()).isEqualTo("clickLink");
        assertThat(obj.getAttribute("Role").getValue()).isEqualTo("link");
        assertThat(obj.getAttribute("Text").getValue()).isEqualTo("Click here");
        assertThat(obj.getAttribute("xpath").getValue()).isEqualTo("//a[@id='clickHere']");
    }
    
    // ==================== YamlPageDefinition Tests ====================
    
    @Test
    public void testYamlPageDefinition_SerializeWithElements() throws IOException {
        YamlPageDefinition page = new YamlPageDefinition("LoginPage");
        
        YamlElementDefinition username = new YamlElementDefinition();
        username.setRole("textbox");
        username.setLabel("Username");
        page.addElement("usernameField", username);
        
        YamlElementDefinition password = new YamlElementDefinition();
        password.setRole("textbox");
        password.setPlaceholder("Password");
        page.addElement("passwordField", password);
        
        String yaml = yamlMapper.writeValueAsString(page);
        
        assertThat(yaml).contains("page: LoginPage");
        assertThat(yaml).contains("usernameField:");
        assertThat(yaml).contains("passwordField:");
        assertThat(yaml).contains("role: textbox");
    }
    
    @Test
    public void testYamlPageDefinition_FromWebORPage() {
        WebOR root = new WebOR("TestProject");
        WebORPage webPage = new WebORPage("ContactPage", root);
        
        // Use direct object creation and list manipulation
        ObjectGroup<WebORObject> group1 = new ObjectGroup<>("emailInput", webPage);
        WebORObject obj1 = new WebORObject("emailInput", group1);
        obj1.getAttribute("Role").setValue("textbox");
        obj1.getAttribute("Label").setValue("Email Address");
        group1.getObjects().add(obj1);
        webPage.getObjectGroups().add(group1);
        
        YamlPageDefinition yamlPage = YamlPageDefinition.fromWebORPage(webPage);
        
        assertThat(yamlPage.getPage()).isEqualTo("ContactPage");
        assertThat(yamlPage.getElementCount()).isEqualTo(1);
        assertThat(yamlPage.hasElement("emailInput")).isTrue();
        assertThat(yamlPage.getElement("emailInput").getRole()).isEqualTo("textbox");
    }
    
    // ==================== YamlMobileElementDefinition Tests ====================
    
    @Test
    public void testYamlMobileElementDefinition_SerializeWithAccessibility() throws IOException {
        YamlMobileElementDefinition element = new YamlMobileElementDefinition();
        element.setAccessibility("login_button");
        element.setId("com.example.app:id/login");
        
        String yaml = yamlMapper.writeValueAsString(element);
        
        assertThat(yaml).contains("accessibility: login_button");
        assertThat(yaml).contains("id: com.example.app:id/login");
        // Empty fields should not be serialized
        assertThat(yaml).doesNotContain("uiAutomator:");
    }
    
    @Test
    public void testYamlMobileElementDefinition_PrimaryLocator() {
        YamlMobileElementDefinition element = new YamlMobileElementDefinition();
        element.setAccessibility("submit_btn");
        element.setId("com.app:id/submit");
        
        // Accessibility has higher priority
        assertThat(element.getPrimaryLocatorType()).isEqualTo("Accessibility");
        assertThat(element.getPrimaryLocatorValue()).isEqualTo("submit_btn");
    }
    
    // ==================== YamlORWriter / YamlORReader Integration Tests ====================
    
    @Test
    public void testWriteAndReadWebOR() throws IOException {
        // Create Web OR with test data using direct list manipulation
        WebOR webOR = new WebOR("TestProject");
        WebORPage page = new WebORPage("HomePage", webOR);
        webOR.getPages().add(page);
        
        ObjectGroup<WebORObject> group = new ObjectGroup<>("searchBox", page);
        WebORObject obj = new WebORObject("searchBox", group);
        obj.getAttribute("Role").setValue("searchbox");
        obj.getAttribute("Placeholder").setValue("Search...");
        group.getObjects().add(obj);
        page.getObjectGroups().add(group);
        
        // Write to YAML
        YamlORWriter writer = new YamlORWriter();
        writer.writeWebOR(webOR, tempDir.toFile());
        
        // Verify file exists
        File yamlFile = new File(tempDir.toFile(), "Web/pages/HomePage.yaml");
        assertThat(yamlFile).exists();
        
        // Read back
        YamlORReader reader = new YamlORReader();
        WebOR loadedOR = reader.readWebOR(tempDir.toFile());
        
        assertThat(loadedOR.getPages()).hasSize(1);
        assertThat(loadedOR.getPages().get(0).getName()).isEqualTo("HomePage");
    }
    
    @Test
    public void testWriteAndReadMobileOR() throws IOException {
        // Create Mobile OR with test data using direct list manipulation
        MobileOR mobileOR = new MobileOR("TestProject");
        MobileORPage page = new MobileORPage("LoginScreen", mobileOR);
        page.setPackageName("com.example.app");
        mobileOR.getPages().add(page);
        
        ObjectGroup<MobileORObject> group = new ObjectGroup<>("loginBtn", page);
        MobileORObject obj = new MobileORObject("loginBtn", group);
        obj.getAttribute("Accessibility").setValue("login_button");
        group.getObjects().add(obj);
        page.getObjectGroups().add(group);
        
        // Write to YAML
        YamlORWriter writer = new YamlORWriter();
        writer.writeMobileOR(mobileOR, tempDir.toFile());
        
        // Verify file exists
        File yamlFile = new File(tempDir.toFile(), "Mobile/pages/LoginScreen.yaml");
        assertThat(yamlFile).exists();
        
        // Read back
        YamlORReader reader = new YamlORReader();
        MobileOR loadedOR = reader.readMobileOR(tempDir.toFile());
        
        assertThat(loadedOR.getPages()).hasSize(1);
        assertThat(loadedOR.getPages().get(0).getName()).isEqualTo("LoginScreen");
        assertThat(loadedOR.getPages().get(0).getPackageName()).isEqualTo("com.example.app");
    }
    
    @Test
    public void testYamlFormatSize() throws IOException {
        // Create an element with multiple properties
        YamlElementDefinition element = new YamlElementDefinition();
        element.setRole("button");
        element.setText("Submit");
        
        String yaml = yamlMapper.writeValueAsString(element);
        
        // YAML should be compact - only 2 properties
        long lineCount = yaml.lines().count();
        assertThat(lineCount).isLessThanOrEqualTo(3); // Max 3 lines for 2 props
    }
}
